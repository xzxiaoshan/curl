package org.toilelibre.libe.curl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.toilelibre.libe.curl.http.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * 请求响应结果后置处理
 *
 * @author shanhy
 * @date 2023-07-31 17:51
 */
@Slf4j
public class AfterResponse {

    private static final Logger LOGGER = Logger.getLogger(AfterResponse.class.getName());

    public void handle(final CommandLine commandLine, final Response response) {
        this.handleOutput(commandLine, response.body());
    }

    private void handleOutput(final CommandLine commandLine, final Response.Body body){
        // 处理是否将结果写入到文件
        if (!commandLine.hasOption(Arguments.OUTPUT.getOpt())) return;
        File file = createTheOutputFile(commandLine.getOptionValue(Arguments.OUTPUT.getOpt()));
        FileOutputStream outputStream = getOutputStreamFromFile(file);
        writeTheResponseEntityInsideStream(outputStream, body);
    }

    private void writeTheResponseEntityInsideStream(FileOutputStream outputStream, Response.Body body) {
        try {
            if (body.length() >= 0) {
                outputStream.write(IOUtils.toByteArray(body.asInputStream(), (int)body.length()));
            } else {
                outputStream.write(IOUtils.toByteArray(body.asInputStream()));
            }
        } catch (final IOException e) {
            throw new CurlException(e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                LOGGER.warning("Cannot flush the file in output");
            }
        }
    }

    private FileOutputStream getOutputStreamFromFile(File file) {
        try {
            return new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new CurlException(e);
        }
    }

    private File createTheOutputFile(String fileName) {
        final File file = new File(fileName);
        try {
            if (!file.createNewFile()) {
                throw new CurlException(new IOException("Could not create the file. Does it already exist ?"));
            }
        } catch (IOException e) {
            LOGGER.warning("Cannot flush the output file");
            throw new CurlException(e);
        }
        return file;
    }

}
