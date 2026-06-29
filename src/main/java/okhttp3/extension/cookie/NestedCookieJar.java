package okhttp3.extension.cookie;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link CookieJar} that delegates to multiple cookie jars.
 */
@Slf4j
public class NestedCookieJar implements CookieJar {

    private List<CookieJar> cookieJars;

    public NestedCookieJar(List<CookieJar> cookieJars) {
        this.cookieJars = cookieJars;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (cookieJars == null || cookieJars.isEmpty()) {
            return;
        }
        for (CookieJar cookieJar : cookieJars) {
            try {
                cookieJar.saveFromResponse(url, cookies);
            } catch (Exception e) {
                log.error("saveFromResponse error", e);
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        if (cookieJars == null || cookieJars.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Cookie> cookieMap = new HashMap<>();
        for (CookieJar cookieJar : cookieJars) {
            try {
                List<Cookie> cookies = cookieJar.loadForRequest(url);
                if (cookies == null || cookies.isEmpty()) {
                    continue;
                }
                for (Cookie cookie : cookies) {
                    if (cookie.matches(url) && cookie.expiresAt() >= System.currentTimeMillis()) {
                        cookieMap.put(cookie.name(), cookie);
                    }
                }
            } catch (Exception e) {
                log.error("loadForRequest error", e);
            }
        }
        if (cookieMap.isEmpty()) {
            return Collections.emptyList();
        }
        return cookieMap.values().stream().collect(Collectors.toList());
    }
}
