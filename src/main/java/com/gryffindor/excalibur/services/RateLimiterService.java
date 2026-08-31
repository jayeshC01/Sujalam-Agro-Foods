package com.gryffindor.excalibur.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder().maximumSize(50_000).expireAfterAccess(10, TimeUnit.MINUTES).build();

  public Bucket resolveBucket(String key, int capacity, Duration refillDuration) {
    return buckets.get(
        key,
        k ->
            Bucket.builder()
                .addLimit(
                    Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillDuration)
                        .build())
                .build());
  }
}
