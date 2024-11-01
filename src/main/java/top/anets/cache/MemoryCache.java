package top.anets.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Data
public class MemoryCache {
    Cache<String, Object>  CacheDay = Caffeine.newBuilder()
            .maximumSize(100)
//           最后访问后间隔多久60s淘汰
            .expireAfterWrite(60*60*24, TimeUnit.SECONDS)
            .build();
}
