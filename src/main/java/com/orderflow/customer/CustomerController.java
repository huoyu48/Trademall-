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
import com.orderflow.order.OrderPricingDTO;
import com.orderflow.order.OrderService;
import com.orderflow.payment.MockCheckoutDTO;
import com.orderflow.payment.PaymentStatusDTO;
import com.orderflow.payment.PaymentService;
import com.orderflow.product.ProductDTO;
import com.orderflow.product.ProductService;
import com.orderflow.security.LoginUser;
import com.orderflow.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final PaymentService paymentService;

    public CustomerController(ProductService productService, OrderService orderService,
                              CategoryService categoryService, PaymentService paymentService) {
        this.productService = productService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.paymentService = paymentService;
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
    public ApiResponse<OrderDTO> createOrder(@Valid @RequestBody CustomerOrderRequest req,
                                             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        LoginUser me = SecurityUtils.current();
        Long orderTenantId = resolveOrderTenant(req);
        CreateOrderRequest cor = toOrderRequest(req, me);
        return ApiResponse.success(orderService.create(cor, idempotencyKey, orderTenantId));
    }

    /** 购物车实时展示“商品合计、店铺满减、应付金额”；不创建订单，也不扣库存。 */
    @PostMapping("/orders/preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderPricingDTO> previewOrder(@Valid @RequestBody CustomerOrderRequest req) {
        LoginUser me = SecurityUtils.current();
        return ApiResponse.success(orderService.preview(toOrderRequest(req, me), resolveOrderTenant(req)));
    }

    /** 生成项目内模拟收银台二维码；仅在手机端确认后更新订单状态。 */
    @PostMapping("/orders/{id}/payments/mock")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<MockCheckoutDTO> startMockPayment(@PathVariable Long id) {
        LoginUser me = SecurityUtils.current();
        return ApiResponse.success(paymentService.createMockCheckout(id, me.getUserId()));
    }

    /** 二维码弹窗只轮询本地订单状态，不能由电脑端直接伪造付款成功。 */
    @GetMapping("/orders/{id}/payments/mock/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PaymentStatusDTO> mockPaymentStatus(@PathVariable Long id) {
        LoginUser me = SecurityUtils.current();
        return ApiResponse.success(paymentService.getPaymentStatus(id, me.getUserId()));
    }

    /** 顾客主动取消待付款订单：回补库存，并关闭已生成但尚未支付的付款码。 */
    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderDTO> cancelPendingPaymentOrder(@PathVariable Long id) {
        LoginUser me = SecurityUtils.current();
        OrderDTO order = orderService.cancelPendingPaymentByCustomer(id, me.getUserId());
        paymentService.closePendingPayments(id);
        return ApiResponse.success(order);
    }

    /** 我的订单：按顾客身份查全部订单（跨租户） */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<OrderDTO>> myOrders() {
        LoginUser me = SecurityUtils.current();
        return ApiResponse.success(orderService.listByCustomer(me.getUserId()));
    }

    private Long resolveOrderTenant(CustomerOrderRequest req) {
        List<Long> productIds = req.getItems().stream().map(CreateOrderRequest.OrderItemRequest::getProductId).toList();
        List<Product> products = productService.listByIds(productIds);
        if (products.isEmpty()) throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        Long orderTenantId = products.get(0).getTenantId();
        for (Product product : products) {
            if (!orderTenantId.equals(product.getTenantId())) {
                throw new BizException(BizErrorCode.PRODUCT_NOT_IN_TENANT);
            }
        }
        return orderTenantId;
    }

    private CreateOrderRequest toOrderRequest(CustomerOrderRequest request, LoginUser customer) {
        CreateOrderRequest result = new CreateOrderRequest();
        result.setCustomerName(customer.getUsername());
        result.setPromoCode(request.getPromoCode());
        result.setItems(request.getItems());
        result.setCustomerId(customer.getUserId());
        return result;
    }
}
