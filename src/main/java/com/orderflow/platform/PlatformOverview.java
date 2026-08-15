package com.orderflow.platform;

import lombok.Data;

@Data
public class PlatformOverview {
    private Long tenants;
    private Long orders;
    private Long gmvCent;
}
