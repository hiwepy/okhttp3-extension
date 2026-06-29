package okhttp3.extension.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OkHttp interceptor that retries failed requests.
 */
@Slf4j
public class RequestRetryIntercepter implements RequestInterceptor {

    private int retryMaxAttempts;
    private long retryInterval;
    private AtomicBoolean enabled = new AtomicBoolean(false);
    private final Cache<String, Integer> cache;
    private final String RETRY_ID_TMP = "retryId-%s";

    public RequestRetryIntercepter(int retryMaxAttempts, long retryInterval) {
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryInterval = retryInterval;
        this.enabled.set(retryMaxAttempts > 0);
        this.cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .removalListener(new RemovalListener<String, Integer>() {
                    @Override
                    public void onRemoval(@Nullable String key, @Nullable Integer value, @NonNull RemovalCause cause) {
                        log.debug("Remove Cache : {}", key);
                    }
                })
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    @SuppressWarnings("resource")
    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!enabled.get()) {
            return chain.proceed(chain.request());
        }
        Request request = chain.request();
        Response response = doRequest(chain, request);
        String retryId = String.format(RETRY_ID_TMP, request.hashCode());
        Integer retryNum = cache.getIfPresent(retryId);
        if (Objects.isNull(retryNum)) {
            retryNum = 0;
        }
        while ((response == null || !response.isSuccessful()) && retryNum < retryMaxAttempts) {
            log.info("intercept Request is not successful - {}", retryNum);
            final long nextInterval = getRetryInterval();
            try {
                log.info("Wait for {}", nextInterval);
                Thread.sleep(nextInterval);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
            retryNum++;
            cache.put(retryId, retryNum);
            response = doRequest(chain, request);
        }
        if (response.isSuccessful()) {
            cache.invalidate(retryId);
        }
        return response;
    }

    private Response doRequest(Chain chain, Request request) {
        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
        }
        return response;
    }

    public long getRetryInterval() {
        return this.retryInterval;
    }
}
