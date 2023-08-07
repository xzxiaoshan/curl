package org.toilelibre.libe.outside.curl;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.http.Response;

import java.io.IOException;
import java.util.regex.Pattern;

public class CurlWebTest {
    @Test
    public void wrongHost () {
        Curl.create().curl ("-k https://wrong.host.badssl.com/");
    }

    @Ignore // keeps failing
    @Test
    public void proxyWithAuthentication () throws IOException {
        Response response = Curl.create().curl ("http://httpbin.org/get -x http://204.133.187.66:3128 -U " +
                "user:password");
        String body = IOUtils.toString (response.body().asInputStream());
        Assert.assertTrue (body.contains ("Host\": \"httpbin.org\""));
        Assert.assertTrue (Pattern.compile ("\"origin\": \"[a-zA-Z0-9.]+, [0-9.]+\"").matcher (body).find ());
        Assert.assertFalse (body.contains ("Proxy-Authorization"));
    }

    @Test
    public void sslTest (){
        Curl.create().curl ("curl -k https://lenovo.prod.ondemandconnectivity.com");
    }
}
