package com.platform.cloud.mall.system.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.mall.system.dto.AdminSaveRequest;
import com.platform.cloud.mall.system.dto.ChangePasswordRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志参数：密码脱敏、截断、文件与 Servlet 对象过滤。
 */
class OperLogParamsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void masksPasswordFieldsAtAnyDepth() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old-secret");
        req.setNewPassword("new-secret");
        AdminSaveRequest admin = new AdminSaveRequest();
        admin.setUsername("bob");
        admin.setPassword("p@ss");

        String json = OperLogParams.serialize(mapper, new Object[]{1L, req, Map.of("nested", List.of(admin))});

        assertThat(json).doesNotContain("old-secret", "new-secret", "p@ss");
        assertThat(json).contains("\"oldPassword\":\"******\"", "\"password\":\"******\"", "\"username\":\"bob\"");
    }

    @Test
    void truncatesTo2000Chars() {
        String json = OperLogParams.serialize(mapper, new Object[]{"x".repeat(5000)});
        assertThat(json).hasSize(OperLogParams.MAX_LENGTH);
    }

    @Test
    void fileContentIsNotLogged() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "BINARY".getBytes());
        String json = OperLogParams.serialize(mapper, new Object[]{file, "goods"});
        assertThat(json).contains("a.png").doesNotContain("BINARY");
    }

    @Test
    void emptyArgsGiveEmptyString() {
        assertThat(OperLogParams.serialize(mapper, new Object[0])).isEmpty();
    }

    @Test
    void onlyAdminWriteRequestsAreLogged() {
        assertThat(OperLogAspect.shouldLog("POST", "/admin/goods/goods")).isTrue();
        assertThat(OperLogAspect.shouldLog("delete", "/admin/system/role/1")).isTrue();
        assertThat(OperLogAspect.shouldLog("GET", "/admin/goods/goods/page")).isFalse();
        assertThat(OperLogAspect.shouldLog("POST", "/admin/auth/login")).isFalse();
        assertThat(OperLogAspect.shouldLog("POST", "/app/order")).isFalse();
    }
}
