package org.toilelibre.libe.curl.http;


import java.util.ArrayList;
import java.util.List;

/**
 * 表单数据实体
 *
 * @author shanhy
 * @date 2023-07-31 17:51
 */
public class FormBody implements RequestBody<List<FormBodyPart>> {

    private final List<FormBodyPart> partList = new ArrayList<>();

    public void addTextPart(String name, String value) {
        partList.add(new FormBodyPart(name, value));
    }

    public void addFilePart(String name, String filePath) {
        partList.add(new FormBodyPart(name, filePath, true));
    }

    @Override
    public RequestBodyType getBodyType() {
        return RequestBodyType.FORM;
    }

    @Override
    public List<FormBodyPart> getBody() {
        return partList;
    }

    public String asString() {
        StringBuilder builder = new StringBuilder();
        for (final FormBodyPart part : partList) {
            builder.append(part.getName()).append(": ").append(part.getValue()).append('\n');
        }
        return builder.toString();
    }
}
