package org.toilelibre.libe.curl.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.CurlException;
import org.toilelibre.libe.curl.IOUtils;
import org.toilelibre.libe.curl.SSLOption;
import org.toilelibre.libe.curl.Version;
import org.toilelibre.libe.curl.http.DataBody;
import org.toilelibre.libe.curl.http.FormBody;
import org.toilelibre.libe.curl.http.FormBodyPart;
import org.toilelibre.libe.curl.http.HttpMethod;
import org.toilelibre.libe.curl.http.ProxyInfo;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.RequestBodyType;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.http.auth.AuthCredentials;
import org.toilelibre.libe.curl.http.auth.AuthType;
import org.toilelibre.libe.curl.http.auth.BasicAuthCredentials;
import org.toilelibre.libe.curl.http.auth.NTLMAuthCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * 基于HttpClient实现Curl参数的调用
 *
 * @author shanhy
 * @date 2023-08-04 10:55
 */
public class HttpClientProvider extends AbstractClient {

    private final HttpClientBuilder httpClientBuilder;

    private static final HttpClientSSLHandler httpClientSSLHandler = new HttpClientSSLHandler();

    public static HttpClientProvider create(){
        return new HttpClientProvider(HttpClientBuilder.create());
    }

    public HttpClientProvider(HttpClientBuilder httpClientBuilder) {
        this.httpClientBuilder = httpClientBuilder;
    }

    @Override
    public void handleAuthCredentials(final AuthCredentials credentials) {
        if (credentials.getAuthType() == AuthType.NTML) {
            NTLMAuthCredentials ntlmCredentials = (NTLMAuthCredentials) credentials;
            final SystemDefaultCredentialsProvider systemDefaultCredentialsProvider =
                    new SystemDefaultCredentialsProvider();
            systemDefaultCredentialsProvider.setCredentials(AuthScope.ANY,
                    new NTCredentials(ntlmCredentials.getUserName(), ntlmCredentials.getPassword(), ntlmCredentials.getWorkstation(), ntlmCredentials.getDomain()));
            httpClientBuilder.setDefaultCredentialsProvider(systemDefaultCredentialsProvider);
        } else if (credentials.getAuthType() == AuthType.BASIC) {
            BasicAuthCredentials basicCredentials = (BasicAuthCredentials) credentials;
            final BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
            basicCredentialsProvider.setCredentials(new AuthScope(basicCredentials.getHost(), basicCredentials.getPort(), null, null),
                    new UsernamePasswordCredentials(basicCredentials.getUserName(), basicCredentials.getPassword()));
            httpClientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
        }
    }

    @Override
    public void handleSSL(Map<SSLOption, List<String>> sslOptions) {
        if(sslOptions != null)
            httpClientSSLHandler.handle(sslOptions, this.httpClientBuilder);
    }

    @Override
    public Response doExecute(Request request) throws IOException {
        HttpResponse httpClientResponse = send(request);
        return convertResponse(httpClientResponse, request);
    }

    /**
     * 转换详解结果对象
     *
     * @param httpClientResponse HttpClient请求的响应对象
     * @param request            原始请求request对象
     * @return Curl的Response对象
     * @throws IOException
     */
    private Response convertResponse(HttpResponse httpClientResponse, Request request) throws IOException {
        int status = httpClientResponse.getStatusLine().getStatusCode();
        String reason = httpClientResponse.getStatusLine().getReasonPhrase();

        if (status < 0) {
            throw new IOException(String.format("Invalid status(%s) executing %s %s", status,
                    request.getMethod().name(), request.url()));
        }

        Map<String, List<String>> headers = new LinkedHashMap<>();
        Arrays.stream(httpClientResponse.getAllHeaders()).filter(h -> h.getName() != null)
                .forEach(header -> headers.put(header.getName(), Collections.singletonList(header.getValue())));

        HttpEntity httpEntity = httpClientResponse.getEntity();
        long length = httpEntity != null ? httpEntity.getContentLength() : 0;
        InputStream stream = httpEntity !=null ? httpClientResponse.getEntity().getContent() : null;
        return Response.builder()
                .status(status)
                .reason(reason)
                .headers(headers)
                .request(request)
                .body(stream, length)
                .build();
    }

