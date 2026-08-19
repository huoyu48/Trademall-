-- 店铺名称是顾客侧识别商家的展示名称，要求全平台唯一。
-- 应用层会先给出友好提示；该唯一索引用于兜底并发创建/修改的竞争条件。
ALTER TABLE store
    ADD CONSTRAINT uk_store_name UNIQUE (store_name);
