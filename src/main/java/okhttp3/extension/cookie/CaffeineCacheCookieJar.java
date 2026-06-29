package okhttp3.extension.cookie;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A {@link CookieJar} backed by Caffeine cache.
 */
@Slf4j
public class CaffeineCacheCookieJar implements CookieJar {

    protected Cache<String, List<Cookie>> cookieCache;

    public CaffeineCacheCookieJar(long maximumSize, Duration expireAfterWrite, Duration expireAfterAccess) {
        this.cookieCache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(maximumSize)
                .removalListener(new RemovalListener<String, List<Cookie>>() {
                    @Override
                    public void onRemoval(@Nullable String host, @Nullable List<Cookie> value, @NonNull RemovalCause cause) {
                        log.debug("Remove Cookie Cache : {}", host);
                    }
                })
                .expireAfterWrite(expireAfterWrite)
                .expireAfterAccess(expireAfterAccess)
                .build();
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cookieCache.put(url.host(), cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieCache.getIfPresent(url.host());
        if (Objects.nonNull(cookies)) {
            List<Cookie> newCookies = new ArrayList<>();
            for (Cookie cookie : cookies) {
                if (cookie.expiresAt() >= System.currentTimeMillis()) {
                    newCookies.add(cookie);
                }
            }
            cookieCache.put(url.host(), newCookies);
            return newCookies;
        }
        return Collections.emptyList();
    }
}
