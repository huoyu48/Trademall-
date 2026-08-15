package com.orderflow.product;

import com.orderflow.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@RequestMapping("/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjustments")
    public ApiResponse<Void> adjust(@Valid @RequestBody AdjustInventoryRequest request) {
        inventoryService.adjust(request);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<InventoryDTO>> list() {
        return ApiResponse.success(inventoryService.list());
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<InventoryDTO>> lowStock() {
        return ApiResponse.success(inventoryService.lowStock());
    }
}
