package com.orderflow.store;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Store;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/stores")
public class StoreController {

    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Store> create(@RequestBody CreateStoreRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Store> update(@PathVariable Long id, @RequestBody UpdateStoreRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Store> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @GetMapping
    public ApiResponse<PageResult<Store>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(service.page(page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<Store>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
