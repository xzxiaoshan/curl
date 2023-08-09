package org.toilelibre.libe.curl;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * IO工具类
 */
public final class IOUtils {

    private static class StringBuilderWriter extends Writer implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 8461966367767048539L;
        private final StringBuilder builder;

        StringBuilderWriter() {
            this.builder = new StringBuilder();
        }

        @Override
        public Writer append(final char value) {
            this.builder.append(value);
            return this;
        }

        @Override
        public Writer append(final CharSequence value) {
            this.builder.append(value);
            return this;
        }

        @Override
        public Writer append(final CharSequence value, final int start, final int end) {
            this.builder.append(value, start, end);
            return this;
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public String toString() {
            return this.builder.toString();
        }

        @Override
        public void write(final char[] value, final int offset, final int length) {
            this.builder.append(value, offset, length);
        }

        @Override
        public void write(final String value) {
            this.builder.append(value);
        }
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static final int EOF = -1;

    private static void copy(final InputStream input, final Writer output, final Charset encoding) throws IOException {
        final InputStreamReader in = new InputStreamReader(input, encoding);
        IOUtils.copy(in, output);
    }

    private static int copy(final Reader input, final Writer output) throws IOException {
        final long count = IOUtils.copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    private static long copyLarge(final Reader input, final Writer output) throws IOException {
        return IOUtils.copyLarge(input, output, new char[IOUtils.DEFAULT_BUFFER_SIZE]);
    }

    private static long copyLarge(final Reader input, final Writer output, final char[] buffer) throws IOException {
        long count = 0;
        int n;
        while (IOUtils.EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static byte[] toByteArray(final File fileObject) throws IOException {
        final FileInputStream fis = new FileInputStream(fileObject);
        return IOUtils.toByteArray(fis, (int) fileObject.length());

    }

    public static byte[] toByteArray(final InputStream fis, final int length) throws IOException {
        final byte[] result = new byte[length];
        final DataInputStream dis = new DataInputStream(fis);
        dis.readFully(result);
        dis.close();
        fis.close();
        return result;
    }

    public static byte[] toByteArray(final InputStream fis) throws IOException {
        byte[] chunk = new byte[4096];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read;
        while ((read = fis.read(chunk)) != -1) {
            byteArrayOutputStream.write(chunk, 0, read);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static String quietToString(final InputStream inputStream) {
        try {
            if (inputStream == null) return null;
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (IOException e) {
            throw new CurlException(e);
        }
    }

    public static File getFile(final String filePath) {
        final File file = new File(filePath);
        if (file.exists()) {
            return file;
        }
        return new File(System.getProperty("user.dir") + File.separator + filePath);
    }

    public static byte[] dataBehind(final String ref) {
        try {
            return IOUtils.toByteArray(new File(ref.substring(1).trim()));
        } catch (final IOException e) {
            throw new CurlException(e);
        }
    }

    public static boolean isFile(final String ref) {
        final String fileName = (Optional.ofNullable(ref).orElse("").trim() + " ");
        if (fileName.charAt(0) != '@') {
            return false;
        }

        final File file = new File(fileName.substring(1).trim().replaceAll("\"",""));
        return file.exists() && file.isFile();
    }

    private static String toString(final InputStream input, final Charset encoding) throws IOException {
        final StringBuilderWriter sw = new StringBuilderWriter();
        IOUtils.copy(input, sw, encoding);
        return sw.toString();
    }
}
