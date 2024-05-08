package org.toilelibre.libe.curl.http.auth;

/**
 * AuthCredentials
 *
 * @author shanhy
 * @date 2023-08-01 15:47
 */
public interface AuthCredentials {

    /**
     * getAuthType
     *
     * @return AuthType
     */
    AuthType getAuthType();

    /**
     * getPassword
     *
     * @return String
     */
    String getPassword();

    /**
     * 重写toString必须包含所有字段信息，其他地方有使用，用于重复判断
     *
     * @return str
     */
    String toString();

}
