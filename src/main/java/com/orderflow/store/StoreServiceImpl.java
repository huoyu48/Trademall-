package com.orderflow.store;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Store;
import com.orderflow.domain.mapper.StoreMapper;
import com.orderflow.security.TenantContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreMapper mapper;

    public StoreServiceImpl(StoreMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Store create(CreateStoreRequest req) {
        Long tenantId = TenantContext.getTenantId();
        String storeName = normalizeStoreName(req.getStoreName());
        ensureStoreNameAvailable(storeName, null);
        Store s = new Store();
        s.setTenantId(tenantId);
        s.setStoreCode(req.getStoreCode());
        s.setStoreName(storeName);
        s.setProvince(req.getProvince());
        s.setCity(req.getCity());
        s.setAddress(req.getAddress());
        s.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        try {
            mapper.insert(s);
        } catch (DuplicateKeyException ex) {
            throw new BizException(BizErrorCode.STORE_NAME_DUPLICATED);
        }
        return s;
    }

    @Override
    public Store update(Long id, UpdateStoreRequest req) {
        Long tenantId = TenantContext.getTenantId();
        Store s = mapper.selectById(id);
        if (s == null || !tenantId.equals(s.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        if (req.getStoreName() != null) {
            String storeName = normalizeStoreName(req.getStoreName());
            ensureStoreNameAvailable(storeName, id);
            s.setStoreName(storeName);
        }
        if (req.getProvince() != null) s.setProvince(req.getProvince());
        if (req.getCity() != null) s.setCity(req.getCity());
        if (req.getAddress() != null) s.setAddress(req.getAddress());
        if (req.getStatus() != null) s.setStatus(req.getStatus());
        try {
            mapper.updateById(s);
        } catch (DuplicateKeyException ex) {
            throw new BizException(BizErrorCode.STORE_NAME_DUPLICATED);
        }
        return s;
    }

    @Override
    public Store detail(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Store s = mapper.selectById(id);
        if (s == null || !tenantId.equals(s.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        return s;
    }

    @Override
    public PageResult<Store> page(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        Page<Store> p = mapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Store>().eq("tenant_id", tenantId).orderByAsc("id"));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Override
    public List<Store> listAll() {
        Long tenantId = TenantContext.getTenantId();
        return mapper.selectList(new QueryWrapper<Store>().eq("tenant_id", tenantId).orderByAsc("id"));
    }

    /** 店铺是面对顾客展示的全局名称，因此不能只在当前商家内查重。 */
    private void ensureStoreNameAvailable(String storeName, Long excludedStoreId) {
        boolean wasIgnoringTenant = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            QueryWrapper<Store> query = new QueryWrapper<Store>().eq("store_name", storeName);
            if (excludedStoreId != null) {
                query.ne("id", excludedStoreId);
            }
            if (mapper.selectCount(query) > 0) {
                throw new BizException(BizErrorCode.STORE_NAME_DUPLICATED);
            }
        } finally {
            TenantContext.setIgnoreTenant(wasIgnoringTenant);
        }
    }

    private String normalizeStoreName(String storeName) {
        if (storeName == null || storeName.isBlank()) {
            throw new BizException(40001, "店铺名称不能为空");
        }
        return storeName.trim();
    }
}
