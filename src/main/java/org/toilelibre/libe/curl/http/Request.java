package org.toilelibre.libe.curl.http;

import lombok.Getter;
import lombok.Setter;
import org.toilelibre.libe.curl.SSLOption;
import org.toilelibre.libe.curl.Utils;
import org.toilelibre.libe.curl.http.auth.AuthCredentials;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Http请求的Request对象，主要由curl相关参数转换而来
 * An immutable request to an http server.
 *
 * @author shanhy
 * @date 2023-07-25 16:15
 */
public final class Request {

    /**
     * httpMethod
     */
    private final HttpMethod httpMethod;
    /**
     * url
     */
    private final String url;
    /**
     * headers
     */
    private final Map<String, List<String>> headers;
    /**
     * body
     */
    private final RequestBody<?> body;
    /**
     * options
     */
    private final Options options;

    /**
     * Builds a Request. All parameters must be effectively immutable, via safe copies.
     *
     * @param httpMethod for the request.
     * @param url        for the request.
     * @param headers    to include.
     * @param body       of the request, can be {@literal null}
     * @param options    of the request, can be {@literal null}
     * @return a Request
     */
    public static Request create(HttpMethod httpMethod,
                                 String url,
                                 Map<String, List<String>> headers,
                                 RequestBody<?> body, Options options) {
        return new Request(httpMethod, url, headers, body, options);
    }

    /**
     * Creates a new Request.
     *
     * @param method  of the request.
     * @param url     for the request.
     * @param headers for the request.
     * @param body    for the request, optional.
     * @param options options
     */
    Request(HttpMethod method,
            String url,
            Map<String, List<String>> headers,
            RequestBody<?> body, Options options) {
        this.httpMethod = Utils.checkNotNull(method, "httpMethod of %s", method.name());
        this.url = Utils.checkNotNull(url, "url");
        this.headers = Utils.checkNotNull(headers, "headers of %s %s", method, url);
        this.body = body;
        this.options = options;
    }

    /**
     * Http Method for the request.
     *
     * @return the HttpMethod.
     */
    public HttpMethod httpMethod() {
        return this.httpMethod;
    }

    /**
     * Http Content-Type for the request.
     *
     * @return the ContentType.
     */
    public String getContentType() {
        return this.getFirstHeader(Utils.CONTENT_TYPE);
    }

    /**
     * Request Header (first by name).
     *
     * @param name name
     * @return the request headers.
     */
    public String getFirstHeader(final String name) {
        List<String> headerValues = this.headers.get(name);
        return headerValues != null && !headerValues.isEmpty() ? headerValues.get(0) : null;
    }


    /**
     * URL for the request.
     *
     * @return URL as a String.
     */
    public String url() {
        return url;
    }


    /**
     * Options for the request.
     *
     * @return Options instance.
     */
    public Options options() {
        return options;
    }

    /**
     * Request Headers.
     *
     * @return the request headers.
     */
    public Map<String, List<String>> headers() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Charset of the request.
     *
     * @return the current character set for the request, may be {@literal null} for binary data.
     */
    public Charset getCharset() {
        Charset charset = null;
        if (body instanceof DataBody) {
            charset = ((DataBody) body).getEncoding().orElse(null);
        }
        if (charset == null) {
            String contentType = this.getContentType();
            if (contentType != null) {
                int idx = contentType.indexOf("charset=");
                if (idx != -1) {
                    charset = Charset.forName(contentType.substring(idx + 8));// string 'charset=' length is 8
                }
            }
        }
        return charset;
    }

    /**
     * Charset of the request.
     *
     * @return the current character set for the request, may be {@literal null} for binary data.
     */
    public Charset getEncoding() {
        return this.getCharset();
    }

    /**
     * body
     *
     * @return RequestBody
     */
    public RequestBody<?> body() {
        return body;
    }

    /**
     * isBinary
     *
     * @return data
     */
    public boolean isBinary() {
        if (body instanceof DataBody)
            return ((DataBody) body).isBinary();
        return false;
    }

    /**
     * Request Length.
     *
     * @return size of the request body.
     */
    public int length() {
        if (body instanceof DataBody)
            return ((DataBody) body).length();
        return 0;
    }

