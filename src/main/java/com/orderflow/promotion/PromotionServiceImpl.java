package com.orderflow.promotion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Promotion;
import com.orderflow.domain.mapper.PromotionMapper;
import com.orderflow.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionMapper mapper;

    public PromotionServiceImpl(PromotionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Promotion create(CreatePromotionRequest req) {
        Long tenantId = TenantContext.getTenantId();
        Promotion p = new Promotion();
        p.setTenantId(tenantId);
        p.setPromoCode(req.getPromoCode());
        p.setPromoName(req.getPromoName());
        p.setPromoType(req.getPromoType());
        p.setThresholdCent(req.getThresholdCent() == null ? 0 : req.getThresholdCent());
        p.setDiscountAmountCent(req.getDiscountAmountCent() == null ? 0 : req.getDiscountAmountCent());
        p.setBeginAt(req.getBeginAt());
        p.setEndAt(req.getEndAt());
        p.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        mapper.insert(p);
        return p;
    }

    @Override
    public Promotion update(Long id, UpdatePromotionRequest req) {
        Long tenantId = TenantContext.getTenantId();
        Promotion p = mapper.selectById(id);
        if (p == null || !tenantId.equals(p.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        if (req.getPromoName() != null) p.setPromoName(req.getPromoName());
        if (req.getPromoType() != null) p.setPromoType(req.getPromoType());
        if (req.getThresholdCent() != null) p.setThresholdCent(req.getThresholdCent());
        if (req.getDiscountAmountCent() != null) p.setDiscountAmountCent(req.getDiscountAmountCent());
        if (req.getBeginAt() != null) p.setBeginAt(req.getBeginAt());
        if (req.getEndAt() != null) p.setEndAt(req.getEndAt());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        mapper.updateById(p);
        return p;
    }

    @Override
    public Promotion detail(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Promotion p = mapper.selectById(id);
        if (p == null || !tenantId.equals(p.getTenantId())) {
            throw new BizException(BizErrorCode.NOT_FOUND);
        }
        return p;
    }

    @Override
    public PageResult<Promotion> page(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        Page<Promotion> pg = mapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Promotion>().eq("tenant_id", tenantId).orderByDesc("id"));
        return PageResult.of(pg.getRecords(), pg.getTotal(), page, size);
    }

    @Override
    public List<Promotion> listAll() {
        Long tenantId = TenantContext.getTenantId();
        return mapper.selectList(new QueryWrapper<Promotion>().eq("tenant_id", tenantId).orderByDesc("id"));
    }
}
