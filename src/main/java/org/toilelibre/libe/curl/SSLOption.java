package org.toilelibre.libe.curl;

/**
 * Curl参数中跟SSL有关的选项
 *
 * @author shanhy
 * @date 2023-08-02 17:15
 */
public enum SSLOption {

    TRUST_INSECURE(Arguments.TRUST_INSECURE.getOpt()),
    CERT_TYPE(Arguments.CERT_TYPE.getOpt()),
    CA_CERT(Arguments.CA_CERT.getOpt()),
    KEY(Arguments.KEY.getOpt()),
    KEY_TYPE(Arguments.KEY_TYPE.getOpt()),
    CERT(Arguments.CERT.getOpt()),
    TLS_V1(Arguments.TLS_V1.getOpt()),
    TLS_V10(Arguments.TLS_V10.getOpt()),
    TLS_V11(Arguments.TLS_V11.getOpt()),
    TLS_V12(Arguments.TLS_V12.getOpt()),
    SSL_V2(Arguments.SSL_V2.getOpt()),
    SSL_V3(Arguments.SSL_V3.getOpt());

    private final String val;

    SSLOption(String val) {
        this.val = val;
    }

    public String value() {
        return val;
    }

}
