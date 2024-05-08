package org.toilelibre.libe.curl.http.auth;

import lombok.Getter;

import java.util.Locale;

/**
 * NTLMAuthCredentials
 *
 * @author shanhy
 * @date 2023-08-01 15:51
 */
public class NTLMAuthCredentials implements AuthCredentials {

    /**
     * userName
     */
    @Getter
    private final String userName;
    /**
     * password
     */
    private final String password;
    /**
     * workstation
     */
    @Getter
    private final String workstation;
    /**
     * domain
     */
    @Getter
    private final String domain;

    /**
     * NTLMAuthCredentials
     *
     * @param userName    userName
     * @param password    password
     * @param workstation workstation
     * @param domain      domain
     */
    public NTLMAuthCredentials(
            final String userName,
            final String password,
            final String workstation,
            final String domain) {
        this.userName = userName;
        this.password = password;
        if (workstation != null) {
            this.workstation = workstation.toUpperCase(Locale.ROOT);
        } else {
            this.workstation = null;
        }
        this.domain = domain;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.NTLM;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "NTLMAuthCredentials{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", workstation='" + workstation + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }
}
