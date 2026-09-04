package rate_limiter.service;

import rate_limiter.bucket.TokenBucket;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterService {

    ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientId){
        return clientBuckets.computeIfAbsent(clientId, k-> new TokenBucket(10,5)).tryConsume();


    }
}
