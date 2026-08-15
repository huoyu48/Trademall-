package com.orderflow.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlatformMapper extends BaseMapper<Tenant> {

    @Select("SELECT t.id, t.tenant_code tenantCode, t.tenant_name tenantName, t.status, " +
            "COUNT(o.id) orderCount, COALESCE(SUM(o.total_amount_cent),0) gmvCent " +
            "FROM tenant t LEFT JOIN orders o ON o.tenant_id = t.id " +
            "GROUP BY t.id, t.tenant_code, t.tenant_name, t.status ORDER BY t.id")
    List<TenantStatDTO> tenantStats();

    @Select("SELECT (SELECT COUNT(*) FROM tenant) tenants, " +
            "(SELECT COUNT(*) FROM orders) orders, " +
            "COALESCE((SELECT SUM(total_amount_cent) FROM orders),0) gmvCent")
    PlatformOverview overview();
}
