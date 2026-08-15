package com.orderflow.order;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderDTO> create(@Valid @RequestBody CreateOrderRequest request,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.success(orderService.create(request, idempotencyKey));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDTO> get(@PathVariable Long id) {
        return ApiResponse.success(orderService.get(id));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderDTO>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.page(page, size));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderDTO> confirm(@PathVariable Long id) {
        return ApiResponse.success(orderService.confirm(id));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<OrderDTO> ship(@PathVariable Long id) {
        return ApiResponse.success(orderService.ship(id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderDTO> complete(@PathVariable Long id) {
        return ApiResponse.success(orderService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDTO> cancel(@PathVariable Long id) {
        return ApiResponse.success(orderService.cancel(id));
    }

    @GetMapping("/stats")
    public ApiResponse<OrderStatsDTO> stats() {
        return ApiResponse.success(orderService.stats());
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<OrderStatusHistoryDTO>> history(@PathVariable Long id) {
        return ApiResponse.success(orderService.getHistory(id));
    }
}
