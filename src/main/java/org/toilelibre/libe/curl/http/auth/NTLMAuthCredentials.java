package org.toilelibre.libe.curl.http.auth;

import java.util.Locale;

/**
 * @author shanhy
 * @date 2023-08-01 15:51
 */
public class NTLMAuthCredentials implements AuthCredentials {

    private final String userName;
    private final String password;
    private final String workstation;
    private final String domain;

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

    public String getUserName() {
        return userName;
    }

    public String getWorkstation() {
        return workstation;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.NTML;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
