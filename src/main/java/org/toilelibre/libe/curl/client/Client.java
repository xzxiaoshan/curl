package org.toilelibre.libe.curl.client;

import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.Version;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.http.ResponseHandler;

import java.io.IOException;

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
     * @param request         safe to replay.
     * @param responseHandler responseHandler
     * @param outputFilePath  outputFilePath
     * @param <T>             T
     * @return connected response, {@link Response.Body} is absent or unread.
     * @throws IOException on a network error connecting to {@link Request#url()}.
     */
    <T> T execute(Request request, ResponseHandler<? extends T> responseHandler, final String outputFilePath) throws IOException;

    /**
     * 默认的UA信息，实现该接口的其他http库，可以Override该方法返回自己的默认设定的UA
     * 当最终使用curl请求时，如果参数中提供了UA，则使用参数提供的UA，该方法返回的UA只在于用户没有主动提供UA时自动设置
     *
     * @return 返回参数未设定UA时的默认UA字符串
     */
    default String userAgent() {
        return String.format("%s/%s/%s, %s, (Java/%s)", Curl.class.getPackage().getName(),
                Version.NUMBER, Version.BUILD_TIME, "CurlClient", System.getProperty("java.version"));
    }

}

