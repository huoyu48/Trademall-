package com.orderflow.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.BizException;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Category;
import com.orderflow.domain.entity.Inventory;
import com.orderflow.domain.entity.Product;
import com.orderflow.domain.entity.Store;
import com.orderflow.domain.mapper.CategoryMapper;
import com.orderflow.domain.mapper.InventoryMapper;
import com.orderflow.domain.mapper.ProductMapper;
import com.orderflow.domain.mapper.StoreMapper;
import com.orderflow.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final CategoryMapper categoryMapper;
    private final StoreMapper storeMapper;
    private final ProductCacheService cacheService;

    public ProductServiceImpl(ProductMapper productMapper, InventoryMapper inventoryMapper,
                              CategoryMapper categoryMapper, StoreMapper storeMapper,
                              ProductCacheService cacheService) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.categoryMapper = categoryMapper;
        this.storeMapper = storeMapper;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional
    public ProductDTO create(CreateProductRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Product dup = productMapper.findByCode(tenantId, request.getProductCode());
        if (dup != null) {
            throw new BizException(BizErrorCode.PRODUCT_CODE_DUPLICATED);
        }
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setUnitPriceCent(request.getUnitPriceCent());
        product.setStatus(request.getStatus());
        productMapper.insert(product);

        Inventory inventory = new Inventory();
        inventory.setTenantId(tenantId);
        inventory.setProductId(product.getId());
        inventory.setPhysicalQuantity(0L);
        inventory.setReservedQuantity(0L);
        inventory.setVersion(0L);
        inventoryMapper.insert(inventory);

        return toDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO update(Long productId, UpdateProductRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Product product = productMapper.selectById(productId);
        if (product == null || !tenantId.equals(product.getTenantId())) {
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
        product.setProductName(request.getProductName());
        product.setUnitPriceCent(request.getUnitPriceCent());
        product.setStatus(request.getStatus());
        productMapper.updateById(product);

        cacheService.evict(tenantId, productId);
        return toDTO(product);
    }

    @Override
    public ProductDTO detail(Long productId) {
        Long tenantId = TenantContext.getTenantId();
        ProductDTO cached = cacheService.get(tenantId, productId);
        if (cached != null) {
            return cached;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || !tenantId.equals(product.getTenantId())) {
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductDTO dto = toDTO(product);
        cacheService.put(tenantId, productId, dto);
        return dto;
    }

    @Override
    public PageResult<ProductDTO> page(int page, int size) {
        return page(page, size, null, null);
    }

    @Override
    public PageResult<ProductDTO> page(int page, int size, Long categoryId, String keyword) {
        Long tenantId = TenantContext.getTenantId();
        QueryWrapper<Product> qw = new QueryWrapper<Product>().eq("tenant_id", tenantId);
        if (categoryId != null) {
            qw.eq("category_id", categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("product_name", keyword).or().like("product_code", keyword));
        }
        qw.orderByDesc("sales").orderByDesc("id");
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(
                p.getRecords().stream().map(this::toDTO).toList(),
                p.getTotal(), page, size);
    }

    @Override
    public PageResult<ProductDTO> pageForMall(int page, int size, Long categoryId, String keyword) {
        // 商城端：跨租户查所有启用商品。临时开启 ignoreTenant 让 MyBatis-Plus 多租户插件放行，
        // 否则 TenantLineHandler 会自动注入 tenant_id=customer的t-a 导致只看到一家。
        TenantContext.setIgnoreTenant(true);
        try {
            QueryWrapper<Product> qw = new QueryWrapper<Product>().eq("status", 1);
            if (categoryId != null) {
                qw.eq("category_id", categoryId);
            }
            if (StringUtils.hasText(keyword)) {
                qw.and(w -> w.like("product_name", keyword).or().like("product_code", keyword));
            }
            qw.orderByDesc("sales").orderByDesc("id");
            Page<Product> p = productMapper.selectPage(new Page<>(page, size), qw);
            return PageResult.of(
                    p.getRecords().stream().map(this::toDTO).toList(),
                    p.getTotal(), page, size);
        } finally {
            TenantContext.setIgnoreTenant(false);
        }
    }

    @Override
    public ProductDTO detailForMall(Long productId) {
        TenantContext.setIgnoreTenant(true);
        try {
            Product product = productMapper.selectById(productId);
            if (product == null || product.getStatus() == 0) {
                throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
            }
            return toDTO(product);
        } finally {
            TenantContext.setIgnoreTenant(false);
        }
    }

    @Override
    public List<Product> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        TenantContext.setIgnoreTenant(true);
        try {
            return productMapper.selectBatchIds(ids);
        } finally {
            TenantContext.setIgnoreTenant(false);
        }
    }

    private ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setProductCode(p.getProductCode());
        dto.setProductName(p.getProductName());
        dto.setUnitPriceCent(p.getUnitPriceCent());
        dto.setStatus(p.getStatus());
        dto.setCategoryId(p.getCategoryId());
        dto.setSales(p.getSales() != null ? p.getSales() : 0L);
        if (p.getCategoryId() != null) {
            Category c = categoryMapper.selectById(p.getCategoryId());
            dto.setCategoryName(c != null ? c.getCategoryName() : null);
        }
        dto.setStoreId(p.getStoreId());
        if (p.getStoreId() != null) {
            Store s = storeMapper.selectById(p.getStoreId());
            dto.setStoreName(s != null ? s.getStoreName() : null);
        }
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
