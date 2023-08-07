package org.toilelibre.libe.curl;

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
public class CurlJavaOptions {

    private final List<BiFunction<Request, Supplier<Response>, Response>> interceptors;
    private final List<String> placeHolders;
    private final Client httpClient;

    private CurlJavaOptions(Builder builder) {
        this.interceptors = builder.interceptors;
        this.placeHolders = builder.placeHolders;
        this.httpClient = builder.httpClient;
    }

    public static Builder with() {
        return new Builder();
    }

    public List<BiFunction<Request, Supplier<Response>, Response>> getInterceptors() {
        return interceptors;
    }

    public List<String> getPlaceHolders() {
        return placeHolders;
    }

    public Client getHttpClient() {
        return this.httpClient;
    }

    public static final class Builder {
        private final List<BiFunction<Request, Supplier<Response>, Response>> interceptors = new ArrayList<>();
        private List<String> placeHolders;
        private Client httpClient;

        private Builder() {
        }

        public Builder interceptor(BiFunction<Request, Supplier<Response>, Response> val) {
            interceptors.add(val);
            return this;
        }

        public Builder placeHolders(List<String> val) {
            placeHolders = val;
            return this;
        }

        public Builder httpClient(Client httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public CurlJavaOptions build() {
            return new CurlJavaOptions(this);
        }
    }
}
