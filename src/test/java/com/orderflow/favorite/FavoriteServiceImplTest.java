package com.orderflow.favorite;

import com.orderflow.common.BizException;
import com.orderflow.common.PageResult;
import com.orderflow.domain.entity.ProductFavorite;
import com.orderflow.domain.mapper.ProductFavoriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/* 使用 Mockito 模拟 Mapper，只测试 Service 业务逻辑 不连接 Mysql */

@ExtendWith(MockitoExtension.class)//让 JUnit5 在每个测试前自动创建 Mockito 对象
public class FavoriteServiceImplTest {
    @Mock //创建一个家 Mapper，不会真正执行 SQL。
    private ProductFavoriteMapper favoriteMapper;

    @InjectMocks //创建 FavoriteServiceImpl ,并把上面的假 Mapper 注入构造器
    private FavoriteServiceImpl favoriteService;

    @Test //标记这是一个可有 JUnit执行的测试方法
    void shouldAddActiveProductCurrentCustomerFavorites() {
        //Arrange:当 Service 查询商品 30 时，Mapper 固定返回 1.表示测试场景中该商品存在且上架。
        when(favoriteMapper.countActiveProduct(30L)).thenReturn(1L);

        //Act:tenantId=10、customerId=20、productId=30。
        FavoriteStatusDTO result = favoriteService.add(10L, 20L, 30L);

        //Assert:验证 Service 返回的商品 ID 和收藏状态。
        assertTrue(result.favorited());
        assertEquals(30L, result.productId());

        //ArgumentCaptor 用来拿到真正传给 Mapper 的 ProductFavorite。
        ArgumentCaptor<ProductFavorite> captor = ArgumentCaptor.forClass(ProductFavorite.class);

        //verify验证 insertIdempotently确实被调用过。capture同时保存他们的参数。
        verify(favoriteMapper).insertIdempotently(captor.capture());

        //检查 Service 组装 Entity 时没有把租户、顾客或商品搞错。
        assertEquals(10L, captor.getValue().getTenantId());
        assertEquals(20L, captor.getValue().getCustomerId());
        assertEquals(30L, captor.getValue().getProductId());
    }
    @Test
    void shouldRejectMissingOrDisabledProduct() {
        //返回 0 表示查不到有效上架商品。
        when(favoriteMapper.countActiveProduct(30L)).thenReturn(0L);

        //assertThrows 验证该场景必须抛出业务异常，不能返回成功
        assertThrows(BizException.class, () -> favoriteService.add(10L, 20L, 30L));

        //never()验证商品无效时插入方法一次都没有执行
        verify(favoriteMapper, never()).insertIdempotently(any(ProductFavorite.class));
    }
        @Test
        void shouldReturnNotFavoritedAfterRemove() {
            //取消收藏本身应该也要有幂等性，Mapper 删除 0 行也不影响最终状态。
            FavoriteStatusDTO result = favoriteService.remove(10L, 20L, 30L);

            //验证删除时同时传入了 tenantId、customerId、productId。
            verify(favoriteMapper).deleteByCustomerAndProduct(10L, 20L, 30L);
            assertFalse(result.favorited());

        }
        @Test
        void shouldClampUnsafePageArguments() {
            //创建一条假的收藏商品作为 Mapper 分页查询结果
            FavoriteProductDTO product = new FavoriteProductDTO();
            product.setProductId(30L);

            //模拟总数为 1
            when(favoriteMapper.countFavoriteProducts(10L, 20L)).thenReturn(1L);

            //service应将危险参数修正后，使用 offset=0，size=100 查询 Mapper。
            when(favoriteMapper.selectFavoriteProducts(10L, 20L, 0L, 100L)).thenReturn(List.of(product));

            //故意传入 page=-1，size=1000，测试 Service 的边界保护。
            PageResult<FavoriteProductDTO> result = favoriteService.page(10L, 20L, -1, 1000);
            assertEquals(1,result.getPage());
            assertEquals(100,result.getSize());
            assertEquals(1,result.getTotal());
            assertEquals(30L,result.getList().get(0).getProductId());
        }

}
