package com.orderflow.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 顾客商城公开注册请求。 */
@Data
public class CustomerRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需为 3 到 32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅支持字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为 6 到 64 位")
    private String password;

    @NotBlank(message = "请再次确认密码")
    private String confirmPassword;

    @Size(max = 64, message = "昵称不能超过 64 位")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的中国大陆手机号")
    private String phone;
}
