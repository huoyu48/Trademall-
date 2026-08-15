package com.orderflow.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.orderflow.common.BizException;
import com.orderflow.common.BizErrorCode;
import com.orderflow.domain.entity.Inventory;
import com.orderflow.domain.entity.InventoryAdjustment;
import com.orderflow.domain.entity.Product;
import com.orderflow.domain.mapper.InventoryAdjustmentMapper;
import com.orderflow.domain.mapper.InventoryMapper;
import com.orderflow.domain.mapper.ProductMapper;
import com.orderflow.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryAdjustmentMapper adjustmentMapper;
    private final ProductMapper productMapper;

    @Value("${orderflow.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    public InventoryServiceImpl(InventoryMapper inventoryMapper,
                               InventoryAdjustmentMapper adjustmentMapper,
                               ProductMapper productMapper) {
        this.inventoryMapper = inventoryMapper;
        this.adjustmentMapper = adjustmentMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public void adjust(AdjustInventoryRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Inventory inventory = inventoryMapper.selectByProduct(tenantId, request.getProductId());
        if (inventory == null) {
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setTenantId(tenantId);
        adjustment.setProductId(request.getProductId());
        adjustment.setChangeQuantity(request.getChangeQuantity());
        adjustment.setReason(request.getReason());
        adjustment.setOperatorId(TenantContext.getUserId());
        adjustmentMapper.insert(adjustment);

        int rows = inventoryMapper.update(null, new UpdateWrapper<Inventory>()
                .eq("tenant_id", tenantId)
                .eq("product_id", request.getProductId())
                .setSql("physical_quantity = physical_quantity + " + request.getChangeQuantity()));
        if (rows == 0) {
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Override
    public List<InventoryDTO> list() {
        Long tenantId = TenantContext.getTenantId();
        List<Inventory> inventories = inventoryMapper.selectList(
                new QueryWrapper<Inventory>().eq("tenant_id", tenantId));
        List<InventoryDTO> result = new ArrayList<>();
        for (Inventory inv : inventories) {
            Product product = productMapper.selectById(inv.getProductId());
            InventoryDTO dto = new InventoryDTO();
            dto.setProductId(inv.getProductId());
            dto.setProductName(product == null ? null : product.getProductName());
            dto.setPhysicalQuantity(inv.getPhysicalQuantity());
            dto.setReservedQuantity(inv.getReservedQuantity());
            dto.setAvailableQuantity(inv.getPhysicalQuantity() - inv.getReservedQuantity());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<InventoryDTO> lowStock() {
        return list().stream()
                .filter(i -> i.getAvailableQuantity() <= lowStockThreshold)
                .toList();
    }
}
