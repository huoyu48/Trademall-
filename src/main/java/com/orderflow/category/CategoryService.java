package com.orderflow.category;

import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.Category;

import java.util.List;

public interface CategoryService {
    Category create(CreateCategoryRequest request);

    Category update(Long id, UpdateCategoryRequest request);

    Category detail(Long id);

    PageResult<Category> page(int page, int size);

    List<Category> listAll();
}
