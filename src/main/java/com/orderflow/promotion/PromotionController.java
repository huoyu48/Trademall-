package com.orderflow.promotion;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Promotion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/promotions")
public class PromotionController {

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Promotion> create(@RequestBody CreatePromotionRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Promotion> update(@PathVariable Long id, @RequestBody UpdatePromotionRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Promotion> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @GetMapping
    public ApiResponse<PageResult<Promotion>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(service.page(page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<Promotion>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
