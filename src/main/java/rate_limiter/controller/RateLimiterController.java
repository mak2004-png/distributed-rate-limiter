package rate_limiter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rate_limiter.service.RateLimiterService;

@RestController
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService){
        this.rateLimiterService = rateLimiterService;
    }


    @GetMapping("/api/ping")
    public ResponseEntity<Void> userRequest(@RequestParam String clientId){
        boolean result = rateLimiterService.allowRequest(clientId);
        if (result){
               return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }


}
