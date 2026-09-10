package com.example.labkey.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * JSON 工具：统一 ObjectMapper 配置与响应写出逻辑。
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 以 JSON 写出响应，统一设置 Content-Type / 字符编码 / 状态码 / CORS 头。
     */
    public static void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.reset();
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // 允许跨域调试（同源部署时无副作用）
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Cache-Control", "no-store");
        MAPPER.writeValue(resp.getWriter(), body);
    }

    /**
     * 标准错误体：{"code":400,"message":"...","errors":{...}}
     */
    public static Map<String, Object> errorBody(int code, String message, Map<String, String> errors) {
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors);
        }
        return body;
    }
}
