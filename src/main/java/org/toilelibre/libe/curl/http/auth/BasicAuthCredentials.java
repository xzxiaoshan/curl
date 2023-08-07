package org.toilelibre.libe.curl.http.auth;

/**
 * @author shanhy
 * @date 2023-08-01 15:51
 */
public class BasicAuthCredentials implements AuthCredentials {

    private final String userName;
    private final String password;
    private final String host;
    private final int port;

    /**
     * The constructor with the username and password arguments.
     *
     * @param userName the user name
     * @param password the password
     */
    public BasicAuthCredentials(final String userName,
                                final String password,
                                final String host,
                                final int port) {
        this.userName = userName;
        this.password = password;
        this.host = host;
        this.port = port < 0 ? -1 : port;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.BASIC;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
