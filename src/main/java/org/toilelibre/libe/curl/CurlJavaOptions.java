package org.toilelibre.libe.curl;

import lombok.Getter;
import org.toilelibre.libe.curl.client.Client;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Curl扩展的Java参数选项
 *
 * @author shanhy
 * @date 2023-08-02 14:10
 */
@Getter
public class CurlJavaOptions<T> {

    private final List<BiFunction<Request, Supplier<? extends T>, ? extends T>> interceptors;
    private final List<String> placeHolders;
    private final Client httpClient;

    public CurlJavaOptions(List<BiFunction<Request, Supplier<? extends T>, ? extends T>> interceptors, List<String> placeHolders, Client httpClient) {
        this.interceptors = interceptors;
        this.placeHolders = placeHolders;
        this.httpClient = httpClient;
    }

    public CurlJavaOptions() {
        this(new ArrayList<>(), new ArrayList<>(), null);
    }


    public CurlJavaOptions<T> addInterceptor(BiFunction<Request, Supplier<? extends T>, ? extends T> interceptors) {
        this.interceptors.add(interceptors);
        return this;
    }

    public CurlJavaOptions<T> addPlaceHolders(List<String> placeHolders) {
        this.placeHolders.addAll(placeHolders);
        return this;
    }
}
