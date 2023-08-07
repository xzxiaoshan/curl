package org.toilelibre.libe.curl.http;

import org.toilelibre.libe.curl.Utils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
public final class Response implements Closeable {

    private final int status;
    private final String reason;
    private final String contentType;
    private final Map<String, List<String>> headers;
    private final Body body;
    private final Request request;

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
        this.contentType = list == null || list.size() == 0 ? null : list.get(0);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        int status;
        String reason;
        Map<String, List<String>> headers;
        Body body;
        Request request;

        Builder() {
        }

        Builder(Response source) {
            this.status = source.status;
            this.reason = source.reason;
            this.headers = source.headers;
            this.body = source.body;
            this.request = source.request;
        }

        /**
         * @see Response#status
         */
        public Builder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * @see Response#reason
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @see Response#headers
         */
        public Builder headers(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * @see Response#body
         */
        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        /**
         * @see Response#body
         */
        public Builder body(InputStream inputStream, long length) {
            this.body = InputStreamBody.orNull(inputStream, length);
            return this;
        }

        /**
         * @see Response#body
         */
        public Builder body(byte[] data) {
            this.body = ByteArrayBody.orNull(data);
            return this;
        }

        /**
         * @see Response#body
         */
        public Builder body(String text, Charset charset) {
            this.body = ByteArrayBody.orNull(text, charset);
            return this;
        }

        /**
         * @see Response#request
         */
        public Builder request(Request request) {
            Utils.checkNotNull(request, "request is required");
            this.request = request;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }

    /**
     * status code. ex {@code 200}
     * <p>
     * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html" >rfc2616</a>
     */
    public int status() {
        return status;
    }

    /**
     * Nullable and not set when using http/2
     * <p>
     * See https://github.com/http2/http2-spec/issues/202
     */
    public String reason() {
        return reason;
    }

    /**
     * Returns a case-insensitive mapping of header names to their values.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * if present, the response had a body
     */
    public Body body() {
        return body;
    }

    /**
     * Http Content-Type for the response. Including charset。
     *
     * @return the ContentType.
     */
    public String getContentType() {
        return this.contentType;
    }

    public Charset getCharset() {
        String contentType = this.getContentType();
        if (contentType != null) {
            int idx = contentType.indexOf("charset=");
            if (idx != -1) {
                return Charset.forName(contentType.substring(idx + 8));// string 'charset=' length is 8
            }
        }
        return null;
    }

    public Charset getEncoding() {
        return this.getCharset();
    }

    /**
     * the request that generated this response
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

    @Override
    public void close() {
        Utils.ensureClosed(body);
    }

    public interface Body extends Closeable {

        /**
         * length in bytes, if known. Null if unknown or greater than {@link Integer#MAX_VALUE}.
         *
         * <br>
         * <br>
         * <br>
         * <b>Note</b><br>
         * This is an integer as most implementations cannot do bodies greater than 2GB.
         */
        long length();

        /**
         * True if {@link #asInputStream()} and {@link #asReader()} can be called more than once.
         */
        boolean isRepeatable();

        /**
         * It is the responsibility of the caller to close the stream.
         */
        InputStream asInputStream() throws IOException;

        /**
         * It is the responsibility of the caller to close the stream.
         *
         * @deprecated favor {@link Body#asReader(Charset)}
         */
        @Deprecated
        default Reader asReader() throws IOException {
            return asReader(StandardCharsets.UTF_8);
        }

        /**
         * It is the responsibility of the caller to close the stream.
         */
        Reader asReader(Charset charset) throws IOException;
    }

    private static final class InputStreamBody implements Response.Body {

        private final InputStream inputStream;
        private final Long length;

        private InputStreamBody(InputStream inputStream, long length) {
            this.inputStream = inputStream;
            this.length = length;
        }

        private static Body orNull(InputStream inputStream, long length) {
            if (inputStream == null) {
                return null;
            }
            return new InputStreamBody(inputStream, length);
        }

        @Override
        public long length() {
            return length;
        }

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public InputStream asInputStream() {
            return inputStream;
        }

        @SuppressWarnings("deprecation")
        @Override
        public Reader asReader() {
            return new InputStreamReader(inputStream, Utils.UTF_8);
        }

        @Override
        public Reader asReader(Charset charset) throws IOException {
            Utils.checkNotNull(charset, "charset should not be null");
            return new InputStreamReader(inputStream, charset);
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public String toString() {
            try {
                return new String(Utils.toByteArray(inputStream), Utils.UTF_8);
            } catch (Exception e) {
                return super.toString();
            }
        }
    }

    private static final class ByteArrayBody implements Response.Body {

        private final byte[] data;

        public ByteArrayBody(byte[] data) {
            this.data = data;
        }

        private static Body orNull(byte[] data) {
            if (data == null) {
                return null;
            }
            return new ByteArrayBody(data);
        }

        private static Body orNull(String text, Charset charset) {
            if (text == null) {
                return null;
            }
            Utils.checkNotNull(charset, "charset");
            return new ByteArrayBody(text.getBytes(charset));
        }

        @Override
        public long length() {
            return data.length;
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public InputStream asInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @SuppressWarnings("deprecation")
        @Override
        public Reader asReader() throws IOException {
            return new InputStreamReader(asInputStream(), Utils.UTF_8);
        }

        @Override
        public Reader asReader(Charset charset) throws IOException {
            Utils.checkNotNull(charset, "charset should not be null");
            return new InputStreamReader(asInputStream(), charset);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public String toString() {
            return Utils.decodeOrDefault(data, Utils.UTF_8, "Binary data");
        }
    }

    private static Map<String, List<String>> caseInsensitiveCopyOf(Map<String, List<String>> headers) {
        Map<String, List<String>> result =
                new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (!result.containsKey(headerName)) {
                result.put(headerName.toLowerCase(Locale.ROOT), new LinkedList<String>());
            }
            result.get(headerName).addAll(entry.getValue());
        }
        return result;
    }
}