    /**
     * 发送请求
     *
     * @param request 原始请求对象
     * @return HttpResponse
     * @throws IOException
     */
    private HttpResponse send(Request request) throws IOException {
        return getHttpClient(request).execute(getHttpUriRequest(request));
    }

    /**
     * 创建 Httpclient 对象
     *
     * @param request 原始请求对象
     * @return
     * @throws CurlException
     */
    private HttpClient getHttpClient(final Request request) throws CurlException {
        if (!request.options().isCompressed()) {
            httpClientBuilder.disableContentCompression();
        }

        if (!request.options().isFollowRedirects()) {
            httpClientBuilder.disableRedirectHandling();
        }

        handleSSL(request.options().getSslOptions());
        return httpClientBuilder.build();
    }

    /**
     * 创建适用于HttpClient的HttpUriRequest对象
     *
     * @param request 原始请求对象
     * @return
     * @throws CurlException
     */
    private HttpUriRequest getHttpUriRequest(final Request request) throws CurlException {
        try {
            HttpMethod httpMethod = request.getMethod();
            RequestBuilder requestBuilder = RequestBuilder.create(httpMethod.name()).setUri(new URI(request.url()));

            // 即便是GET请求，如果用户还是设定了data，依然正常传递data，这里注释掉原来对Method的判断
//            if (asList(HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.POST, HttpMethod.PUT).contains(httpMethod) && request.body() != null) {
            if (request.body() != null) {
                HttpEntity httpEntity;
                if (asList(RequestBodyType.DATA, RequestBodyType.DATA_BINARY, RequestBodyType.DATA_URLENCODED).contains(request.body().getBodyType())) {
                    DataBody dataBody = (DataBody) request.body();
                    String reqContentType = request.getContentType();
                    if(reqContentType != null){
                        ContentType contentType = reqContentType.contains("charset") ? ContentType.parse(reqContentType)
                                : ContentType.create(reqContentType, Optional.of(request.getCharset()).orElse(StandardCharsets.UTF_8));
                        httpEntity = new ByteArrayEntity(dataBody.getBody(), contentType);
                    }else{
                        httpEntity = new ByteArrayEntity(dataBody.getBody());
                    }
                } else {
                    httpEntity = getEntityByFormData((FormBody) request.body());
                }
                requestBuilder.setEntity(httpEntity);
            }
            this.setHeaders(requestBuilder, request);
            this.setConfig(requestBuilder, request);

            return requestBuilder.build();
        } catch (URISyntaxException e) {
            throw new CurlException(e);
        }
    }

    /**
     * 设置请求头
     *
     * @param requestBuilder
     * @param request
     */
    private void setHeaders(final RequestBuilder requestBuilder, final Request request) {
        request.headers().forEach((name, values) -> values.forEach(val -> requestBuilder.addHeader(name, val)));
    }

    /**
     * 设置相关配置参数
     *
     * @param requestBuilder
     * @param request
     */
    private void setConfig(final RequestBuilder requestBuilder, final Request request) {
        final RequestConfig.Builder requestConfig = RequestConfig.custom();
        Request.Options options = request.options();
        ProxyInfo proxyInfo = options.getProxy();
        if (proxyInfo != null) {
            requestConfig.setProxy(HttpHost.create(proxyInfo.toString()));
        }
        if (options.connectTimeout() > 0) {
            requestConfig.setConnectTimeout(options.connectTimeout());
        }
        if (options.maxTimeout() > 0) {
            requestConfig.setSocketTimeout(options.maxTimeout());
        }
        requestBuilder.setConfig(requestConfig.build());
    }

    /**
     * 构造上传文件HttpEntity对象
     *
     * @param formBody
     * @return
     */
    private HttpEntity getEntityByFormData(final FormBody formBody) {
        final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();
        for (FormBodyPart formBodyPart : formBody.getBody()) {
            if (formBodyPart.isFile()) {
                multiPartBuilder.addPart(formBodyPart.getName(),
                        new FileBody(IOUtils.getFile(formBodyPart.getValue())));
            } else {
                multiPartBuilder.addTextBody(formBodyPart.getName(), formBodyPart.getValue());
            }
        }
        return multiPartBuilder.build();
    }

    @Override
    public String defaultUserAgent() {
        return String.format("%s/%s/%s, %s, (Java/%s)", Curl.class.getPackage().getName(),
                Version.NUMBER, Version.BUILD_TIME, "HttpClient", System.getProperty("java.version"));
    }

}
