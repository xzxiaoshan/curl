package org.toilelibre.libe.curl.http;

import lombok.Data;
import org.toilelibre.libe.curl.Utils;

/**
 * 代理信息
 *
 * @author shanhy
 * @date 2023-07-31 20:12
 */
@Data
public class ProxyInfo {

    private String schema;
    private String host;
    private int port = -1;
    private String username;
    private String password;

    public ProxyInfo(String schema, String hostString, String userString) {
        this.schema = schema;
        if (userString != null) {
            this.username = userString.substring(0, userString.indexOf(':'));
            this.password = userString.substring(userString.indexOf(':') + 1);
        }
        this.host = hostString.substring(0, hostString.indexOf(':'));
        String portStr = hostString.substring(hostString.indexOf(':') + 1);
        if (!portStr.isEmpty())
            this.port = Integer.parseInt(portStr);
    }

    public String getUserString() {
        return Utils.isBlank(this.username) ? "" :
                this.username.concat(":").concat(Utils.defaultIfEmpty(password, ""));
    }

    public String getHostString() {
        return this.host.concat(":").concat(String.valueOf(port));
    }

    /**
     * 重写toString必须包含所有字段信息，其他地方有使用，用于重复判断
     *
     * @return str
     */
    @Override
    public String toString() {
        String userStr = getUserString();
        userStr = Utils.isNotBlank(userStr) ? userStr.concat("@") : "";
        return schema.concat("://").concat(userStr).concat(getHostString());
    }
}
