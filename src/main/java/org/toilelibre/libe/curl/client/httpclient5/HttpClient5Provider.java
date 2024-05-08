package org.toilelibre.libe.curl.client.httpclient5;

import lombok.Setter;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.CurlException;
import org.toilelibre.libe.curl.IOUtils;
import org.toilelibre.libe.curl.OutputHandler;
import org.toilelibre.libe.curl.SSLOption;
import org.toilelibre.libe.curl.Utils;
import org.toilelibre.libe.curl.Version;
import org.toilelibre.libe.curl.client.Client;
import org.toilelibre.libe.curl.http.DataBody;
import org.toilelibre.libe.curl.http.FormBody;
import org.toilelibre.libe.curl.http.FormBodyPart;
import org.toilelibre.libe.curl.http.ProxyInfo;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.RequestBodyType;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.http.ResponseHandler;
import org.toilelibre.libe.curl.http.auth.AuthCredentials;
import org.toilelibre.libe.curl.http.auth.AuthType;
import org.toilelibre.libe.curl.http.auth.BasicAuthCredentials;
import org.toilelibre.libe.curl.http.auth.NTLMAuthCredentials;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * 基于HttpClient实现Curl参数的调用
 *
 * @author shanhy
 * @date 2023-08-04 10:55
 */
public class HttpClient5Provider implements Client {

    /**
     * outputHandler
     */
    @Setter
    private OutputHandler outputHandler;

    /**
     * httpClientBuilder
     */
    private final HttpClientBuilder httpClientBuilder;

    /**
     * httpClientContext
     */
    private final HttpClientContext httpClientContext;

    /**
     * cachedHttpClient
     * 可以考虑使用超时过期的缓存策略，CacheBuilder.newBuilder().expireAfterWrite(channelTimeout, TimeUnit.SECONDS).build();
     */
    private static final Map<String, HttpClient> cachedSSLHttpClient = new HashMap<>();

    /**
     * httpClientSSLHandler
     */
    private static final SSLConnectionSocketFactoryProvider sslConnectionSocketFactoryProvider = new SSLConnectionSocketFactoryProvider();

    /**
     * 连接池最大连接数
     */
    private static final int POOL_MAX_TOTAL = 100;

    /**
     * 每个路由（每个目标主机）的默认最大连接数
     */
    private static final int MAX_PER_ROUTE = 10;

    /**
     * create
     *
     * @return HttpClient5Provider
     */
    public static HttpClient5Provider create() {
        return create(null);
    }

    /**
     * create
     *
     * @param outputHandler outputHandler
     * @return HttpClient5Provider
     */
    public static HttpClient5Provider create(OutputHandler outputHandler) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .disableAutomaticRetries() // disable重试
                .disableConnectionState()
                .useSystemProperties();

