package com.orderflow.product;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Product;

import java.util.List;

public interface ProductService {
    ProductDTO create(CreateProductRequest request);

    ProductDTO update(Long productId, UpdateProductRequest request);

    ProductDTO detail(Long productId);

    PageResult<ProductDTO> page(int page, int size);

    /** 商家端分页：支持按分类过滤 + 关键词搜索（名称/编码模糊匹配）。 */
    PageResult<ProductDTO> page(int page, int size, Long categoryId, String keyword);

    /** 商城端分页：跨租户查所有启用商品，支持分类/关键词过滤。 */
    PageResult<ProductDTO> pageForMall(int page, int size, Long categoryId, String keyword);

    /** 商城端详情：跨租户查商品，仅展示启用商品。 */
    ProductDTO detailForMall(Long productId);

    /** 批量查商品（顾客下单时根据商品反推所属租户）。 */
    List<Product> listByIds(List<Long> ids);
}
