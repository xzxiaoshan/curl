package org.toilelibre.libe.outside.curl;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.toilelibre.libe.curl.Curl.curl;

public class CurlWebTest {
    @Test
    public void wrongHost () {
        curl("-k https://wrong.host.badssl.com/");
    }

    @Test
    public void proxyWithAuthentication() throws IOException {
        HttpResponse response = curl ("http://httpbin.org/get -x http://static.199.91.251.148.clients.your-server.de:3128/ -U user:password");
        String body = IOUtils.toString (response.getEntity ().getContent ());
        Assert.assertTrue (body.contains ("Host\":\"httpbin.org\""));
        Assert.assertTrue (Pattern.compile ("\"origin\":\"[0-9.]+, [0-9.]+\"").matcher (body).find ());
        Assert.assertFalse (body.contains ("Proxy-Authorization"));
    }
}
