package rate_limiter.bucket;

import java.time.Duration;
import java.time.Instant;

public class TokenBucket {

    private long capacity;
    private long refillRate;
    private long currentTokens;
    private Instant lastRefillTimeStamp;

    public TokenBucket(long capacity, long refillRate, long currentTokens, Instant lastRefillTimeStamp){
    this.capacity = capacity;
    this.refillRate = refillRate;
    this.currentTokens = currentTokens;
    this.lastRefillTimeStamp = lastRefillTimeStamp;
    }

    private synchronized void refill(){

        Instant start = lastRefillTimeStamp;
        Instant now = Instant.now();
        Duration durationBetween = Duration.between(start, now);
        long elapsedTime = durationBetween.toMillis();

        long tokensEarned = refillRate * elapsedTime/1000;

        long newTokensCount = Math.min(capacity, currentTokens + tokensEarned);
        this.currentTokens = newTokensCount;
        this.lastRefillTimeStamp = now;
    }
    public synchronized boolean tryConsume(){
        refill();
        if (currentTokens >= 1){
            currentTokens -= 1;
            return true;
        }
        else {
            return false;
        }
    }
// getters so that redis can get the actual value out from here
    public long getCurrentTokens() {
        return currentTokens;
    }

    public Instant getLastRefillTimeStamp() {
        return lastRefillTimeStamp;
    }
}
