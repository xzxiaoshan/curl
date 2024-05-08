package org.toilelibre.libe.curl;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * 请求响应结果后置处理
 *
 * @author shanhy
 * @date 2023-07-31 17:51
 */
@Slf4j
public class OutputHandler {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = Logger.getLogger(OutputHandler.class.getName());

    /**
     * handle
     *
     * @param inputStream    inputStream
     * @param outputFilePath outputFilePath
     */
    public Path handle(final InputStream inputStream, final String outputFilePath) {
        if (outputFilePath == null || outputFilePath.trim().isEmpty()) {
            return null;
        }
        // 处理是否将结果写入到文件
        File file = createTheOutputFile(outputFilePath);
        FileOutputStream outputStream = getOutputStreamFromFile(file);

        writeTheResponseEntityInsideStream(outputStream, inputStream);
        return file.toPath();
    }

    /**
     * writeTheResponseEntityInsideStream
     *
     * @param outputStream outputStream
     * @param inputStream  inputStream
     */
    private void writeTheResponseEntityInsideStream(FileOutputStream outputStream, InputStream inputStream) {
        try (OutputStream os = outputStream) {
            byte[] buffer = new byte[1024]; // 缓冲区
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new CurlException(e);
        }
    }

    /**
     * getOutputStreamFromFile
     *
     * @param file file
     * @return FileOutputStream
     */
    private FileOutputStream getOutputStreamFromFile(File file) {
        try {
            return new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new CurlException(e);
        }
    }

    /**
     * createTheOutputFile
     *
     * @param fileName fileName
     * @return File
     */
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
