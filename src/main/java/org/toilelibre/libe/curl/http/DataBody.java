package org.toilelibre.libe.curl.http;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * 非表单类的数据体对象，例如Body为json
 *
 * @author shanhy
 * @date 2023-07-31 17:51
 */
public class DataBody implements RequestBody<byte[]> {
    
    private RequestBodyType bodyType;
    private byte[] data;
    private Charset encoding;

    private DataBody() {
        super();
    }

    public DataBody(byte[] data, RequestBodyType bodyType) {
        this.data = data;
        this.bodyType = bodyType;
    }

    public DataBody(byte[] data, RequestBodyType bodyType, Charset encoding) {
        this.data = data;
        this.bodyType = bodyType;
        this.encoding = encoding;
    }

    public Optional<Charset> getEncoding() {
        return Optional.ofNullable(this.encoding);
    }

    public int length() {
        /* calculate the content length based on the data provided */
        return data != null ? data.length : 0;
    }

    public String asString() {
        return !isBinary()
                ? new String(data, encoding)
                : "Binary data";
    }

    public boolean isBinary() {
        return encoding == null || data == null;
    }

    @Override
    public RequestBodyType getBodyType() {
        return this.bodyType;
    }

    @Override
    public byte[] getBody() {
        return this.data;
    }
}
