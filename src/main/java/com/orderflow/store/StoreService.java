package com.orderflow.store;

import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Store;

import java.util.List;

public interface StoreService {
    Store create(CreateStoreRequest request);

    Store update(Long id, UpdateStoreRequest request);

    Store detail(Long id);

    PageResult<Store> page(int page, int size);

    List<Store> listAll();
}
