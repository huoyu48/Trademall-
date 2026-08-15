package com.orderflow.store;

import lombok.Data;

@Data
class CreateStoreRequest {
    private String storeCode;
    private String storeName;
    private String province;
    private String city;
    private String address;
    private Integer status;
}

@Data
class UpdateStoreRequest {
    private String storeName;
    private String province;
    private String city;
    private String address;
    private Integer status;
}
