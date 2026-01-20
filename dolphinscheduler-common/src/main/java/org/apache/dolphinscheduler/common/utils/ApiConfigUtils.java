package org.apache.dolphinscheduler.common.utils;

public class ApiConfigUtils {

    /**
     * 获取完整的 API 服务地址
     */
    public static String getServerUrl() {
        String host = getApiServerHost();
        String port = getApiServerPort();
        String prefix = getApiPrefix();
        String url = getUrl();
        return prefix + host + ":" + port + url;
    }

    public static String getUrl() {
        return PropertyUtils.getString("server.url");
    }

    /**
     * 获取 API 服务主机地址前缀
     */
    public static String getApiPrefix() {
        return PropertyUtils.getString("server.prefix");
    }

    /**
     * 获取 API 服务主机地址
     */
    public static String getApiServerHost() {
        return PropertyUtils.getString("server.address");
    }

    /**
     * 获取 API 服务端口
     */
    public static String getApiServerPort() {
        return PropertyUtils.getString("server.port");
    }

}
