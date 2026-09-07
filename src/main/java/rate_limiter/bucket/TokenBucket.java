package rate_limiter.bucket;

import java.time.Duration;
import java.time.Instant;

public class TokenBucket {

    private long capacity;
    private long refillRate;
    private long currentTokens;
    private Instant lastRefillTimeStamp;

    public TokenBucket(long capacity, long refillRate){
    this.capacity = capacity;
    this.refillRate = refillRate;
    this.currentTokens = capacity;
    this.lastRefillTimeStamp = Instant.now();
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
// race condition can occur in this method for now we'll fix it later while testing
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
}
