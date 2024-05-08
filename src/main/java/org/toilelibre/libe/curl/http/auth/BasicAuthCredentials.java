package org.toilelibre.libe.curl.http.auth;

import lombok.Getter;

/**
 * @author shanhy
 * @date 2023-08-01 15:51
 */
public class BasicAuthCredentials implements AuthCredentials {

    @Getter
    private final String userName;
    private final String password;
    @Getter
    private final String host;
    @Getter
    private final int port;

    /**
     * The constructor with the username and password arguments.
     *
     * @param userName the userName
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

    @Override
    public AuthType getAuthType() {
        return AuthType.BASIC;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 重写toString必须包含所有字段信息，其他地方有使用，用于重复判断
     *
     * @return str
     */
    @Override
    public String toString() {
        return "BasicAuthCredentials{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
