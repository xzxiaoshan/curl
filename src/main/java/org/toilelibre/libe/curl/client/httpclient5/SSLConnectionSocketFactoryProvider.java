package org.toilelibre.libe.curl.client.httpclient5;

import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
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
import java.security.cert.CertificateException;
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
public final class SSLConnectionSocketFactoryProvider {

    /**
     * create
     *
     * @param sslOptionsMap sslOptionsMap
     * @return SSLConnectionSocketFactory
     * @throws CurlException CurlException
     */
    public SSLConnectionSocketFactory create(Map<SSLOption, List<String>> sslOptionsMap) throws CurlException {
        sslOptionsMap = sslOptionsMap == null ? new HashMap<>() : sslOptionsMap;
        final SSLContextBuilder builder = new SSLContextBuilder();
        builder.setProtocol(getProtocol(sslOptionsMap));

        if (sslOptionsMap.containsKey(SSLOption.TRUST_INSECURE)) {
            setTrustInsecure(builder);
        }

        final CertFormat certFormat = sslOptionsMap.containsKey(SSLOption.CERT_TYPE) ?
                CertFormat.valueOf(sslOptionsMap.get(SSLOption.CERT_TYPE).get(0).toUpperCase()) :
                CertFormat.PEM;
        final SSLConnectionSocketFactoryProvider.CertPlusKeyInfo.Builder certAndKeysBuilder;
        if (sslOptionsMap.containsKey(SSLOption.KEY)) {
            certAndKeysBuilder = CertPlusKeyInfo.newBuilder()
                    .cacert(sslOptionsMap.containsKey(SSLOption.CA_CERT) ?
                            sslOptionsMap.get(SSLOption.CA_CERT).get(0) : null)
                    .certFormat(certFormat)
                    .keyFormat(sslOptionsMap.containsKey(SSLOption.KEY_TYPE) ?
                            CertFormat.valueOf(sslOptionsMap.get(SSLOption.KEY_TYPE).get(0).toUpperCase()) : CertFormat.PEM);
        } else {
            certAndKeysBuilder = CertPlusKeyInfo.newBuilder()
                    .cacert(sslOptionsMap.containsKey(SSLOption.CA_CERT) ?
                            sslOptionsMap.get(SSLOption.CA_CERT).get(0) : null)
                    .certFormat(certFormat)
                    .keyFormat(certFormat);
        }

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
            return new SSLConnectionSocketFactory(builder.build(),
                    sslOptionsMap.containsKey(SSLOption.TRUST_INSECURE) ? NoopHostnameVerifier.INSTANCE :
                            HttpsSupport.getDefaultHostnameVerifier());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new CurlException(e);
        }
    }


    /**
     * addClientCredentials
     *
     * @param builder         builder
     * @param certPlusKeyInfo certPlusKeyInfo
     * @throws CurlException CurlException
     */
    private void addClientCredentials(final SSLContextBuilder builder,
                                      final SSLConnectionSocketFactoryProvider.CertPlusKeyInfo certPlusKeyInfo) throws CurlException {
        try {
            final String keyPassword = certPlusKeyInfo.getKeyPassphrase() == null ?
                    certPlusKeyInfo.getCertPassphrase() : certPlusKeyInfo.getKeyPassphrase();
            final KeyStore keyStore = generateKeyStore(certPlusKeyInfo);
            builder.loadKeyMaterial(keyStore, keyPassword == null ? null : keyPassword.toCharArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new CurlException(e);
        }
    }

    /**
     * generateKeyStore
     *
     * @param certPlusKeyInfo certPlusKeyInfo
     * @return KeyStore
     * @throws KeyStoreException        KeyStoreException
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     * @throws CertificateException     CertificateException
     * @throws IOException              IOException
     * @throws CurlException            CurlException
     */
    private KeyStore generateKeyStore(final SSLConnectionSocketFactoryProvider.CertPlusKeyInfo certPlusKeyInfo)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, CurlException {
        final CertFormat certFormat = certPlusKeyInfo.getCertFormat();
        final File caCertFileObject = certPlusKeyInfo.getCacert() == null ? null : getFile(certPlusKeyInfo.getCacert());
        final File certFileObject = getFile(certPlusKeyInfo.getCert());
        final CertFormat keyFormat = certPlusKeyInfo.getKeyFormat();
        final File keyFileObject = getFile(certPlusKeyInfo.getKey());
        final char[] certPasswordAsCharArray = certPlusKeyInfo.getCertPassphrase() == null ? null :
                certPlusKeyInfo.getCertPassphrase().toCharArray();
        final char[] keyPasswordAsCharArray = certPlusKeyInfo.getKeyPassphrase() == null ? certPasswordAsCharArray :
                certPlusKeyInfo.getKeyPassphrase().toCharArray();
        final List<Certificate> caCertificatesNotFiltered = caCertFileObject == null ?
                Collections.emptyList() :
                certFormat.generateCredentialsFromFileAndPassword(CertFormat.Kind.CERTIFICATE,
                        IOUtils.toByteArray(caCertFileObject), keyPasswordAsCharArray);
        final List<Certificate> caCertificatesFiltered =
                caCertificatesNotFiltered.stream()
                        .filter(certificate -> (certificate instanceof X509Certificate) && (((X509Certificate) certificate).getBasicConstraints() != -1))
                        .collect(toList());
        final List<Certificate> certificates =
                certFormat.generateCredentialsFromFileAndPassword(CertFormat.Kind.CERTIFICATE,
                        IOUtils.toByteArray(certFileObject), certPasswordAsCharArray);
        final List<PrivateKey> privateKeys =
                keyFormat.generateCredentialsFromFileAndPassword(CertFormat.Kind.PRIVATE_KEY,
                        IOUtils.toByteArray(keyFileObject), keyPasswordAsCharArray);

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        final Certificate[] certificatesAsArray =
                certificates.toArray(new Certificate[0]);
        IntStream.range(0, certificates.size()).forEach(i -> setCertificateEntry(keyStore, certificates, i));
        IntStream.range(0, caCertificatesFiltered.size()).forEach(i -> setCaCertificateEntry(keyStore,
                caCertificatesFiltered, i));
        IntStream.range(0, privateKeys.size()).forEach(i -> setPrivateKeyEntry(keyStore, privateKeys,
                keyPasswordAsCharArray, certificatesAsArray, i));
        return keyStore;
    }

    /**
     * getSslSeparatorIndex
     *
     * @param entireOption entireOption
     * @return data
     */
    private int getSslSeparatorIndex(String entireOption) {
        return entireOption.matches("^[A-Za-z]:\\\\") && entireOption.lastIndexOf(':') == 1 ? -1 :
                entireOption.lastIndexOf(':');
    }

    /**
     * getProtocol
     *
     * @param sslOptions sslOptions
     * @return String
     */
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
//        return "TLS";
        return "TLSv1.3";
    }

    /**
     * setTrustInsecure
     *
     * @param builder builder
     * @throws CurlException CurlException
     */
    private void setTrustInsecure(final SSLContextBuilder builder) throws CurlException {
        try {
            builder.loadTrustMaterial(null, (chain, authType) -> true);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    /**
     * setCaCertificateEntry
     *
     * @param keyStore     keyStore
     * @param certificates certificates
     * @param i            i
     */
    private void setCaCertificateEntry(final KeyStore keyStore,
                                       final List<Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry("ca-cert-alias-" + i, certificates.get(i));
        } catch (final KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    /**
     * setCertificateEntry
     *
     * @param keyStore     keyStore
     * @param certificates certificates
     * @param i            i
     */
    private void setCertificateEntry(final KeyStore keyStore,
                                     final List<Certificate> certificates, final int i) {
        try {
            keyStore.setCertificateEntry("cert-alias-" + i, certificates.get(i));
        } catch (final KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    /**
     * setPrivateKeyEntry
     *
     * @param keyStore            keyStore
     * @param privateKeys         privateKeys
     * @param passwordAsCharArray passwordAsCharArray
     * @param certificatesAsArray certificatesAsArray
     * @param i                   i
     */
    private void setPrivateKeyEntry(final KeyStore keyStore, final List<PrivateKey> privateKeys,
                                    final char[] passwordAsCharArray, final Certificate[] certificatesAsArray, final int i) {
        try {
            keyStore.setKeyEntry("key-alias-" + i, privateKeys.get(i), passwordAsCharArray, certificatesAsArray);
        } catch (final KeyStoreException e) {
            throw new CurlException(e);
        }
    }

    /**
     * CertPlusKeyInfo
     *
     * @author 单红宇
     * @date 2024-05-06 18:45:13
     */
    static class CertPlusKeyInfo {

        /**
         * certFormat
         */
        private final CertFormat certFormat;
        /**
         * keyFormat
         */
        private final CertFormat keyFormat;
        /**
         * cert
         */
        private final String cert;
        /**
         * certPassphrase
         */
        private final String certPassphrase;
        /**
         * cacert
         */
        private final String cacert;
        /**
         * key
         */
        private final String key;
        /**
         * keyPassphrase
         */
        private final String keyPassphrase;

        /**
         * CertPlusKeyInfo
         *
         * @param builder builder
         */
        private CertPlusKeyInfo(Builder builder) {
            certFormat = builder.certFormat;
            keyFormat = builder.keyFormat;
            cert = builder.cert;
            certPassphrase = builder.certPassphrase;
            cacert = builder.cacert;
            key = builder.key;
            keyPassphrase = builder.keyPassphrase;
        }

        /**
         * newBuilder
         *
         * @return Builder
         */
        static Builder newBuilder() {
            return new Builder();
        }

        /**
         * getCertFormat
         *
         * @return CertFormat
         */
        CertFormat getCertFormat() {
            return certFormat;
        }

        /**
         * getKeyFormat
         *
         * @return CertFormat
         */
        CertFormat getKeyFormat() {
            return keyFormat;
        }

        /**
         * getCert
         *
         * @return String
         */
        String getCert() {
            return cert;
        }

        /**
         * getCertPassphrase
         *
         * @return String
         */
        String getCertPassphrase() {
            return certPassphrase;
        }

        /**
         * getCacert
         *
         * @return String
         */
        String getCacert() {
            return cacert;
        }

        /**
         * getKey
         *
         * @return String
         */
        String getKey() {
            return key;
        }

        /**
         * getKeyPassphrase
         *
         * @return String
         */
        String getKeyPassphrase() {
            return keyPassphrase;
        }


        /**
         * Builder
         *
         * @author 单红宇
         * @date 2024-05-06 18:45:13
         */
        static final class Builder {
            /**
             * certFormat
             */
            private CertFormat certFormat;
            /**
             * keyFormat
             */
            private CertFormat keyFormat;
            /**
             * cert
             */
            private String cert;
            /**
             * certPassphrase
             */
            private String certPassphrase;
            /**
             * cacert
             */
            private String cacert;
            /**
             * key
             */
            private String key;
            /**
             * keyPassphrase
             */
            private String keyPassphrase;

            /**
             * Builder
             */
            private Builder() {
            }

            /**
             * certFormat
             *
             * @param val val
             * @return Builder
             */
            Builder certFormat(CertFormat val) {
                certFormat = val;
                return this;
            }

            /**
             * keyFormat
             *
             * @param val val
             * @return Builder
             */
            Builder keyFormat(CertFormat val) {
                keyFormat = val;
                return this;
            }

            /**
             * cert
             *
             * @param val val
             * @return Builder
             */
            Builder cert(String val) {
                cert = val;
                return this;
            }

            /**
             * certPassphrase
             *
             * @param val val
             * @return Builder
             */
            Builder certPassphrase(String val) {
                certPassphrase = val;
                return this;
            }

            /**
             * cacert
             *
             * @param val val
             * @return Builder
             */
            Builder cacert(String val) {
                cacert = val;
                return this;
            }

            /**
             * key
             *
             * @param val val
             * @return Builder
             */
            Builder key(String val) {
                key = val;
                return this;
            }

            /**
             * keyPassphrase
             *
             * @param val val
             * @return Builder
             */
            Builder keyPassphrase(String val) {
                keyPassphrase = val;
                return this;
            }

            /**
             * build
             *
             * @return CertPlusKeyInfo
             */
            CertPlusKeyInfo build() {
                return new CertPlusKeyInfo(this);
            }
        }
    }
}
