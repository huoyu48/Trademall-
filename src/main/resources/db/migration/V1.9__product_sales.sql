-- V1.9 商品销量字段：商城展示“已售 xx”，驱动商品卡片的销量/热度标签
ALTER TABLE product
    ADD COLUMN sales BIGINT NOT NULL DEFAULT 0 COMMENT '累计销量' AFTER status;
