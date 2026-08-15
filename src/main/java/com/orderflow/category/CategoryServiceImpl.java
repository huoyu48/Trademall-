package com.orderflow.category;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Category;
import com.orderflow.domain.mapper.CategoryMapper;
import com.orderflow.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper mapper;

    public CategoryServiceImpl(CategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Category create(CreateCategoryRequest req) {
        Long tenantId = TenantContext.getTenantId();
        Category c = new Category();
        c.setTenantId(tenantId);
        c.setCategoryCode(req.getCategoryCode());
        c.setCategoryName(req.getCategoryName());
        c.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        c.setSort(req.getSort() == null ? 0 : req.getSort());
        c.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        mapper.insert(c);
        return c;
    }

    @Override
    public Category update(Long id, UpdateCategoryRequest req) {
        Long tenantId = TenantContext.getTenantId();
        Category c = mapper.selectById(id);
        if (c == null || !tenantId.equals(c.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        if (req.getCategoryName() != null) c.setCategoryName(req.getCategoryName());
        if (req.getParentId() != null) c.setParentId(req.getParentId());
        if (req.getSort() != null) c.setSort(req.getSort());
        if (req.getStatus() != null) c.setStatus(req.getStatus());
        mapper.updateById(c);
        return c;
    }

    @Override
    public Category detail(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Category c = mapper.selectById(id);
        if (c == null || !tenantId.equals(c.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        return c;
    }

    @Override
    public PageResult<Category> page(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        Page<Category> p = mapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Category>().eq("tenant_id", tenantId).orderByAsc("sort"));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Override
    public List<Category> listAll() {
        Long tenantId = TenantContext.getTenantId();
        return mapper.selectList(new QueryWrapper<Category>().eq("tenant_id", tenantId).orderByAsc("sort"));
    }
}
