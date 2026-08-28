// 引入顾客端专用 axios 示例
// customerHttp 会自动在请求头中添加顾客 JWT，并统一处理错误响应
// 已经配置好的 axios 实例，用于发送顾客端的 HTTP 请求
import http from "./customerHttp";
import type { PageResult, Product } from "../types";

// 后端返回的收藏状态
export interface FavoriteStatus{
    // 本次操作对应的商品 id
    productId: number;
    // 是否收藏
    favorited: boolean;
}
/*收藏列表中的商品数据。
 *pick 从公共 product 接口复用指定字段。
 *避免避免在这里重复声明商品名称、价格和店铺等类型
*/
export interface FavoriteProduct extends Pick<Product, 'productCode' | 'productName' | 'unitPriceCent' | 'categoryId' | 'categoryName' | 'storeId' | 'storeName' | 'sales'>{
    favoriteId: number//收藏记录 ID，作为 v-for 的唯一 key。
    productId: number//商品 ID，用于跳转详情和取消收藏。
    favoritedAt:string//后端 LocalDateTime
}

/* 收藏商品
 *<FavoriteStatus>是泛型，告诉 TypeScript 这个 Promise 成功后返回什么结构。
 */
export function addFavorite(productId: number) {
    return http.post<FavoriteStatus>(`/customer/favorites/${productId}`)
}

// DELECT 只删除顾客和商品之间的关系
export function removeFavorite(productId: number) {
    return http.delete<FavoriteStatus>(`/customer/favorites/${productId}`)
}
//进入详情页调用，用于决定按钮显示收藏或者已收藏。
export function favoriteStatus(productId: number) {
    return http.get<FavoriteStatus>(`/customer/favorites/${productId}/status`)
}
// 分页查询当前顾客的收藏列表
export function favoriteProducts(page = 1, size = 10) {
    //params 会被 axios 转成？page=1&size=12查询字符串。
    return http.get<PageResult<FavoriteProduct>>(`/customer/favorites`, { params: { page, size } })
}
