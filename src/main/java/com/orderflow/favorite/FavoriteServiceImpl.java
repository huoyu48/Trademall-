package com.orderflow.favorite;

import com.orderflow.common.BizErrorCode;
import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.ProductFavorite;
import com.orderflow.domain.mapper.ProductFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/*  收藏业务实现类
1、先校验业务条件
2、组装 entity
3、调用 Mapper 读写数据库
4、组装接口返回值
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {
    //限制单次查询的最大数量，防止大查询拖垮数据库
    private static final int MAX_PAGE_SIZE = 100;
    // final 表示依赖在构造完成后不能被替换
    private final ProductFavoriteMapper favoriteMapper;

    // 构造器注入
    public FavoriteServiceImpl(ProductFavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    // 收藏商品
    @Override
    @Transactional
    public FavoriteStatusDTO add(Long tenantId, Long customerId, Long productId) {
        // 业务校验：只允许收藏存在且上架的商品
        if (favoriteMapper.countActiveProduct(productId) == 0) {
            // 抛出异常
            throw new BizException(BizErrorCode.PRODUCT_NOT_FOUND);
        }
        // 组装待写入数据库的 entity
        ProductFavorite favorite = new ProductFavorite();
        favorite.setTenantId(tenantId);
        favorite.setCustomerId(customerId);
        favorite.setProductId(productId);
        favorite.setCreatedAt(LocalDateTime.now());

        // 调用方法来安全的处理重复收藏请求
        favoriteMapper.insertIdempotently(favorite);

        // 不直接返回 entity，而是只返回前端需要的状态 DTO
        return new FavoriteStatusDTO(productId, true);
    }

    // 取消收藏
    @Override
    @Transactional
    public FavoriteStatusDTO remove(Long tenantId, Long customerId, Long productId) {
        // 删除条件包含租户和顾客，不能只根据 productid 来删除
        favoriteMapper.deleteByCustomerAndProduct(tenantId, customerId, productId);
        return new FavoriteStatusDTO(productId, false);

    }
    // 查询收藏状态
    @Override
    @Transactional(readOnly = true)//只读
    public FavoriteStatusDTO status(Long tenantId, Long customerId, Long productId) {
        boolean favorited = favoriteMapper.countByCustomerAndProduct(tenantId, customerId, productId) > 0;
        return new FavoriteStatusDTO(productId, favorited);
    }
    // 分页查询收藏
    @Override
    @Transactional(readOnly = true)
    public PageResult<FavoriteProductDTO>page(
        Long tenantId,Long customerId, int page, int size) {
            // 参数安全处理，页码小于 1 且每页不超过 100
            int safePage = Math.max(page,1);
            int safeSize = Math.min(Math.max(size,1),MAX_PAGE_SIZE);
            // SQL LIMIT 使用游标
            long offset = (long) (safePage - 1) * safeSize;
            // 先查总数 前端需要计算总页数
            long total = favoriteMapper.countFavoriteProducts(tenantId, customerId);
            // 没有数据是不在执行列表 SQL
            List<FavoriteProductDTO> list = total ==0 ? List.of() : favoriteMapper.selectFavoriteProducts(tenantId, customerId, offset, safeSize);
            // 使用项目统一分页结构
            return PageResult.of(list, total, safePage, safeSize);

        }

}
