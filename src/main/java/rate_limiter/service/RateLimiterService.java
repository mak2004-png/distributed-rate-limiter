package rate_limiter.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import rate_limiter.bucket.TokenBucket;

import java.time.Instant;
import java.util.Map;

@Service

public class RateLimiterService {

    RedisTemplate<String, Object> redisTemplate;

    public RateLimiterService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String clientId){
        Map<Object, Object> existingData = redisTemplate.opsForHash().entries(clientId);

        TokenBucket bucket;

        if (existingData.isEmpty()){
            bucket = new TokenBucket(10, 5, 10, Instant.now());
        }
        else {
            long existingTokens = Long.parseLong(existingData.get("tokens").toString());
            Instant existingTimestamp = Instant.parse(existingData.get("lastRefill").toString());
            bucket = new TokenBucket(10, 5, existingTokens, existingTimestamp);
        }
        boolean allowed = bucket.tryConsume();
        redisTemplate.opsForHash().put(clientId, "tokens", String.valueOf(bucket.getCurrentTokens()));
        redisTemplate.opsForHash().put(clientId, "lastRefill", bucket.getLastRefillTimeStamp().toString());
        return allowed;

    }
}
