package org.toilelibre.libe.curl.http.auth;

/**
 * @author shanhy
 * @date 2023-08-01 15:47
 */
public interface AuthCredentials {

    AuthType getAuthType();

    String getPassword();
}
