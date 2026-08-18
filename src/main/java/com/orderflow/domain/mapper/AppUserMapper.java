package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    @Select("SELECT * FROM app_user WHERE username = #{username} LIMIT 1")
    AppUser findByUsername(@Param("username") String username);

    /** 将实时消息推给当前商家租户的所有启用管理员会话。 */
    @Select("SELECT DISTINCT u.id FROM app_user u "
            + "JOIN user_role ur ON ur.user_id = u.id "
            + "JOIN role r ON r.id = ur.role_id "
            + "WHERE u.tenant_id = #{tenantId} AND u.status = 1 AND r.role_code = 'MERCHANT_ADMIN'")
    List<Long> findMerchantAdminIdsByTenant(@Param("tenantId") Long tenantId);
}
