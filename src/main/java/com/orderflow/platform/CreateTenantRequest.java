package com.orderflow.platform;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台管理员新增租户请求。默认会为租户创建管理员账号 admin-{tenantCode}（密码 admin123）。
 */
@Data
public class CreateTenantRequest {
    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    /** 可选：管理员账号，默认 admin-{tenantCode} */
    private String adminUsername;

    /** 可选：管理员初始密码，默认 admin123 */
    private String adminPassword;
}
