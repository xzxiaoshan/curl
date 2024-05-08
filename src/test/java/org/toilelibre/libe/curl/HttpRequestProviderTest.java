package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.junit.Test;
import org.toilelibre.libe.curl.client.Client;
import org.toilelibre.libe.curl.client.httpclient5.HttpClient5Provider;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.RequestProvider;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class HttpRequestProviderTest {

    private final Client httpClientProvider = HttpClient5Provider.create();

    @Test
    public void curlWithoutVerbAndWithoutDataShouldBeTransformedAsGetRequest() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest("curl -H'Accept: application/json' " +
                "http://localhost/user/byId/1", Collections.emptyList());

        //when
        Request request = RequestProvider.build(httpClientProvider).buildRequest(commandLine);

        //then
        assertEquals("GET", request.httpMethod().toString());
    }

    @Test
    public void curlWithAPlaceholder() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest("curl -H $curl_placeholder_0 " +
                "http://localhost/user/byId/1", Collections.singletonList("Accept: application/json"));

        //when
        Request request = RequestProvider.build(httpClientProvider).buildRequest(commandLine);

        //then
        assertEquals("GET", request.httpMethod().toString());
    }


    @Test
    public void curlWithoutVerbAndWithDataShouldBeTransformedAsPostRequest() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest("curl -H'Accept: application/json' " +
                "-d'{\"id\":1,\"name\":\"John Doe\"}' http://localhost/user/", Collections.emptyList());

        //when
        Request request = RequestProvider.build(httpClientProvider).buildRequest(commandLine);

        //then
        assertEquals("POST", request.getMethod().name());
    }

    @Test
    public void proxyWithAuthentication() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest("http://httpbin.org/get -x http://87.98.174" +
                ".157:3128/ -U user:password", Collections.emptyList());

        //when
        Request request = RequestProvider.build(httpClientProvider).buildRequest(commandLine);

        //then
        assertEquals(request.getFirstHeader("Proxy-Authorization"), "Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    public void proxyWithAuthentication2() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest("-x http://localhost:80/ -U jack:insecure " +
                "http://www.baidu.com/", Collections.emptyList());

        //when
        Request request = RequestProvider.build(httpClientProvider).buildRequest(commandLine);

        //then
        assertEquals(request.getFirstHeader("Proxy-Authorization"), "Basic amFjazppbnNlY3VyZQ==");
        assertEquals(request.options().getProxy().getHostString(), "localhost:80");
    }

    @Test
    public void proxyWithAuthentication3() {
        //given
        CommandLine commandLine = ReadArguments.getCommandLineFromRequest("-x http://jack:insecure@localhost:80/ " +
                "http://www.baidu.com/", Collections.emptyList());

        //when
        Request request = RequestProvider.build(httpClientProvider).buildRequest(commandLine);

        //then
        assertEquals(request.getFirstHeader("Proxy-Authorization"), "Basic amFjazppbnNlY3VyZQ==");
        assertEquals(request.options().getProxy().getHostString(), "localhost:80");
    }
}
