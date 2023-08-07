package org.toilelibre.libe.curl.client;

import org.toilelibre.libe.curl.Curl;
import org.toilelibre.libe.curl.SSLOption;
import org.toilelibre.libe.curl.Version;
import org.toilelibre.libe.curl.http.Request;
import org.toilelibre.libe.curl.http.Response;
import org.toilelibre.libe.curl.http.auth.AuthCredentials;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Client 抽象类
 *
 * @author shanhy
 * @date 2023-08-04 10:55
 */
public abstract class AbstractClient implements Client{

    @Override
    public Response execute(Request request) throws IOException{
        AuthCredentials authCredentials = request.options().getAuthCredentials();
        if(authCredentials != null)
            handleAuthCredentials(authCredentials);
        Map<SSLOption, List<String>> sslOptions = request.options().getSslOptions();
        if(sslOptions != null)
            handleSSL(sslOptions);
        return doExecute(request);
    }

    @Override
    public String defaultUserAgent() {
        return String.format("%s/%s/%s, %s, (Java/%s)", Curl.class.getPackage().getName(),
                Version.NUMBER, Version.BUILD_TIME, "CurlClient", System.getProperty("java.version"));
    }

}