        HttpClientContext clientContext = HttpClientContext.create();
        clientContext.setCookieStore(new BasicCookieStore());
        return new HttpClient5Provider(httpClientBuilder, clientContext, outputHandler);
    }

    /**
     * HttpClient5Provider
     *
     * @param httpClientBuilder httpClientBuilder
     * @param httpClientContext httpClientContext
     * @param outputHandler     outputHandler
     */
    public HttpClient5Provider(HttpClientBuilder httpClientBuilder, HttpClientContext httpClientContext, OutputHandler outputHandler) {
        this.httpClientBuilder = httpClientBuilder;
        this.httpClientContext = httpClientContext;
        if(outputHandler == null) {
            outputHandler = new OutputHandler();
        }
        this.outputHandler = outputHandler;
    }

    /**
     * handleAuthCredentials
     *
     * @param credentials credentials
     */
    public void handleAuthCredentials(final AuthCredentials credentials) {
        if (credentials.getAuthType() == AuthType.NTLM) {
            NTLMAuthCredentials ntlmCredentials = (NTLMAuthCredentials) credentials;
            final SystemDefaultCredentialsProvider systemDefaultCredentialsProvider = new SystemDefaultCredentialsProvider();
            if (ntlmCredentials.getUserName() != null) { // NTLM类型并且提供了账号密码信息
                systemDefaultCredentialsProvider.setCredentials(new AuthScope(null, -1),
                        new NTCredentials(ntlmCredentials.getUserName(), ntlmCredentials.getPassword().toCharArray(),
                                ntlmCredentials.getWorkstation(), ntlmCredentials.getDomain()));
            }
            httpClientContext.setCredentialsProvider(systemDefaultCredentialsProvider);
        } else if (credentials.getAuthType() == AuthType.BASIC) {
            BasicAuthCredentials basicCredentials = (BasicAuthCredentials) credentials;
            final BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
            char[] pwd = basicCredentials.getPassword() == null ? null : basicCredentials.getPassword().toCharArray();
            basicCredentialsProvider.setCredentials(new AuthScope(basicCredentials.getHost(), basicCredentials.getPort()),
                    new UsernamePasswordCredentials(basicCredentials.getUserName(), pwd));
            httpClientContext.setCredentialsProvider(basicCredentialsProvider);
        }
    }

    @Override
    public <T> T execute(Request request, ResponseHandler<? extends T> responseHandler, final String outputFilePath) throws IOException {
        AuthCredentials authCredentials = request.options().getAuthCredentials();
        if (authCredentials != null)
            handleAuthCredentials(authCredentials);
        return send(request, responseHandler, outputFilePath);
    }

    /**
     * 转换详解结果对象
     *
     * @param httpClientResponse HttpClient请求的响应对象
     * @param request            原始请求request对象
     * @param outputFilePath     outputFilePath
     * @return Curl的Response对象
     * @throws IOException IO异常
     */
    private Response convertResponse(ClassicHttpResponse httpClientResponse, final Request request, final String outputFilePath) throws IOException {
        int status = httpClientResponse.getCode();
        String reason = httpClientResponse.getReasonPhrase();

        if (status < 0) {
            throw new IOException(String.format("Invalid status(%s) executing %s %s", status,
                    request.getMethod().name(), request.url()));
        }

        Map<String, List<String>> headers = new LinkedHashMap<>();
        Arrays.stream(httpClientResponse.getHeaders()).filter(h -> h.getName() != null)
                .forEach(header -> headers.put(header.getName(), Collections.singletonList(header.getValue())));

        HttpEntity httpEntity = httpClientResponse.getEntity();
        long length = 0;
        Response.Body body = null;
        if (httpEntity != null) {
            length = httpEntity.getContentLength();
            InputStream contentStream = httpClientResponse.getEntity().getContent();
            // 根据参数output判断是否写入文件
            Path filePath = outputHandler.handle(contentStream, outputFilePath);
            Charset contentCharset = httpEntity.getContentEncoding() == null ? StandardCharsets.UTF_8 : Charset.forName(httpEntity.getContentEncoding());
            body = filePath != null ? new Response.FileBody(filePath, length) :
                    new Response.StringBody(new String(IOUtils.toByteArray(contentStream), contentCharset), contentCharset);
        }
        return Response.builder()
                .status(status)
                .reason(reason)
                .headers(headers)
                .request(request)
                .body(body)
                .build();
    }

    /**
     * 发送请求
     *
     * @param request         原始请求对象
     * @param responseHandler responseHandler
     * @param outputFilePath  outputFilePath
     * @param <T>             T
     * @return HttpResponse
     * @throws IOException IO异常
     */
    private <T> T send(Request request, ResponseHandler<? extends T> responseHandler, final String outputFilePath) throws IOException {
        httpClientContext.setRequestConfig(getRequestConfig(request));
        return getHttpClient(request.options()).execute(getHttpUriRequest(request), httpClientContext,
                (HttpClientResponseHandler<T>) response -> responseHandler.handleResponse(convertResponse(response, request, outputFilePath)));
    }

    /**
     * 创建 Httpclient 对象
     *
     * @param requestOptions requestOptions
     * @return HttpClient实例
     * @throws CurlException CurlException
     */
    private HttpClient getHttpClient(final Request.Options requestOptions) throws CurlException {
        // 因为创建 HttpClient 有一定的资源消耗，所以对于 SSL 参数完全相同的请求，重复使用 CloseableHttpClient 实例对象，这里使用缓存处理
        String reqOptionsMD5 = requestOptionsToMD5(requestOptions);
        return cachedSSLHttpClient.computeIfAbsent(reqOptionsMD5, k -> {
            httpClientBuilder.setConnectionManager(this.buildConnectionManager(requestOptions))
                    .evictExpiredConnections().evictIdleConnections(TimeValue.ofSeconds(5)).disableAutomaticRetries();
            httpClientBuilder.setProxy(buildProxy(requestOptions));
            return httpClientBuilder.build();
        });
    }

    /**
     * buildProxy
     *
     * @param options options
     * @return HttpHost
     * @throws CurlException CurlException
     */
    private HttpHost buildProxy(Request.Options options) throws CurlException {
        ProxyInfo proxyInfo = options.getProxy();
        if (proxyInfo != null) {
            try {
                return HttpHost.create(proxyInfo.toString());
            } catch (URISyntaxException e) {
                throw new CurlException(e);
            }
        }
        return null;
    }

    /**
     * buildConnectionManager
     *
     * @param requestOptions requestOptions
     * @return HttpClientConnectionManager
     */
    private HttpClientConnectionManager buildConnectionManager(final Request.Options requestOptions) {
        //兼容http以及https请求
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslConnectionSocketFactoryProvider.create(requestOptions.getSslOptions()))
                .build();
        //适配http以及https请求 通过new创建PoolingHttpClientConnectionManager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setDefaultSocketConfig(buildSocketConfig(requestOptions));
        connectionManager.setDefaultConnectionConfig(buildConnectionConfig(requestOptions));
        // Use TLS v1.3 only
        connectionManager.setDefaultTlsConfig(TlsConfig.custom()
                .setHandshakeTimeout(Timeout.ofSeconds(30))
                .setSupportedProtocols(TLS.V_1_0, TLS.V_1_1, TLS.V_1_2, TLS.V_1_3)
                .build());
        connectionManager.setMaxTotal(POOL_MAX_TOTAL);
        connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
        return connectionManager;
    }

    /**
     * buildConnectionConfig
     *
     * @param requestOptions requestOptions
     * @return ConnectionConfig
     */
    private ConnectionConfig buildConnectionConfig(final Request.Options requestOptions) {
        ConnectionConfig.Builder builder = ConnectionConfig.custom();
        // 如果没有设置connectTimeout但又设置了maxTimeout，则将connectTimeout也设置为maxTimeout
        if (requestOptions.connectTimeout() > 0) {
            builder.setConnectTimeout(requestOptions.connectTimeout(), TimeUnit.SECONDS);
        } else if (requestOptions.maxTimeout() > 0) {
            builder.setConnectTimeout(requestOptions.maxTimeout(), TimeUnit.SECONDS);
        }
        return builder.build();
    }

    /**
     * buildSocketConfig
     *
     * @param requestOptions requestOptions
     * @return SocketConfig
     */
    private SocketConfig buildSocketConfig(final Request.Options requestOptions) {
        SocketConfig.Builder socketConfig = SocketConfig.custom();
        // 对于Socket的读取超时（SO_TIMEOUT），如果没有显式设置，默认值通常是0，这意味着无限等待，
        // 即客户端在读取操作上将不会自动超时，除非遇到网络断开等其他异常情况。
        // 这意味着，如果不手动设置setSoTimeout，你的HTTP客户端可能会因为等待服务器响应而一直阻塞。
        // 因为httpclient并没有直接设置等同于curl max-time的时间反复，防止soTimeout无限期等待，我们这里将max-time设置到soTimeout中
        if (requestOptions.maxTimeout() > 0) {
            socketConfig.setSoTimeout(requestOptions.maxTimeout(), TimeUnit.SECONDS);
        }
        // 预留扩展，根据requestOptions中得信息填充socketConfig对象信息
        return socketConfig.build();
    }

    /**
     * 讲请求的所有选项转换为MD5，为了让选项相同的请求使用同一个HttpClient
     *
     * @param requestOptions requestOptions
     * @return String
     */
    private String requestOptionsToMD5(Request.Options requestOptions) {
        if (requestOptions == null) {
            return "NO-OPTIONS";
        }
        // basic info
        StringBuilder optionsStringBuilder = new StringBuilder();
        optionsStringBuilder.append(requestOptions.isCompressed())
                .append(requestOptions.isFollowRedirects())
                .append(requestOptions.connectTimeout())
                .append(requestOptions.maxTimeout());
        // ssl info
        Map<SSLOption, List<String>> sslOptionListMap = requestOptions.getSslOptions();
        if (sslOptionListMap != null) {
            for (SSLOption sslOption : SSLOption.values()) {
                optionsStringBuilder.append(sslOption.name()).append(":");
                List<String> list = sslOptionListMap.get(sslOption);
                if (list != null && !list.isEmpty()) {
                    for (String s : list) {
                        optionsStringBuilder.append(s).append(";");
                    }
                } else {
                    optionsStringBuilder.append("null");
                }
            }
        }
        // proxy info
        optionsStringBuilder.append(requestOptions.getProxy());
        // auth info
        optionsStringBuilder.append(requestOptions.getAuthCredentials());
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算消息的摘要
            byte[] digest = md.digest(optionsStringBuilder.toString().getBytes());
            // 将摘要转换为十六进制字符串
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new CurlException(e);
        }
    }

    /**
     * 字节到HEX的转换
     *
     * @param bytes bytes
     * @return String
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 创建适用于HttpClient的HttpUriRequest对象
     *
     * @param request 原始请求对象
     * @return HttpUriRequest对象
     * @throws CurlException CurlException
     */
    private HttpUriRequest getHttpUriRequest(final Request request) throws CurlException {
        HttpUriRequestBase httpUriRequestBase = new HttpUriRequestBase(request.getMethod().name(),
                URI.create(request.url()));
        // 即便是GET请求，如果用户还是设定了data，依然正常传递data，这里已经除去原来对Method的判断
        // 原条件判断：asList(HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.POST, HttpMethod.PUT).contains(httpMethod) && request.body() != null
        if (request.body() != null) {
            HttpEntity httpEntity;
            if (asList(RequestBodyType.DATA, RequestBodyType.DATA_BINARY, RequestBodyType.DATA_URLENCODED).contains(request.body().getBodyType())) {
                DataBody dataBody = (DataBody) request.body();
                String reqContentType = request.getContentType();
                if (reqContentType != null) {
                    ContentType contentType = reqContentType.contains("charset") ? ContentType.parse(reqContentType)
                            : ContentType.create(reqContentType, Optional.of(request.getCharset()).orElse(StandardCharsets.UTF_8));
                    httpEntity = new ByteArrayEntity(dataBody.getBody(), contentType);
                } else {
                    httpEntity = new ByteArrayEntity(dataBody.getBody(), null);
                }
            } else {
                httpEntity = getEntityByFormData((FormBody) request.body(), ContentType.parse(request.headers().get(Utils.CONTENT_TYPE).get(0)));
            }
            httpUriRequestBase.setEntity(httpEntity);
        }
        this.setHeaders(httpUriRequestBase, request);

        return httpUriRequestBase;
    }

    /**
     * 设置请求头
     *
     * @param httpUriRequest httpUriRequest
     * @param request        request
     */
    private void setHeaders(final HttpUriRequest httpUriRequest, final Request request) {
        request.headers().forEach((name, values) -> values.forEach(val -> httpUriRequest.addHeader(name, val)));
    }

    /**
     * 设置相关配置参数
     *
     * @param request request
     * @return requestConfig
     */
    private RequestConfig getRequestConfig(final Request request) {
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        // standard-strict 使用 HttpClient 5.0 时优先考虑cookie 策略
        requestConfig.setCookieSpec(StandardCookieSpec.STRICT);
        Request.Options options = request.options();
        // 在curl中max-time是一个从发送请求到完整接收响应总的时间范围，超出时间没有完成curl终止退出
        // 那么在httpclient中这里我们设置connectionTimeout和responseTimeout均为maxTimeout的值，不做那么严谨的总时间处理
        if (options.maxTimeout() > 0) {
            requestConfig.setResponseTimeout(options.maxTimeout(), TimeUnit.MILLISECONDS);
        }
        requestConfig.setContentCompressionEnabled(request.options().isCompressed());
        requestConfig.setRedirectsEnabled(request.options().isFollowRedirects());

        return requestConfig.build();
    }

    /**
     * 构造上传文件HttpEntity对象
     *
     * @param formBody    formBody
     * @param contentType contentType
     * @return HttpEntity实例
     */
    private HttpEntity getEntityByFormData(final FormBody formBody, final ContentType contentType) {
        final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();
        for (FormBodyPart formBodyPart : formBody.getBody()) {
            if (formBodyPart.isFile()) {
                multiPartBuilder.addPart(formBodyPart.getName(),
                        new FileBody(IOUtils.getFile(formBodyPart.getValue()), ContentType.APPLICATION_OCTET_STREAM));
            } else {
                multiPartBuilder.addTextBody(formBodyPart.getName(), formBodyPart.getValue(), ContentType.TEXT_PLAIN);
            }
        }
        multiPartBuilder.setContentType(contentType);
        multiPartBuilder.setMode(HttpMultipartMode.EXTENDED);
        return multiPartBuilder.build();
    }

    @Override
    public String userAgent() {
        return String.format("%s/%s/%s, %s, (Java/%s)", Curl.class.getPackage().getName(),
                Version.NUMBER, Version.BUILD_TIME, "HttpClient", System.getProperty("java.version"));
    }

}
