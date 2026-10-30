package com.platform.cloud.mall.system.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 把接口入参序列化成操作日志里的 params：密码类字段替换为 ******，超长截断。
 */
public final class OperLogParams {

    /** 与 sys_oper_log.params 列宽一致 */
    public static final int MAX_LENGTH = 2000;

    public static final String MASK = "******";

    private OperLogParams() {
    }

    public static String serialize(ObjectMapper mapper, Object[] args) {
        List<Object> loggable = new ArrayList<>();
        for (Object arg : args == null ? new Object[0] : args) {
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof BindingResult) {
                continue;
            }
            // 文件内容不入日志，只记录文件名和大小，便于追溯是谁上传了什么
            if (arg instanceof MultipartFile f) {
                loggable.add(Map.of("file", String.valueOf(f.getOriginalFilename()), "size", f.getSize()));
                continue;
            }
            loggable.add(arg);
        }
        if (loggable.isEmpty()) {
            return "";
        }
        String json;
        try {
            JsonNode tree = mapper.valueToTree(loggable.size() == 1 ? loggable.get(0) : loggable);
            mask(tree);
            json = mapper.writeValueAsString(tree);
        } catch (Exception e) {
            json = "[参数无法序列化: " + e.getClass().getSimpleName() + "]";
        }
        return json.length() <= MAX_LENGTH ? json : json.substring(0, MAX_LENGTH);
    }

    /** 递归脱敏：字段名含 password（不区分大小写，覆盖 oldPassword / newPassword）一律替换 */
    static void mask(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            List<String> sensitive = new ArrayList<>();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getKey().toLowerCase(Locale.ROOT).contains("password")) {
                    sensitive.add(e.getKey());
                } else {
                    mask(e.getValue());
                }
            }
            sensitive.forEach(k -> obj.set(k, TextNode.valueOf(MASK)));
        } else if (node instanceof ArrayNode arr) {
            arr.forEach(OperLogParams::mask);
        }
    }
}
