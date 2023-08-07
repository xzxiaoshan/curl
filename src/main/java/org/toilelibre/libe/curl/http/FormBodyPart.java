package org.toilelibre.libe.curl.http;

import lombok.Data;

/**
 * 表单项
 * 1.值类型value为项值
 * 2.文件类型value为文件路径
 *
 * @author shanhy
 * @date 2023-07-31 17:52
 */
@Data
public class FormBodyPart {

    private String name;
    private String value;
    private boolean isFile;

    public FormBodyPart(String name, String value) {
        this(name, value, false);
    }

    public FormBodyPart(String name, String value, boolean isFile) {
        this.name = name;
        this.value = value;
        this.isFile = isFile;
    }
}
