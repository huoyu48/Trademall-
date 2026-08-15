package com.orderflow.product;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductDTO> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(productService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDTO> update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productService.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDTO> detail(@PathVariable Long id) {
        return ApiResponse.success(productService.detail(id));
    }

    @GetMapping
    public ApiResponse<PageResult<ProductDTO>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(productService.page(page, size));
    }
}
