package com.orderflow.platform;

import com.orderflow.common.ApiResponse;
import com.orderflow.domain.entity.Tenant;
import com.orderflow.domain.mapper.TenantMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/platform")
public class PlatformController {

    private final PlatformMapper platformMapper;
    private final TenantMapper tenantMapper;
    private final PlatformTenantService tenantService;

    public PlatformController(PlatformMapper platformMapper, TenantMapper tenantMapper,
                              PlatformTenantService tenantService) {
        this.platformMapper = platformMapper;
        this.tenantMapper = tenantMapper;
        this.tenantService = tenantService;
    }

    /** 全平台各租户经营概览（订单数 + GMV） */
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<TenantStatDTO>> tenants() {
        return ApiResponse.success(platformMapper.tenantStats());
    }

    /** 新增租户（让新商家进驻商城），自动创建其商家管理员账号 */
    @PostMapping("/tenants")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Tenant> createTenant(@Valid @RequestBody CreateTenantRequest req) {
        return ApiResponse.success(tenantService.createTenant(req));
    }

    /** 平台汇总指标 */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PlatformOverview> stats() {
        return ApiResponse.success(platformMapper.overview());
    }

    /** 启停租户（平台治理动作） */
    @PostMapping("/tenants/{id}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam int status) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) {
            return ApiResponse.fail(40400, "租户不存在");
        }
        t.setStatus(status);
        tenantMapper.updateById(t);
        return ApiResponse.success();
    }
}
