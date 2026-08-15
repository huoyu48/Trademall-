import { createHttp } from './httpFactory'

/** 顾客商城专用 http 实例（token 存 of_customer_token，失效跳 /shop/login）。 */
export default createHttp('of_customer_token', '/shop/login')
