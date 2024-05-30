package org.toilelibre.libe.curl;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Utilities, typically copied in from guava, so as to avoid dependency conflicts.
 *
 * @author shanhy
 * @date 2023-07-25 16:37
 */
public final class Utils {

    /**
     * MULTIPART_CHARS
     */
    private static final char[] MULTIPART_CHARS =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * The HTTP User-Agent header field name.
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * The HTTP Proxy-Authorization header field name.
     */
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * The HTTP Content-Type header field name.
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * The HTTP Connection header field name.
     */
    public static final String CONN_DIRECTIVE = "Connection";

    /**
     * The HTTP Content-Length header field name.
     */
    public static final String CONTENT_LENGTH = "Content-Length";
    /**
     * The HTTP Content-Encoding header field name.
     */
    public static final String CONTENT_ENCODING = "Content-Encoding";
    /**
     * The HTTP Retry-After header field name.
     */
    public static final String RETRY_AFTER = "Retry-After";
    /**
     * Value for the Content-Encoding header that indicates that GZIP encoding is in use.
     */
    public static final String ENCODING_GZIP = "gzip";
    /**
     * Value for the Content-Encoding header that indicates that DEFLATE encoding is in use.
     */
    public static final String ENCODING_DEFLATE = "deflate";
    /**
     * UTF-8: eight-bit UCS Transformation Format.
     */
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * ISO-8859-1: ISO Latin Alphabet Number 1 (ISO-LATIN-1).
     */
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
    private static final int BUF_SIZE = 0x800; // 2K chars (4K bytes)

    /** HTTP connection control */
    public static final String CONN_CLOSE = "Close";
    public static final String CONN_KEEP_ALIVE = "Keep-Alive";

    /** Transfer encoding definitions */
    public static final String CHUNK_CODING = "chunked";
    public static final String IDENTITY_CODING = "identity";

    public static final Charset DEF_CONTENT_CHARSET = ISO_8859_1;
    public static final Charset DEF_PROTOCOL_CHARSET = StandardCharsets.US_ASCII;

    private Utils() { // no instances
    }

    /**
     * Copy of {@code com.google.common.base.Preconditions#checkArgument}.
     */
    public static void checkArgument(boolean expression,
                                     String errorMessageTemplate,
                                     Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(
                    format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Copy of {@code com.google.common.base.Preconditions#checkNotNull}.
     */
    public static <T> T checkNotNull(T reference,
                                     String errorMessageTemplate,
                                     Object... errorMessageArgs) {
        if (reference == null) {
            // If either of these parameters is null, the right thing happens anyway
            throw new NullPointerException(
                    format(errorMessageTemplate, errorMessageArgs));
        }
        return reference;
    }

    /**
     * Copy of {@code com.google.common.base.Preconditions#checkState}.
     */
    public static void checkState(boolean expression,
                                  String errorMessageTemplate,
                                  Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalStateException(
                    format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Identifies a method as a default instance method.
     */
    public static boolean isDefault(Method method) {
        // Default methods are public non-abstract, non-synthetic, and non-static instance methods
        // declared in an interface.
        // method.isDefault() is not sufficient for our usage as it does not check
        // for synthetic methods. As a result, it picks up overridden methods as well as actual default
        // methods.
        final int SYNTHETIC = 0x00001000;
        return ((method.getModifiers()
                & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC | SYNTHETIC)) == Modifier.PUBLIC)
                && method.getDeclaringClass().isInterface();
    }

    /**
     * Adapted from {@code com.google.common.base.Strings#emptyToNull}.
     */
    public static String emptyToNull(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

    /**
     * Removes values from the array that meet the criteria for removal via the supplied
     * {@link Predicate} value
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] removeValues(T[] values, Predicate<T> shouldRemove, Class<T> type) {
        Collection<T> collection = new ArrayList<>(values.length);
        for (T value : values) {
            if (shouldRemove.negate().test(value)) {
                collection.add(value);
            }
        }
        T[] array = (T[]) Array.newInstance(type, collection.size());
        return collection.toArray(array);
    }

    /**
     * Adapted from {@code com.google.common.base.Strings#emptyToNull}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> type) {
        Collection<T> collection;
        if (iterable instanceof Collection) {
            collection = (Collection<T>) iterable;
        } else {
            collection = new ArrayList<>();
            for (T element : iterable) {
                collection.add(element);
            }
        }
        T[] array = (T[]) Array.newInstance(type, collection.size());
        return collection.toArray(array);
    }

    /**
     * Returns an unmodifiable collection which may be empty, but is never null.
     */
    public static <T> List<T> valuesOrEmpty(Map<String, List<T>> map, String key) {
        List<T> values = map.get(key);
        return values != null ? values : Collections.emptyList();
    }

    public static void ensureClosed(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) { // NOPMD
            }
        }
    }

    private static final Map<Class<?>, Supplier<Object>> EMPTIES;
    static {
        final Map<Class<?>, Supplier<Object>> empties = new LinkedHashMap<Class<?>, Supplier<Object>>();
        empties.put(boolean.class, () -> false);
        empties.put(Boolean.class, () -> false);
        empties.put(byte[].class, () -> new byte[0]);
        empties.put(Collection.class, Collections::emptyList);
        empties.put(Iterator.class, Collections::emptyIterator);
        empties.put(List.class, Collections::emptyList);
        empties.put(Map.class, Collections::emptyMap);
        empties.put(Set.class, Collections::emptySet);
        empties.put(Optional.class, Optional::empty);
        empties.put(Stream.class, Stream::empty);
        EMPTIES = Collections.unmodifiableMap(empties);
    }

    /**
     * Adapted from {@code com.google.common.io.CharStreams.toString()}.
     */
    public static String toString(Reader reader) throws IOException {
        if (reader == null) {
            return null;
        }
        try {
            StringBuilder to = new StringBuilder();
            CharBuffer charBuf = CharBuffer.allocate(BUF_SIZE);
            // must cast to super class Buffer otherwise break when running with java 11
            Buffer buf = charBuf;
            while (reader.read(charBuf) != -1) {
                buf.flip();
                to.append(charBuf);
                buf.clear();
            }
            return to.toString();
        } finally {
            ensureClosed(reader);
        }
    }

    /**
     * Adapted from {@code com.google.common.io.ByteStreams.toByteArray()}.
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        checkNotNull(in, "in");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out);
            return out.toByteArray();
        } finally {
            ensureClosed(in);
        }
    }

    /**
     * Adapted from {@code com.google.common.io.ByteStreams.copy()}.
     */
    private static long copy(InputStream from, OutputStream to)
            throws IOException {
        checkNotNull(from, "from");
        checkNotNull(to, "to");
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static String decodeOrDefault(byte[] data, Charset charset, String defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        checkNotNull(charset, "charset");
        try {
            return charset.newDecoder().decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException ex) {
            return defaultValue;
        }
    }

    /**
     * If the provided String is not null or empty.
     *
     * @param value to evaluate.
     * @return true of the value is not null and not empty.
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * If the provided String is null or empty.
     *
     * @param value to evaluate.
     * @return true if the value is null or empty.
     */
    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * If the string is empty, return the specified default value
     *
     * @param value value.
     * @param defaultValue defaultValue.
     * @return value or defaultValue
     */
    public static String defaultIfEmpty(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * Generate Boundary (use by content-type)
     *
     * @return value of boundary
     */
    public static  String generateBoundary() {
        final ThreadLocalRandom rand = ThreadLocalRandom.current();
        final StringBuilder buffer = new StringBuilder();
        final int count = rand.nextInt(11) + 30; // a random size from 30 to 40
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

}