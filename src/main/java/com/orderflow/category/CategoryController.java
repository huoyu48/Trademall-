package com.orderflow.category;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Category;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Category> create(@RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Category> update(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @GetMapping
    public ApiResponse<PageResult<Category>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(service.page(page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<Category>> listAll() {
        return ApiResponse.success(service.listAll());
    }
}
