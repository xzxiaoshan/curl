package org.toilelibre.libe.curl.http;

/**
 * 请求体
 *
 * @author shanhy
 * @date 2023-07-31 17:49
 */
public interface RequestBody<T> {

    RequestBodyType getBodyType();

    T getBody();

}
