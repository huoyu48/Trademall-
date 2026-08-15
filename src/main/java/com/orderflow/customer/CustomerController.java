package com.orderflow.customer;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.BizException;
import com.orderflow.common.BizErrorCode;
import com.orderflow.common.PageResult;
import com.orderflow.category.CategoryService;
import com.orderflow.domain.entity.Category;
import com.orderflow.domain.entity.Product;
import com.orderflow.order.CreateOrderRequest;
import com.orderflow.order.OrderDTO;
import com.orderflow.order.OrderService;
import com.orderflow.product.ProductDTO;
import com.orderflow.product.ProductService;
import com.orderflow.security.LoginUser;
import com.orderflow.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;

    public CustomerController(ProductService productService, OrderService orderService,
                              CategoryService categoryService) {
        this.productService = productService;
        this.orderService = orderService;
        this.categoryService = categoryService;
    }

    /**
     * 商城首页商品：跨租户浏览所有入驻商家（status=1）的商品，
     * 支持分类过滤 + 关键词搜索，按销量降序。
     */
    @GetMapping("/products")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PageResult<ProductDTO>> products(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "12") int size,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) String keyword) {
        return ApiResponse.success(productService.pageForMall(page, size, categoryId, keyword));
    }

    /** 商城商品详情：跨租户查任意商品 */
    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ProductDTO> productDetail(@PathVariable Long id) {
        return ApiResponse.success(productService.detailForMall(id));
    }

    /** 商城分类导航：聚合所有租户的分类 */
    @GetMapping("/categories")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.success(categoryService.listAll());
    }

    /**
     * 顾客下单：复用商家下单链路（库存预占、幂等、营销减免、状态机），
     * 订单归属商品所在租户——一次下单只支持同一商家，跨商家需拆单。
     */
    @PostMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderDTO> createOrder(@RequestBody CustomerOrderRequest req) {
        LoginUser me = SecurityUtils.current();
        List<Long> productIds = req.getItems().stream().map(CreateOrderRequest.OrderItemRequest::getProductId).toList();
        List<Product> products = productService.listByIds(productIds);
        if (products.isEmpty()) {
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
        Long orderTenantId = products.get(0).getTenantId();
        for (Product p : products) {
            if (!orderTenantId.equals(p.getTenantId())) {
                throw new BizException(BizErrorCode.PRODUCT_NOT_IN_TENANT);
            }
        }

        CreateOrderRequest cor = new CreateOrderRequest();
        cor.setCustomerName(me.getUsername());
        cor.setPromoCode(req.getPromoCode());
        cor.setItems(req.getItems());
        cor.setCustomerId(me.getUserId());
        return ApiResponse.success(orderService.create(cor, UUID.randomUUID().toString(), orderTenantId));
    }

    /** 我的订单：按顾客身份查全部订单（跨租户） */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<OrderDTO>> myOrders() {
        LoginUser me = SecurityUtils.current();
        return ApiResponse.success(orderService.listByCustomer(me.getUserId()));
    }
}