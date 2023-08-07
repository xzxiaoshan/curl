package org.toilelibre.libe.curl;

import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.http.DataBody;
import org.toilelibre.libe.curl.http.FormBody;
import org.toilelibre.libe.curl.http.FormBodyPart;
import org.toilelibre.libe.curl.http.RequestBody;
import org.toilelibre.libe.curl.http.RequestBodyType;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.toilelibre.libe.curl.IOUtils.dataBehind;

/**
 * 请求体读取器
 *
 * @author shanhy
 * @date 2023-07-31 17:51
 */
public final class PayloadReader {
    private static final Pattern CONTENT_TYPE_ENCODING =
            Pattern.compile("\\s*content-type\\s*:[^;]+;\\s*charset\\s*=\\s*(.*)", Pattern.CASE_INSENSITIVE);

    public static RequestBody<?> getData(final CommandLine commandLine) {
        RequestBody<?> bodyData = null;
        if (commandLine.hasOption(Arguments.DATA.getOpt())) {
            bodyData = simpleData(commandLine);
        } else if (commandLine.hasOption(Arguments.DATA_BINARY.getOpt())) {
            bodyData = binaryData(commandLine);
        } else if (commandLine.hasOption(Arguments.DATA_URLENCODED.getOpt())) {
            bodyData = urlEncodedData(commandLine);
        } else if (commandLine.hasOption(Arguments.FORM.getOpt())) {
            bodyData = formData(commandLine);
        }
        return bodyData;
    }

    private static RequestBody<byte[]> simpleData(CommandLine commandLine) {
        try {
            Charset encoding = charsetReadFromThe(commandLine).orElse(Utils.UTF_8);
            return new DataBody(commandLine.getOptionValue(Arguments.DATA.getOpt()).getBytes(encoding), RequestBodyType.DATA, encoding);
        } catch (final IllegalArgumentException e) {
            throw new CurlException(e);
        }
    }

    private static RequestBody<byte[]> binaryData(CommandLine commandLine) {
        final String value = commandLine.getOptionValue(Arguments.DATA_BINARY.getOpt());
        byte[] data;
        if (value.indexOf('@') == 0) {
            data = dataBehind(value);
        } else {
            data = value.getBytes();
        }
        return new DataBody(data, RequestBodyType.DATA_BINARY);
    }

    private static RequestBody<List<FormBodyPart>> formData(final CommandLine commandLine) {
        final String[] forms = Optional.ofNullable(commandLine.getOptionValues(Arguments.FORM.getOpt())).orElse(new String[0]);

        if (forms.length == 0) {
            return null;
        }

        stream(forms).forEach(arg -> {
            if (arg.indexOf('=') == -1) {
                throw new IllegalArgumentException("option -F: is badly used here");
            }
        });

        final List<String> fileForms = stream(forms).filter(arg -> IOUtils.isFile(arg.substring(arg.indexOf('=') + 1))).collect(toList());
        final List<String> textForms = stream(forms).filter(form -> !fileForms.contains(form)).collect(toList());

        FormBody formBody = new FormBody();
        fileForms.forEach(arg -> formBody.addFilePart(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf("=@") + 2)));
        textForms.forEach(arg -> formBody.addTextPart(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf('=') + 1)));

        return formBody;

    }


    private static Optional<Charset> charsetReadFromThe(CommandLine commandLine) {
        return stream(Optional.ofNullable(commandLine.getOptionValues(Arguments.HEADER.getOpt())).orElse(new String[0]))
                .filter(header -> header != null && CONTENT_TYPE_ENCODING.asPredicate().test(header))
                .findFirst().map(correctHeader -> {
                    final Matcher matcher = CONTENT_TYPE_ENCODING.matcher(correctHeader);
                    if (!matcher.find()) return null;
                    return Charset.forName(matcher.group(1));
                });
    }

    private static RequestBody<byte[]> urlEncodedData(CommandLine commandLine) {
        String value = stream(commandLine.getOptionValues(Arguments.DATA_URLENCODED.getOpt()))
                .map(val -> {
                    if (val.startsWith("=")) {
                        val = val.substring(1);
                    }
                    if (val.indexOf('=') != -1) {
                        return val.substring(0, val.indexOf('=') + 1) + encodeOrFail(val.substring(val.indexOf('=') + 1), Charset.defaultCharset());
                    }
                    if (val.indexOf('@') == 0) {
                        return encodeOrFail(new String(dataBehind(val)), Charset.defaultCharset());
                    }
                    if (val.indexOf('@') != -1) {
                        return val.substring(0, val.indexOf('@')) + '=' + encodeOrFail(new String(dataBehind(val.substring(val.indexOf('@')))), Charset.defaultCharset());
                    }
                    return encodeOrFail(val, Charset.defaultCharset());
                }).collect(Collectors.joining("&"));
        Charset charset = Charset.defaultCharset();
        return new DataBody(value.getBytes(charset), RequestBodyType.DATA_BINARY, charset);
    }

    private static String encodeOrFail(String value, Charset encoding) {
        try {
            return encode(value, encoding.name());
        } catch (UnsupportedEncodingException e) {
            throw new CurlException(e);
        }
    }
}
