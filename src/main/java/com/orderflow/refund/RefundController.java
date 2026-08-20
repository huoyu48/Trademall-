package com.orderflow.refund;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Refund;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/refunds")
public class RefundController {

    private final RefundService service;

    public RefundController(RefundService service) {
        this.service = service;
    }

    @PostMapping("/apply")
    public ApiResponse<Refund> apply(@RequestParam Long orderId,
                                     @RequestParam(required = false) String reason) {
        return ApiResponse.success(service.apply(orderId, reason));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Refund> approve(@PathVariable Long id) {
        return ApiResponse.success(service.approve(id));
    }

    @PostMapping("/{id}/approve-return")
    public ApiResponse<Refund> approveReturn(@PathVariable Long id) {
        return ApiResponse.success(service.approveReturn(id));
    }

    @PostMapping("/{id}/receive-return")
    public ApiResponse<Refund> receiveReturn(@PathVariable Long id) {
        return ApiResponse.success(service.receiveReturn(id));
    }

    @PostMapping("/{id}/complete-return-refund")
    public ApiResponse<Refund> completeReturnRefund(@PathVariable Long id) {
        return ApiResponse.success(service.completeReturnRefund(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Refund> reject(@PathVariable Long id) {
        return ApiResponse.success(service.reject(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<Refund> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @GetMapping
    public ApiResponse<PageResult<Refund>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(service.page(page, size));
    }
}