    /**
     * Request as an HTTP/1.1 request.
     *
     * @return the request.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(httpMethod).append(' ').append(url).append(" HTTP/1.1\n");
        for (final String field : headers.keySet()) {
            for (final String value : Utils.valuesOrEmpty(headers, field)) {
                builder.append(field).append(": ").append(value).append('\n');
            }
        }
        if (body != null) {
            if (body instanceof DataBody) {
                builder.append('\n').append(((DataBody) body).asString());
            } else if (body instanceof FormBody) {
                builder.append('\n').append(((FormBody) body).asString());
            }
        }
        return builder.toString();
    }

    /**
     * getMethod
     *
     * @return HttpMethod
     */
    public HttpMethod getMethod() {
        return this.httpMethod();
    }

    /**
     * Controls the per-request settings currently required to be implemented by all {@link com.sun.security.ntlm.Client
     * clients}*
     *
     * @author 单红宇
     * @date 2024-04-20 15:07:33
     */
    public static class Options {

        /**
         * connectTimeout, millisecond
         * -- SETTER --
         * setConnectTimeout
         */
        @Setter
        private int connectTimeout;

        /**
         * maxTimeout, millisecond
         * -- SETTER --
         * setMaxTimeout
         */
        @Setter
        private int maxTimeout;
        /**
         * followRedirects
         * -- GETTER --
         * Defaults to true.
         * tells the client to not follow the redirections.
         */
        @Setter
        @Getter
        private boolean followRedirects;
        /**
         * Request compressed response (using deflate or gzip)
         * -- GETTER --
         * isCompressed
         * <p>
         * <p>
         * -- SETTER --
         * setCompressed
         */
        @Setter
        @Getter
        private boolean compressed;
        /**
         * proxy
         * -- SETTER --
         * setProxy
         * <p>
         * <p>
         * -- GETTER --
         * getProxy
         */
        @Getter
        @Setter
        private ProxyInfo proxy;
        /**
         * authCredentials
         * -- GETTER --
         * getAuthCredentials
         * <p>
         * <p>
         * -- SETTER --
         * setAuthCredentials
         */
        @Setter
        @Getter
        private AuthCredentials authCredentials;
        /**
         * sslOptions
         * -- GETTER --
         * getSslOptions
         */
        @Getter
        private Map<SSLOption, List<String>> sslOptions;

        /**
         * Creates a new Options Instance.
         *
         * @param connectTimeout  value.
         * @param maxTimeout      value.
         * @param followRedirects if the request should follow 3xx redirections.
         */
        public Options(int connectTimeout,
                       int maxTimeout,
                       boolean followRedirects) {
            super();
            this.connectTimeout = connectTimeout;
            this.maxTimeout = maxTimeout;
            this.followRedirects = followRedirects;
            this.proxy = null;
        }

        /**
         * Creates a new Options Instance.
         *
         * @param connectTimeout  value.
         * @param maxTimeout      value.
         * @param followRedirects if the request should follow 3xx redirections.
         * @param proxy           request proxy info.
         */
        public Options(int connectTimeout,
                       int maxTimeout,
                       boolean followRedirects, ProxyInfo proxy) {
            super();
            this.connectTimeout = connectTimeout;
            this.maxTimeout = maxTimeout;
            this.followRedirects = followRedirects;
            this.proxy = proxy;
        }

        /**
         * Creates the new Options instance using the following defaults:
         * <ul>
         * <li>Connect Timeout: 10 seconds</li>
         * <li>Read Timeout: 60 seconds</li>
         * <li>Follow all 3xx redirects</li>
         * </ul>
         */
        public Options() {
            this(10 * 1000, 60 * 1000, false);
        }

        /**
         * Connect Timeout Value.
         *
         * @return current timeout value.
         */
        public int connectTimeout() {
            return connectTimeout;
        }

        /**
         * Read Timeout value.
         *
         * @return current read timeout value.
         */
        public int maxTimeout() {
            return maxTimeout;
        }

        /**
         * setSSLOptions
         *
         * @param sslOptions sslOptions
         */
        public void setSSLOptions(Map<SSLOption, List<String>> sslOptions) {
            this.sslOptions = sslOptions;
        }

    }

}
