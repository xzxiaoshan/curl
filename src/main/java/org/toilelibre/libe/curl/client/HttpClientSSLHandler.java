package org.toilelibre.libe.curl.client;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.toilelibre.libe.curl.CertFormat;
import org.toilelibre.libe.curl.CurlException;
import org.toilelibre.libe.curl.IOUtils;
import org.toilelibre.libe.curl.SSLOption;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.toilelibre.libe.curl.IOUtils.getFile;

/**
 * SSL配置处理类
 *
 * @author shanhy
 * @date 2023-08-02 14:07
 */
public final class HttpClientSSLHandler {

    private final static Map<Map<SSLOption, List<String>>, SSLConnectionSocketFactory> cachedSSLFactoriesForPerformance = new HashMap<>();

    public void handle(final Map<SSLOption, List<String>> sslOptionsMap, final HttpClientBuilder executor) throws CurlException {
        final SSLConnectionSocketFactory foundInCache = cachedSSLFactoriesForPerformance.get(sslOptionsMap);

        if (foundInCache != null) {
            executor.setSSLSocketFactory(foundInCache);
            return;
        }

        final SSLContextBuilder builder = new SSLContextBuilder();
        builder.setProtocol(getProtocol(sslOptionsMap));

        if (sslOptionsMap.containsKey(SSLOption.TRUST_INSECURE)) {
            setTrustInsecure(builder);
        }

        final CertFormat certFormat = sslOptionsMap.containsKey(SSLOption.CERT_TYPE) ?
                CertFormat.valueOf(sslOptionsMap.get(SSLOption.CERT_TYPE).get(0).toUpperCase()) :
                CertFormat.PEM;
        final HttpClientSSLHandler.CertPlusKeyInfo.Builder certAndKeysBuilder =
                HttpClientSSLHandler.CertPlusKeyInfo.newBuilder()
                        .cacert(sslOptionsMap.containsKey(SSLOption.CA_CERT) ?
                                sslOptionsMap.get(SSLOption.CA_CERT).get(0) : null)
                        .certFormat(certFormat)
                        .keyFormat(sslOptionsMap.containsKey(SSLOption.KEY) ?
                                sslOptionsMap.containsKey(SSLOption.KEY_TYPE) ?
                                        CertFormat.valueOf(sslOptionsMap.get(SSLOption.KEY_TYPE).get(0).toUpperCase()) : CertFormat.PEM : certFormat);


        if (sslOptionsMap.containsKey(SSLOption.CERT)) {
            final String entireOption = sslOptionsMap.get(SSLOption.CERT).get(0);
            final int certSeparatorIndex = getSslSeparatorIndex(entireOption);
            final String cert = certSeparatorIndex == -1 ? entireOption : entireOption.substring(0, certSeparatorIndex);
            certAndKeysBuilder.cert(cert)
                    .certPassphrase(certSeparatorIndex == -1 ? "" : entireOption.substring(certSeparatorIndex + 1))
                    .key(cert);
        }

        if (sslOptionsMap.containsKey(SSLOption.KEY)) {
            final String entireOption = sslOptionsMap.get(SSLOption.KEY).get(0);
            final int keySeparatorIndex = getSslSeparatorIndex(entireOption);
            final String key = keySeparatorIndex == -1 ? entireOption : entireOption.substring(0, keySeparatorIndex);
            certAndKeysBuilder.key(key)
                    .keyPassphrase(keySeparatorIndex == -1 ? "" : entireOption.substring(keySeparatorIndex + 1));
        }
        if (sslOptionsMap.containsKey(SSLOption.CERT) || sslOptionsMap.containsKey(SSLOption.KEY)) {
            addClientCredentials(builder, certAndKeysBuilder.build());
        }

        try {
            final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(builder.build(),
                    sslOptionsMap.containsKey(SSLOption.TRUST_INSECURE) ? NoopHostnameVerifier.INSTANCE :
                            SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            cachedSSLFactoriesForPerformance.put(sslOptionsMap, sslSocketFactory);
            executor.setSSLSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new CurlException(e);
        }
    }


    private void addClientCredentials(final SSLContextBuilder builder,
                                      final HttpClientSSLHandler.CertPlusKeyInfo certPlusKeyInfo) throws CurlException {
        try {
            final String keyPassword = certPlusKeyInfo.getKeyPassphrase() == null ?
                    certPlusKeyInfo.getCertPassphrase() : certPlusKeyInfo.getKeyPassphrase();
            final KeyStore keyStore = generateKeyStore(certPlusKeyInfo);
            builder.loadKeyMaterial(keyStore, keyPassword == null ? null : keyPassword.toCharArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new CurlException(e);
        }
    }

    private KeyStore generateKeyStore(final HttpClientSSLHandler.CertPlusKeyInfo certPlusKeyInfo)
            throws KeyStoreException, NoSuchAlgorithmException, java.security.cert.CertificateException, IOException, CurlException {
        final CertFormat certFormat = certPlusKeyInfo.getCertFormat();
        final File caCertFileObject = certPlusKeyInfo.getCacert() == null ? null : getFile(certPlusKeyInfo.getCacert());
        final File certFileObject = getFile(certPlusKeyInfo.getCert());
        final CertFormat keyFormat = certPlusKeyInfo.getKeyFormat();
        final File keyFileObject = getFile(certPlusKeyInfo.getKey());
        final char[] certPasswordAsCharArray = certPlusKeyInfo.getCertPassphrase() == null ? null :
                certPlusKeyInfo.getCertPassphrase().toCharArray();
        final char[] keyPasswordAsCharArray = certPlusKeyInfo.getKeyPassphrase() == null ? certPasswordAsCharArray :
                certPlusKeyInfo.getKeyPassphrase().toCharArray();
        final List<java.security.cert.Certificate> caCertificatesNotFiltered = caCertFileObject == null ?
                Collections.emptyList() :
                certFormat.generateCredentialsFromFileAndPassword(CertFormat.Kind.CERTIFICATE,
                        IOUtils.toByteArray(caCertFileObject), keyPasswordAsCharArray);
        final List<java.security.cert.Certificate> caCertificatesFiltered =
                caCertificatesNotFiltered.stream().filter((certificate) -> (certificate instanceof X509Certificate) && (((X509Certificate) certificate).getBasicConstraints() != -1)).collect(toList());
        final List<java.security.cert.Certificate> certificates =
                certFormat.generateCredentialsFromFileAndPassword(CertFormat.Kind.CERTIFICATE,
                        IOUtils.toByteArray(certFileObject), certPasswordAsCharArray);
        final List<PrivateKey> privateKeys =
                keyFormat.generateCredentialsFromFileAndPassword(CertFormat.Kind.PRIVATE_KEY,
                        IOUtils.toByteArray(keyFileObject), keyPasswordAsCharArray);

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        final java.security.cert.Certificate[] certificatesAsArray =
                certificates.toArray(new java.security.cert.Certificate[0]);
        IntStream.range(0, certificates.size()).forEach(i -> setCertificateEntry(keyStore, certificates, i));
        IntStream.range(0, caCertificatesFiltered.size()).forEach(i -> setCaCertificateEntry(keyStore,
                caCertificatesFiltered, i));
        IntStream.range(0, privateKeys.size()).forEach(i -> setPrivateKeyEntry(keyStore, privateKeys,
                keyPasswordAsCharArray, certificatesAsArray, i));
        return keyStore;
    }

    private int getSslSeparatorIndex(String entireOption) {
        return entireOption.matches("^[A-Za-z]:\\\\") && entireOption.lastIndexOf(':') == 1 ? -1 :
                entireOption.lastIndexOf(':');
    }

    private String getProtocol(final Map<SSLOption, List<String>> sslOptions) {
        if (sslOptions != null) {
            if (sslOptions.containsKey(SSLOption.TLS_V1)) {
                return "TLSv1";
            }
            if (sslOptions.containsKey(SSLOption.TLS_V10)) {
                return "TLSv1.0";
            }
            if (sslOptions.containsKey(SSLOption.TLS_V11)) {
                return "TLSv1.1";
            }
            if (sslOptions.containsKey(SSLOption.TLS_V12)) {
                return "TLSv1.2";
            }
            if (sslOptions.containsKey(SSLOption.SSL_V2)) {
                return "SSLv2";
            }
            if (sslOptions.containsKey(SSLOption.SSL_V3)) {
                return "SSLv3";
            }
        }
        return "TLS";
    }

    private void setTrustInsecure(final SSLContextBuilder builder) throws CurlException {
        try {
            builder.loadTrustMaterial(null, (chain, authType) -> true);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    private void setCaCertificateEntry(final KeyStore keyStore,
                                       final List<java.security.cert.Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry("ca-cert-alias-" + i, certificates.get(i));
        } catch (final KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    private void setCertificateEntry(final KeyStore keyStore,
                                     final List<java.security.cert.Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry("cert-alias-" + i, certificates.get(i));
        } catch (final KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    private void setPrivateKeyEntry(final KeyStore keyStore, final List<PrivateKey> privateKeys,
                                    final char[] passwordAsCharArray, final Certificate[] certificatesAsArray, final int i) {
        try {
            keyStore.setKeyEntry("key-alias-" + i, privateKeys.get(i), passwordAsCharArray, certificatesAsArray);
        } catch (final KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    static class CertPlusKeyInfo {

        private final CertFormat certFormat;
        private final CertFormat keyFormat;
        private final String cert;
        private final String certPassphrase;
        private final String cacert;
        private final String key;
        private final String keyPassphrase;

        private CertPlusKeyInfo(Builder builder) {
            certFormat = builder.certFormat;
            keyFormat = builder.keyFormat;
            cert = builder.cert;
            certPassphrase = builder.certPassphrase;
            cacert = builder.cacert;
            key = builder.key;
            keyPassphrase = builder.keyPassphrase;
        }

        static Builder newBuilder() {
            return new Builder();
        }

        CertFormat getCertFormat() {
            return certFormat;
        }

        CertFormat getKeyFormat() {
            return keyFormat;
        }

        String getCert() {
            return cert;
        }

        String getCertPassphrase() {
            return certPassphrase;
        }

        String getCacert() {
            return cacert;
        }

        String getKey() {
            return key;
        }

        String getKeyPassphrase() {
            return keyPassphrase;
        }


        static final class Builder {
            private CertFormat certFormat;
            private CertFormat keyFormat;
            private String cert;
            private String certPassphrase;
            private String cacert;
            private String key;
            private String keyPassphrase;

            private Builder() {
            }

            Builder certFormat(CertFormat val) {
                certFormat = val;
                return this;
            }

            Builder keyFormat(CertFormat val) {
                keyFormat = val;
                return this;
            }

            Builder cert(String val) {
                cert = val;
                return this;
            }

            Builder certPassphrase(String val) {
                certPassphrase = val;
                return this;
            }

            Builder cacert(String val) {
                cacert = val;
                return this;
            }

            Builder key(String val) {
                key = val;
                return this;
            }

            Builder keyPassphrase(String val) {
                keyPassphrase = val;
                return this;
            }

            CertPlusKeyInfo build() {
                return new CertPlusKeyInfo(this);
            }
        }
    }
}
