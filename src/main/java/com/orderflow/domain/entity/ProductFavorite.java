package com.orderflow.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/* 收藏实体：一个 Java 对象对应 product_favorite 表中的一行。
Entity 的作用是让 Java 字段和数据库列建立映射关系。
Mapper 查出一行数据后，Mybatis-Plus 会把每一列填入对应字段。
*/
@Data//Lombok自动生成 getter、setter、toSpring 等方法
@TableName("product_favorite")//明确指定这个类对应的 MySQL 表名


public class ProductFavorite {
    /* 收藏记录主键，对应表中的 id 列。
    IdType.AUTO 表示插入时不由 Java 生成 Id，而是使用 MySQL AUTO_INCREMENT
    */
   @TableId(type=IdType.AUTO)
   private Long id;
   /* 顾客账号所属租户，对应 tenant_id.
   后续查询删除都会被带上来防止其他租户数据
   */
   private Long tenantId;
   /* 当前登录顾客 ID，对应 customer_id。
   这个值必须来自 JWT 登录上下文，不能相信前端传入的用户 ID。
   */
   private Long customerId;
   /* 被收藏的商品 ID，对应 product_id.
   它与 product.id 简历逻辑关联，用于查询商品名称、价格和店铺。
   */
   private Long productId;
   /* 顾客首次收藏的时间，对应 created_at
   FieldFill.INSERT表示只在 INSERT 时填充，后续查询不会改变它。
   */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
