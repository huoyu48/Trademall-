package com.orderflow.platform;

import lombok.Data;

@Data
public class TenantStatDTO {
    private Long id;
    private String tenantCode;
    private String tenantName;
    private Integer status;
    private Long orderCount;
    private Long gmvCent;
}
