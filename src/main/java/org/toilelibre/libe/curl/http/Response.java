package org.toilelibre.libe.curl.http;

import lombok.Getter;
import org.toilelibre.libe.curl.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Http请求响应对象
 * <p>
 * An immutable response to an http invocation which only returns string content.
 *
 * @author shanhy
 * @date 2023-07-25 16:16
 */
public final class Response {

    /**
     * status
     */
    private final int status;
    /**
     * reason
     */
    private final String reason;
    /**
     * -- GETTER --
     * Http Content-Type for the response. Including charset。
     */
    @Getter
    private final String contentType;
    /**
     * headers
     */
    private final Map<String, List<String>> headers;
    /**
     * body
     */
    private final Body body;
    /**
     * request
     */
    private final Request request;

    /**
     * Response
     *
     * @param builder builder
     */
    private Response(Builder builder) {
        Utils.checkState(builder.request != null, "original request is required");
        this.status = builder.status;
        this.request = builder.request;
        this.reason = builder.reason; // nullable
        this.headers = (builder.headers != null)
                ? Collections.unmodifiableMap(caseInsensitiveCopyOf(builder.headers))
                : new LinkedHashMap<>();
        this.body = builder.body; // nullable

        List<String> list = this.headers.get(Utils.CONTENT_TYPE);
        this.contentType = list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * toBuilder
     *
     * @return Builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * builder
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder
     *
     * @author 单红宇
     * @date 2024-05-07 13:27:38
     */
    public static final class Builder {
        /**
         * status
         */
        int status;
        /**
         * reason
         */
        String reason;
        /**
         * headers
         */
        Map<String, List<String>> headers;
        /**
         * body
         */
        Body body;
        /**
         * request
         */
        Request request;

        /**
         * Builder
         */
        Builder() {
        }

        /**
         * Builder
         *
         * @param source source
         */
        Builder(Response source) {
            this.status = source.status;
            this.reason = source.reason;
            this.headers = source.headers;
            this.body = source.body;
            this.request = source.request;
        }

        /**
         * status
         *
         * @param status status
         * @return Builder
         * @see Response#status Response#status
         */
        public Builder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * reason
         *
         * @param reason reason
         * @return Builder
         * @see Response#reason Response#reason
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * headers
         *
         * @param headers headers
         * @return Builder
         * @see Response#headers Response#headers
         */
        public Builder headers(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * body
         *
         * @param body body
         * @return Builder
         * @see Response#body Response#body
         */
        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        /**
         * body
         *
         * @param path   path
         * @param length length
         * @return Builder
         */
        public Builder body(Path path, long length) {
            this.body = new FileBody(path, length);
            return this;
        }

        /**
         * body
         *
         * @param text text
         * @return Builder
         */
        public Builder body(String text) {
            this.body = new StringBody(text, StandardCharsets.UTF_8);
            return this;
        }

        /**
         * body
         *
         * @param text    text
         * @param charset charset
         * @return Builder
         * @see Response#body Response#body
         */
        public Builder body(String text, Charset charset) {
            this.body = new StringBody(text, charset);
            return this;
        }

        /**
         * request
         *
         * @param request request
         * @return Builder
         * @see Response#request Response#request
         */
        public Builder request(Request request) {
            Utils.checkNotNull(request, "request is required");
            this.request = request;
            return this;
        }

        /**
         * build
         *
         * @return Response
         */
        public Response build() {
            return new Response(this);
        }
    }

    /**
     * status code. ex {@code 200}
     * <p>
     * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html" >rfc2616</a>
     *
     * @return data
     */
    public int status() {
        return status;
    }

    /**
     * Nullable and not set when using http/2
     * <p>
     * See https://github.com/http2/http2-spec/issues/202
     *
     * @return String
     */
    public String reason() {
        return reason;
    }

    /**
     * Returns a case-insensitive mapping of header names to their values.
     *
     * @return Map
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * if present, the response had a body
     *
     * @return Body
     */
    public Body body() {
        return body;
    }

    /**
     * getCharset
     *
     * @return Charset
     */
    public Charset getCharset() {
        String conType = this.getContentType();
        if (conType != null) {
            int idx = conType.indexOf("charset=");
            if (idx != -1) {
                return Charset.forName(conType.substring(idx + 8));// string 'charset=' length is 8
            }
        }
        return null;
    }

    /**
     * getEncoding
     *
     * @return Charset
     */
    public Charset getEncoding() {
        return this.getCharset();
    }

    /**
     * the request that generated this response
     *
     * @return Request
     */
    public Request request() {
        return request;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HTTP/1.1 ").append(status);
        if (reason != null)
            builder.append(' ').append(reason);
        builder.append('\n');
        for (String field : headers.keySet()) {
            for (String value : Utils.valuesOrEmpty(headers, field)) {
                builder.append(field).append(": ").append(value).append('\n');
            }
        }
        if (body != null)
            builder.append('\n').append(body);
        return builder.toString();
    }

    /**
     * Body
     *
     * @author 单红宇
     * @date 2024-05-07 13:27:38
     */
    public interface Body {

        /**
         * length in bytes, if known. Null if unknown or greater than {@link Integer#MAX_VALUE}.
         *
         * <br>
         * <br>
         * <br>
         * <b>Note</b><br>
         * This is an integer as most implementations cannot do bodies greater than 2GB.
         *
         * @return data
         * @throws UnsupportedEncodingException UnsupportedEncodingException
         */
        long getContentLength() throws UnsupportedEncodingException;

        /**
         * getContentBytes
         *
         * @return data
         */
        byte[] getContentBytes() throws IOException;

        /**
         * getContentString
         *
         * @return String
         */
        default String getContentString() throws IOException {
            return this.getContentString(Utils.UTF_8);
        }

        /**
         * getContentString
         *
         * @param charset charset
         * @return String
         */
        String getContentString(Charset charset) throws IOException;
    }

    /**
     * InputStreamBody
     *
     * @author 单红宇
     * @date 2024-05-07 13:27:38
     */
    public static final class FileBody implements Response.Body {

        /**
         * path
         */
        private final Path path;

        /**
         * length
         */
        private final long length;

        /**
         * FileBody
         *
         * @param path          path
         * @param contentLength contentLength
         */
        public FileBody(Path path, long contentLength) {
            this.path = path;
            this.length = contentLength;
        }

        @Override
        public long getContentLength() {
            return this.length;
        }

        @Override
        public byte[] getContentBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public String getContentString(Charset charset) throws IOException {
            return Utils.decodeOrDefault(this.getContentBytes(), charset, "Binary data");
        }
    }

    /**
     * StringBody
     *
     * @author 单红宇
     * @date 2024-05-07 13:27:38
     */
    public static final class StringBody implements Response.Body {

        /**
         * text
         */
        private final String text;

        /**
         * charset
         */
        private final Charset charset;

        /**
         * StringBody
         *
         * @param text    data
         * @param charset charset
         */
        public StringBody(String text, Charset charset) {
            this.text = text;
            this.charset = charset;
        }

        @Override
        public long getContentLength() throws UnsupportedEncodingException {
            return this.getContentBytes().length;
        }

        @Override
        public byte[] getContentBytes() throws UnsupportedEncodingException {
            return this.text.getBytes(this.charset);
        }

        @Override
        public String getContentString() throws IOException {
            return text;
        }

        @Override
        public String getContentString(Charset charset) throws IOException {
            return Utils.decodeOrDefault(this.getContentBytes(), charset, null);
        }
    }

    /**
     * caseInsensitiveCopyOf
     *
     * @param headers headers
     * @return Map
     */
    private static Map<String, List<String>> caseInsensitiveCopyOf(Map<String, List<String>> headers) {
        Map<String, List<String>> result =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (!result.containsKey(headerName)) {
                result.put(headerName.toLowerCase(Locale.ROOT), new LinkedList<>());
            }
            result.get(headerName).addAll(entry.getValue());
        }
        return result;
    }
}

