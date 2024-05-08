package org.toilelibre.libe.curl.http;

import org.apache.hc.core5.http.HttpException;

import java.io.IOException;

/**
 * 响应处理器
 *
 * @author shanhy
 * @date 2023-09-14 14:48
 */
@FunctionalInterface
public interface ResponseHandler<T> {

    /**
     * Processes an {@link Response} and returns some value
     * corresponding to that response.
     *
     * @param response The response to process
     * @return A value determined by the response
     * @throws IOException in case of a problem or the connection was aborted
     */
    T handleResponse(Response response) throws HttpException, IOException;

}
