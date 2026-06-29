package okhttp3.extension.interceptor;

import okhttp3.Authenticator;

public interface ProxyAuthenticator extends Authenticator {

    /** An authenticator that knows no credentials and makes no attempt to authenticate. */
    ProxyAuthenticator NONE = (route, response) -> null;

}
