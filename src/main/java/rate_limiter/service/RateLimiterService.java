package rate_limiter.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;


@Service

public class RateLimiterService {

    private final RedisScript<Long> tokenBucketScripts =
            RedisScript.of(new ClassPathResource("token_bucket.lua"), Long.class );
    RedisTemplate redisTemplate;

    public RateLimiterService(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String clientId){
        long currentTimestamp = Instant.now().getEpochSecond();
        List<String> keys = Collections.singletonList(clientId);

        Long result = (Long) redisTemplate.execute(
                tokenBucketScripts,
                keys,
                String.valueOf(currentTimestamp),
                String.valueOf(10),
                String.valueOf(5)
        );
        return result == 1;
    }

}
