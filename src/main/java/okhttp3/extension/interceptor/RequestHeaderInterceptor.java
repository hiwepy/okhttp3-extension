package okhttp3.extension.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OkHttp interceptor that sets default request headers.
 */
@Slf4j
public class RequestHeaderInterceptor implements RequestInterceptor {

    private AtomicBoolean enabled = new AtomicBoolean(false);
    private RequestHeaderProvider headerProvider;

    public RequestHeaderInterceptor(RequestHeaderProvider headerProvider) {
        this.headerProvider = headerProvider;
        this.enabled.set(true);
    }

    public void enable() {
        enabled.set(true);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void disable() {
        enabled.set(false);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!enabled.get()) {
            return chain.proceed(chain.request());
        }
        Request originalRequest = chain.request();
        Builder builder = originalRequest.newBuilder();
        for (HeaderEntry entry : headerProvider.getHeaders()) {
            builder = setHeader(originalRequest, builder, entry.getName(), entry.getValue());
        }
        return chain.proceed(builder.build());
    }

    protected Builder setHeader(Request request, Builder builder, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            boolean match = request.headers().names().stream().anyMatch(item -> item.equalsIgnoreCase(key));
            if (!match) {
                if (log.isDebugEnabled()) {
                    log.debug("Set HTTP HEADER: {}:{}.", key, value);
                }
                return builder.header(key, value);
            }
        }
        return builder;
    }

    /**
     * Provider for request headers.
     */
    public interface RequestHeaderProvider {
        java.util.List<HeaderEntry> getHeaders();
    }

    /**
     * A header name-value pair.
     */
    public static class HeaderEntry {
        private final String name;
        private final String value;

        public HeaderEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
    }
}
