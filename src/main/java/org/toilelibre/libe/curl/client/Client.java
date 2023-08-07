package org.toilelibre.libe.curl.client;

import org.toilelibre.libe.curl.SSLOption;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.http.auth.AuthCredentials;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Submits HTTP requests. Implementations are expected to be thread-safe.
 *
 * @author shanhy
 * @date 2023-07-25 16:16
 */
public interface Client {

    /**
     * 该方法使用默认实现，一般情况不需要重写。
     * Executes a request against its {@link Request#url() url} and returns a response.
     *
     * @param request safe to replay.
     * @return connected response, {@link Response.Body} is absent or unread.
     * @throws IOException on a network error connecting to {@link Request#url()}.
     */
    Response execute(Request request) throws IOException;

    Response doExecute(Request request) throws IOException;

    String defaultUserAgent();

    void handleAuthCredentials(final AuthCredentials credentials);

    void handleSSL(Map<SSLOption, List<String>> sslOptions);
}

