package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.client.Client;
import org.toilelibre.libe.curl.client.httpclient5.HttpClient5Provider;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.RequestProvider;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.http.ResponseHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.toilelibre.libe.curl.VersionDisplay.stopAndDisplayVersionIfThe;

/**
 * Curl执行入口类
 *
 * @author 单红宇
 * @date 2024-05-06 19:04:01
 */
public class Curl {

    /**
     * httpClient
     */
    private final Client httpClient;

    /**
     * interceptorsHandler
     */
    private final InterceptorsHandler interceptorsHandler;

    /**
     * create
     *
     * @return Curl
     */
    public static Curl create() {
        return new Curl();
    }

    /**
     * create
     *
     * @param httpClient httpClient
     * @return Curl
     */
    public static Curl create(Client httpClient) {
        return new Curl(httpClient);
    }

    /**
     * Curl
     */
    public Curl() {
        this(null, null, null);
    }

    /**
     * Curl
     *
     * @param httpClient httpClient
     */
    public Curl(Client httpClient) {
        this(httpClient, null, null);
    }

    /**
     * Curl
     *
     * @param httpClient          httpClient
     * @param responseOutput      afterResponse
     * @param interceptorsHandler interceptorsHandler
     */
    public Curl(Client httpClient, OutputHandler responseOutput, InterceptorsHandler interceptorsHandler) {
        if (httpClient == null)
            httpClient = HttpClient5Provider.create(responseOutput);
        if (interceptorsHandler == null)
            interceptorsHandler = new InterceptorsHandler();

        this.httpClient = httpClient;
        this.interceptorsHandler = interceptorsHandler;
    }

    /**
     * curlToString
     *
     * @param requestCommand requestCommand
     * @return String
     * @throws CurlException CurlException
     */
    public String curlToString(final String requestCommand) throws CurlException {
        return curlToString(requestCommand, new CurlJavaOptions<>());
    }

    /**
     * curlToString
     *
     * @param requestCommand  requestCommand
     * @param curlJavaOptions curlJavaOptions
     * @return String
     * @throws CurlException CurlException
     */
    public String curlToString(final String requestCommand, CurlJavaOptions<Response> curlJavaOptions) throws CurlException {
        try {
            Response response = this.curl(requestCommand, curlJavaOptions, response1 -> response1);
            Response.Body body = response.body();
            if (body != null)
                return body.getContentString();
        } catch (final UnsupportedOperationException | IOException e) {
            throw new CurlException(e);
        }
        return null;
    }

    /**
     * curlAsyncToString
     *
     * @param requestCommand requestCommand
     * @return CompletableFuture
     * @throws CurlException CurlException
     */
    public CompletableFuture<String> curlAsyncToString(final String requestCommand) throws CurlException {
        return curlAsyncToString(requestCommand, new CurlJavaOptions<>());
    }

    /**
     * curlAsyncToString
     *
     * @param requestCommand  requestCommand
     * @param curlJavaOptions curlJavaOptions
     * @return CompletableFuture
     * @throws CurlException CurlException
     */
    public CompletableFuture<String> curlAsyncToString(final String requestCommand,
                                                       CurlJavaOptions<Response> curlJavaOptions) throws CurlException {
        return this.curlAsync(requestCommand, curlJavaOptions, response -> response).thenApply(res -> {
            try {
                return res.body().getContentString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * curlAsync
     *
     * @param requestCommand  requestCommand
     * @param responseHandler responseHandler
     * @return CompletableFuture
     * @throws CurlException CurlException
     */
    public CompletableFuture<Response> curlAsync(final String requestCommand,
                                                 final ResponseHandler<Response> responseHandler) throws CurlException {
        return curlAsync(requestCommand, new CurlJavaOptions<>(), responseHandler);
    }

    /**
     * curlAsync
     *
     * @param requestCommand  requestCommand
     * @param curlJavaOptions curlJavaOptions
     * @param responseHandler responseHandler
     * @param <T>             T
     * @return CompletableFuture
     * @throws CurlException CurlException
     */
    public <T> CompletableFuture<T> curlAsync(final String requestCommand,
                                              final CurlJavaOptions<T> curlJavaOptions,
                                              final ResponseHandler<T> responseHandler) throws CurlException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.curl(requestCommand, curlJavaOptions, responseHandler);
            } catch (IllegalArgumentException e) {
                throw new CurlException(e);
            }
        }).toCompletableFuture();
    }

    /**
     * curl
     *
     * @param requestCommand requestCommand
     * @return Response
     * @throws CurlException CurlException
     */
    public Response curl(final String requestCommand) throws CurlException {
        return curl(requestCommand, new CurlJavaOptions<>(), response -> response);
    }

    /**
     * curl
     *
     * @param requestCommand  requestCommand
     * @param responseHandler responseHandler
     * @param <T>             T
     * @return T
     * @throws CurlException CurlException
     */
    public <T> T curl(final String requestCommand,
                      ResponseHandler<T> responseHandler) throws CurlException {
        return curl(requestCommand, new CurlJavaOptions<>(), responseHandler);
    }

    /**
     * curl
     *
     * @param requestCommand  requestCommand
     * @param curlJavaOptions curlJavaOptions
     * @param responseHandler responseHandler
     * @param <T>             T
     * @return T
     * @throws CurlException CurlException
     */
    public <T> T curl(final String requestCommand,
                      CurlJavaOptions<T> curlJavaOptions,
                      ResponseHandler<T> responseHandler) throws CurlException {
        try {
            final CommandLine commandLine = ReadArguments.getCommandLineFromRequest(requestCommand,
                    curlJavaOptions.getPlaceHolders());
            stopAndDisplayVersionIfThe(commandLine.hasOption(Arguments.VERSION.getOpt()));
            Request request = new RequestProvider(httpClient).buildRequest(commandLine);
            String outputFilePath = commandLine.getOptionValue(Arguments.OUTPUT.getOpt());
            Supplier<T> executor = () -> {
                try {
                    return httpClient.execute(request, responseHandler, outputFilePath);
                } catch (IOException e) {
                    throw new CurlException(e);
                }
            };
            return interceptorsHandler.handleInterceptors(request, executor,
                    interceptorsHandler.getInterceptors(commandLine, curlJavaOptions.getInterceptors()));
        } catch (final IllegalArgumentException e) {
            throw new CurlException(e);
        }
    }

    /**
     * getVersion
     *
     * @return String
     */
    public String getVersion() {
        return Version.NUMBER;
    }

    /**
     * getVersionWithBuildTime
     *
     * @return String
     */
    public String getVersionWithBuildTime() {
        return Version.NUMBER + " (Build time : " + Version.BUILD_TIME + ")";
    }


}
