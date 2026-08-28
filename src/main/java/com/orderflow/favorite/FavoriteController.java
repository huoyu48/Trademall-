package com.orderflow.favorite;

import com.orderflow.common.ApiResponse;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.security.LoginUser;
import com.orderflow.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*  顾客商品收藏 HTTP 接口。
 请求接口用来解析 URL 参数，取得登录人的身份
 先调用 service 再将结果统一封装成 json 格式返回
 */
@RestController //方法返回值自动转为 json
@RequestMapping("/customer/favorites") // 当前类中所有 URL 的统一前缀
@PreAuthorize("hasRole('CUSTOMER')  ") //在进入方法前校验 JWT 中是否有 CUSTOMER 角色
public class FavoriteController {

    //Controller 依赖 Service 不直接访问数据库
    private final FavoriteService favoriteService;

    //构造器注入 FavoriteService
    public FavoriteController(
            FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    //收藏商品
    @PostMapping("/{productId}") //将 HTTP post 请求映射到此方法
    public ApiResponse<FavoriteStatusDTO> add(@PathVariable Long productId) {
        LoginUser customer = currentCustomer();
        return ApiResponse.success(favoriteService.add(customer.getTenantId(), customer.getUserId(), productId));
    }

    //DELECT表示删除当前收藏关系
    @DeleteMapping("/{productId}")
    public ApiResponse<FavoriteStatusDTO> remove(@PathVariable Long productId) {
        LoginUser customer = currentCustomer();
        return ApiResponse.success(favoriteService.remove(customer.getTenantId(), customer.getUserId(), productId));
    }
    //GET只读取数据
    @GetMapping("/{productId}/status")
    public ApiResponse<FavoriteStatusDTO> status(@PathVariable Long productId) {
        LoginUser customer = currentCustomer();
        return ApiResponse.success(favoriteService.status(customer.getTenantId(), customer.getUserId(), productId));
    }
    //分页查询收藏商品
    @GetMapping
    public ApiResponse<PageResult<FavoriteProductDTO>> page(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int size){
            LoginUser customer = currentCustomer();
            return ApiResponse.success(favoriteService.page(customer.getTenantId(), customer.getUserId(), page, size));
        }

    //从 Spring SecurityContext 获取 JWT 解析后的当前顾客。customerId 不从请求参数中获取
    private LoginUser currentCustomer() {
        LoginUser customer = SecurityUtils.current();
        if (customer == null) {
            throw new BizException(40102, "请先登录");
        }
        return customer;
    }
}
