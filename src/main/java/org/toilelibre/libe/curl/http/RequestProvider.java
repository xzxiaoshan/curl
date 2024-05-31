package org.toilelibre.libe.curl.http;

import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.Arguments;
import org.toilelibre.libe.curl.CurlException;
import org.toilelibre.libe.curl.PayloadReader;
import org.toilelibre.libe.curl.SSLOption;
import org.toilelibre.libe.curl.Utils;
import org.toilelibre.libe.curl.client.Client;
import org.toilelibre.libe.curl.http.auth.AuthCredentials;
import org.toilelibre.libe.curl.http.auth.BasicAuthCredentials;
import org.toilelibre.libe.curl.http.auth.NTLMAuthCredentials;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * 请求对象构造
 *
 * @author shanhy
 * @date 2023-08-2 15:51
 */
public class RequestProvider {

    /**
     * client
     */
    private final Client client;

    /**
     * build
     *
     * @param client client
     * @return RequestProvider
     */
    public static RequestProvider build(Client client) {
        return new RequestProvider(client);
    }

    /**
     * RequestProvider
     *
     * @param client client
     */
    public RequestProvider(Client client) {
        this.client = client;
    }

    /**
     * buildRequest
     *
     * @param commandLine commandLine
     * @return Request
     * @throws CurlException CurlException
     */
    public Request buildRequest(final CommandLine commandLine) throws CurlException {
        String method = getMethod(commandLine);
        String uri = getUri(commandLine);
        ProxyInfo proxy = getProxy(commandLine);
        Map<String, List<String>> headers = getHeaders(commandLine, proxy, client.userAgent());
        Request.Options options = getOptions(commandLine, proxy);

        RequestBody<?> body;
        // 即便是GET请求，如果用户还是设定了data，依然正常传递data，这里注释掉原来对Method的判断
        // 条件：(asList("DELETE", "PATCH", "POST", "PUT").contains(method.toUpperCase()))
        body = PayloadReader.getData(commandLine);

        return Request.create(HttpMethod.valueOf(method), uri, headers, body, options);

    }

    /**
     * getUri
     *
     * @param commandLine commandLine
     * @return String
     */
    private String getUri(CommandLine commandLine) {
        return commandLine.getArgs()[0];
    }

    /**
     * getMethod
     *
     * @param cl cl
     * @return String
     * @throws CurlException CurlException
     */
    private String getMethod(final CommandLine cl) throws CurlException {
        return cl.getOptionValue(Arguments.HTTP_METHOD.getOpt()) == null ? determineVerbWithoutArgument(cl) : cl.getOptionValue(Arguments.HTTP_METHOD.getOpt());
    }

    /**
     * determineVerbWithoutArgument
     *
     * @param commandLine commandLine
     * @return String
     */
    private String determineVerbWithoutArgument(CommandLine commandLine) {
        if (commandLine.hasOption(Arguments.DATA.getOpt()) ||
                commandLine.hasOption(Arguments.DATA_RAW.getOpt()) ||
                commandLine.hasOption(Arguments.DATA_URLENCODED.getOpt()) ||
                commandLine.hasOption(Arguments.FORM.getOpt())) {
            return "POST";
        }
        return "GET";
    }

    /**
     * getHeaders
     *
     * @param commandLine      commandLine
     * @param proxyInfo        proxyInfo
     * @param defaultUserAgent defaultUserAgent
     * @return Map
     */
    private Map<String, List<String>> getHeaders(final CommandLine commandLine, ProxyInfo proxyInfo, String defaultUserAgent) {
        Map<String, List<String>> headersMap = new HashMap<>();

        final String[] headers = Optional.ofNullable(commandLine.getOptionValues(Arguments.HEADER.getOpt())).orElse(new String[0]);

        stream(headers)
                .filter(optionAsString -> optionAsString.indexOf(':') != -1).forEach(optionAsString -> {
                    int delimiterIndex = optionAsString.indexOf(':');
                    String name = optionAsString.substring(0, delimiterIndex);
                    String value = optionAsString.substring(delimiterIndex + 1).trim();
                    headersMap.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                });

        // -H 中的 UA 优先级高于单独单独 -A
        if (headersMap.keySet().stream().noneMatch(h -> Objects.equals(h.toLowerCase(), "user-agent")) &&
                commandLine.hasOption(Arguments.USER_AGENT.getOpt())) {
            headersMap.computeIfAbsent(Utils.USER_AGENT, k -> new ArrayList<>()).add(commandLine.getOptionValue(Arguments.USER_AGENT.getOpt()));
        }

        // 如果完全没有指定UA，则设定一个默认值
        if (headersMap.keySet().stream().noneMatch(h -> Objects.equals(h.toLowerCase(), "user-agent")) &&
                !commandLine.hasOption(Arguments.USER_AGENT.getOpt())) {
            headersMap.computeIfAbsent(Utils.USER_AGENT, k -> new ArrayList<>()).add(defaultUserAgent);
        }

        // 1.直接判断 Arguments.DATA_URLENCODED
        // 2.curl 在处理 POST 请求时，如没有明确指定 Content-Type，则默认为 application/x-www-form-urlencoded
        if (commandLine.hasOption(Arguments.DATA_URLENCODED.getOpt())
                || ((commandLine.hasOption(Arguments.DATA.getOpt()) || commandLine.hasOption(Arguments.DATA_RAW.getOpt()))
                && !headersMap.containsKey(Utils.CONTENT_TYPE))) {
            headersMap.computeIfAbsent(Utils.CONTENT_TYPE, k -> new ArrayList<>()).add("application/x-www-form-urlencoded");
        }
        // 表单附件上传，且未明确设定Content-Type，则自动生成
        if (commandLine.hasOption(Arguments.FORM.getOpt()) && !headersMap.containsKey(Utils.CONTENT_TYPE)) {
            headersMap.computeIfAbsent(Utils.CONTENT_TYPE, k -> new ArrayList<>()).add("multipart/form-data; boundary=".concat(Utils.generateBoundary()));
        }

        if (commandLine.hasOption(Arguments.NO_KEEPALIVE.getOpt())) {
            headersMap.computeIfAbsent(Utils.CONN_DIRECTIVE, k -> new ArrayList<>()).add(Utils.CONN_CLOSE);
        }

        // 代理
        if (proxyInfo != null) {
            String proxyUser = proxyInfo.getUserString();
            if (Utils.isNotBlank(proxyUser)) {
                headersMap.computeIfAbsent(Utils.PROXY_AUTHORIZATION, k -> new ArrayList<>())
                        .add("Basic ".concat(Base64.getEncoder().encodeToString(proxyUser.getBytes())));
            }
        }
        return headersMap;
    }

