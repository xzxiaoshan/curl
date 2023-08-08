package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.client.Client;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.RequestProvider;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.client.HttpClientProvider;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.toilelibre.libe.curl.VersionDisplay.stopAndDisplayVersionIfThe;

/**
 * Curl执行入口类
 */
public class Curl {

    private final Client httpClient;

    private final AfterResponse afterResponse;
    private final InterceptorsHandler interceptorsHandler;

    public static Curl create(){
        return new Curl();
    }

    public static Curl create(Client httpClient){
        return new Curl(httpClient);
    }

    public Curl() {
        this(null, null, null);
    }

    public Curl(Client httpClient) {
        this(httpClient, null, null);
    }

    public Curl(Client httpClient, AfterResponse afterResponse, InterceptorsHandler interceptorsHandler) {
        if (httpClient == null)
            httpClient = HttpClientProvider.create();
        if (afterResponse == null)
            afterResponse = new AfterResponse();
        if (interceptorsHandler == null)
            interceptorsHandler = new InterceptorsHandler();

        this.httpClient = httpClient;
        this.afterResponse = afterResponse;
        this.interceptorsHandler = interceptorsHandler;
    }

    public String curlToString(final String requestCommand) throws CurlException {
        return curlToString(requestCommand, CurlJavaOptions.with().build());
    }

    public String curlToString(final String requestCommand, CurlJavaOptions curlJavaOptions) throws CurlException {
        try(Response response = this.curl(requestCommand, curlJavaOptions)) {
            Response.Body body = response.body();
            if(body != null)
                return IOUtils.quietToString(body.asInputStream());
        } catch (final UnsupportedOperationException e) {
            throw new CurlException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public CompletableFuture<String> curlAsyncToString(final String requestCommand) throws CurlException {
        return curlAsyncToString(requestCommand, CurlJavaOptions.with().build());
    }

    public CompletableFuture<String> curlAsyncToString(final String requestCommand,
                                                       CurlJavaOptions curlJavaOptions) throws CurlException {
        return this.curlAsync(requestCommand, curlJavaOptions).thenApply((response) -> {
            try {
                return IOUtils.quietToString(response.body().asInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CurlJavaOptions.Builder javaOptionsBuilder() {
        return CurlJavaOptions.with();
    }

    public CompletableFuture<Response> curlAsync(final String requestCommand) throws CurlException {
        return curlAsync(requestCommand, CurlJavaOptions.with().build());
    }

    public CompletableFuture<Response> curlAsync(final String requestCommand,
                                                 CurlJavaOptions curlJavaOptions) throws CurlException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.curl(requestCommand, curlJavaOptions);
            } catch (IllegalArgumentException e) {
                throw new CurlException(e);
            }
        }).toCompletableFuture();
    }

    public Response curl(final String requestCommand) throws CurlException {
        return curl(requestCommand, CurlJavaOptions.with().build());
    }

    public Response curl(final String requestCommand,
                         CurlJavaOptions curlJavaOptions) throws CurlException {
        try {
            final CommandLine commandLine = ReadArguments.getCommandLineFromRequest(requestCommand,
                    curlJavaOptions.getPlaceHolders());
            stopAndDisplayVersionIfThe(commandLine.hasOption(Arguments.VERSION.getOpt()));
            Request request = new RequestProvider(httpClient).buildRequest(commandLine);
            Supplier<Response> executor = () -> {
                try {
                    return httpClient.execute(request);
                } catch (IOException e) {
                    throw new CurlException(e);
                }
            };
            final Response response = interceptorsHandler.handleInterceptors(request, executor,
                    interceptorsHandler.getInterceptors(commandLine, curlJavaOptions.getInterceptors()));

            afterResponse.handle(commandLine, response);
            return response;
        } catch (final IllegalArgumentException e) {
            throw new CurlException(e);
        }
    }

    public String getVersion() {
        return Version.NUMBER;
    }

    public String getVersionWithBuildTime() {
        return Version.NUMBER + " (Build time : " + Version.BUILD_TIME + ")";
    }


}
