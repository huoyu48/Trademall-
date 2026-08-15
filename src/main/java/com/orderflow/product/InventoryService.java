package com.orderflow.product;

import java.util.List;

public interface InventoryService {
    void adjust(AdjustInventoryRequest request);

    List<InventoryDTO> list();

    List<InventoryDTO> lowStock();
}