    /**
     * getProxy
     *
     * @param commandLine commandLine
     * @return ProxyInfo
     */
    private ProxyInfo getProxy(final CommandLine commandLine) {
        String proxyUserString = null;
        // 代理用户
        if (commandLine.hasOption(Arguments.PROXY_USER.getOpt())) {
            proxyUserString = commandLine.getOptionValue(Arguments.PROXY_USER.getOpt());
        } else if (commandLine.hasOption(Arguments.PROXY.getOpt()) &&
                commandLine.getOptionValue(Arguments.PROXY.getOpt()).contains("@")) {
            String proxy = commandLine.getOptionValue(Arguments.PROXY.getOpt());
            proxyUserString = proxy.substring(0, proxy.lastIndexOf("@")).replaceFirst("^[^/]+/+", "");
        }
        String scheme = null;
        String proxyHostString = null;
        // schema
        if (commandLine.hasOption(Arguments.PROXY.getOpt())) {
            String proxyString = commandLine.getOptionValue(Arguments.PROXY.getOpt());
            final int schemeIdx = proxyString.indexOf("://");
            if (schemeIdx > 0) {
                scheme = proxyString.substring(0, schemeIdx);
                proxyString = proxyString.substring(schemeIdx + 3);
            }
            // host:port
            proxyHostString = proxyString.replaceFirst("\\s*/\\s*$", "").replaceFirst(".+@", "");
        }
        if (proxyHostString != null)
            return new ProxyInfo(scheme, proxyHostString, proxyUserString);
        return null;
    }

    /**
     * getOptions
     *
     * @param commandLine commandLine
     * @param proxyInfo   proxyInfo
     * @return Options
     */
    private Request.Options getOptions(final CommandLine commandLine, ProxyInfo proxyInfo) {
        Request.Options options = new Request.Options();
        options.setProxy(proxyInfo);

        if (commandLine.hasOption(Arguments.CONNECT_TIMEOUT.getOpt())) {
            options.setConnectTimeout((int) ((Float.parseFloat(commandLine.getOptionValue(Arguments.CONNECT_TIMEOUT.getOpt()))) * 1000));
        }

        if (commandLine.hasOption(Arguments.MAX_TIME.getOpt())) {
            options.setMaxTimeout((int) ((Float.parseFloat(commandLine.getOptionValue(Arguments.MAX_TIME.getOpt()))) * 1000));
        }

        // 处理ntlm 和 auth user
        AuthCredentials authCredentials = null;
        if (commandLine.hasOption(Arguments.NTLM.getOpt())) {
            String uname = null;
            String pwd = null;
            String domain = null;
            if (commandLine.getOptionValue(Arguments.USER.getOpt()) != null) {
                final String[] authValue = commandLine.getOptionValue(Arguments.USER.getOpt()).split("(?<!\\\\):");
                final String[] userName = authValue[0].split("\\\\");
                uname = userName[1];
                pwd = authValue[1];
                domain = userName[0];
            }
            try {
                authCredentials = new NTLMAuthCredentials(uname, pwd, InetAddress.getLocalHost().getHostName(), domain);
            } catch (final UnknownHostException e1) {
                throw new CurlException(e1);
            }
        } else if (commandLine.getOptionValue(Arguments.USER.getOpt()) != null) {
            final String[] authValue = commandLine.getOptionValue(Arguments.USER.getOpt()).split("(?<!\\\\):");
            String userName = authValue[0];
            String password = authValue.length > 1 ? authValue[1] : null;
            URI uri = URI.create(commandLine.getArgs()[0]);
            authCredentials = new BasicAuthCredentials(userName, password, uri.getHost(), uri.getPort());
        }
        options.setAuthCredentials(authCredentials);

        // 请求响应压缩参数
        if (commandLine.hasOption(Arguments.COMPRESSED.getOpt())) {
            options.setCompressed(true);
        }

        // 跟踪重定向
        if (commandLine.hasOption(Arguments.FOLLOW_REDIRECTS.getOpt())) {
            options.setFollowRedirects(true);
        }

        // SSL 参数
        options.setSSLOptions(getSSLOptionsFromCommandLine(commandLine));

        return options;
    }

    /**
     * getSSLOptionsFromCommandLine
     *
     * @param commandLine commandLine
     * @return Map
     */
    private Map<SSLOption, List<String>> getSSLOptionsFromCommandLine(CommandLine commandLine) {
        Map<SSLOption, List<String>> sslOptionsMap = Stream.of(SSLOption.values())
                .filter(option -> commandLine.hasOption(option.value()))
                .collect(toMap(option -> option, option ->
                        asList(ofNullable(commandLine.getOptionValues(option.value()))
                                .orElse(new String[]{"true"}))));
        return !sslOptionsMap.isEmpty() ? sslOptionsMap : null;
    }

}
