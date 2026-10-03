# Distributed Rate Limiter

A rate limiting API gateway built to explore real distributed-systems problems — shared state across instances, atomicity under concurrency, and race conditions — rather than another CRUD app.

## What it does

Exposes a `GET /api/ping?clientId=...` endpoint protected by a **Token Bucket** rate limiter. Each client gets their own bucket (capacity 10, refill rate 5 tokens/sec by default). State is held in Redis, not in application memory, so the limit holds correctly even when multiple instances of the service are running behind a load balancer.

## Why Token Bucket

Five rate limiting algorithms were considered (Fixed Window, Sliding Window Log, Sliding Window Counter, Leaky Bucket, Token Bucket). Token Bucket was chosen because it tolerates legitimate bursty client behavior (e.g. a page load firing several API calls at once) while Leaky Bucket and the window-based approaches either smooth traffic too aggressively or allow boundary-exploit bursts. This is the same reasoning Stripe and GitHub publish for their own public APIs.

## Architecture

```
Client → RateLimiterController → RateLimiterService → Redis (Lua script)
```

- **`RateLimiterController`** — REST endpoint, returns `200 OK` or `429 Too Many Requests`.
- **`RateLimiterService`** — thin layer that invokes an atomic Redis Lua script per request.
- **`token_bucket.lua`** — the actual algorithm (refill calculation, capacity cap, consume-or-reject, write-back), executed atomically inside Redis.
- **`TokenBucket.java`** — the original in-memory, single-instance implementation. No longer used at runtime; kept to show the project's evolution (see below).

## The evolution (and why it matters)

This project went through three real iterations, each solving a problem the previous one didn't:

1. **In-memory, single-instance.** `TokenBucket.java` holds state as plain Java fields. Works, but each instance has its own memory — running multiple instances behind a load balancer would let a client exceed their limit by simply getting routed to a different instance.
2. **Redis-backed, but racy.** State moved to Redis so instances could share it. But reading the current state, computing in Java, and writing back are three separate steps — under concurrent requests, two requests could both read the same "stale" token count before either writes back, letting more requests through than the limit allows (a classic read-modify-write race).
3. **Redis-backed and atomic (current).** The entire read-compute-write cycle moved into a Redis Lua script, which Redis guarantees runs as one uninterruptible unit. This closes the race condition entirely, even across separate application instances.

Along the way, three real bugs surfaced and were fixed:
- Spring's default `RedisTemplate` serialized arguments as binary, corrupting the numbers the Lua script expected — fixed by switching to `StringRedisTemplate`.
- Lua's `HGET` returns `false` (not `nil`) for a missing field on a nonexistent hash key; a check written as `tokens == nil` silently never caught new clients — fixed with `if not tokens then`.
- `Instant.now().getEpochSecond()` has whole-second precision, which caused incorrect refill amounts for requests landing near a second boundary — fixed by switching to millisecond-precision timestamps.

## Verified correctness

- **Burst test:** 11 rapid sequential requests against a bucket of capacity 10 → 10× `200`, then `429`, as expected.
- **Multi-instance test:** 3 separate instances of the service run concurrently (ports 8080–8082), all backed by one Redis container. A 12-request burst rotated across all three ports returned exactly 10× `200` then 2× `429` — proving the limit is genuinely shared across instances, not per-instance (a naive in-memory implementation would have allowed up to 30 successes).
- **Load test (k6):** 10 virtual users sustained against a single instance for 10 seconds produced 5,290 total requests (~529 req/s); 98.9% were correctly rejected, with ~58–60 succeeding — matching the theoretical maximum (initial capacity + refill rate × duration) almost exactly.

## Running locally

```bash
docker-compose up --build
```

Spins up Redis plus three instances of the app (ports 8080, 8081, 8082), all sharing one Redis-backed rate limit.

## Tech stack

Java 17, Spring Boot, Redis (Lua scripting), Docker / Docker Compose, GitHub Actions (CI), k6 (load testing).

## Next steps

- JUnit test coverage (currently verified manually and via load testing only)
- Run k6 load tests automatically in CI, with pass/fail thresholds
- Per-client load testing to verify isolation between distinct clients under load
- Deploy (Railway, following the same pattern as a previous project)
