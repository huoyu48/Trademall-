import { createHttp } from './httpFactory'

/** 平台后台专用 http 实例（token 存 of_platform_token，失效跳统一登录页并选中平台身份）。 */
export default createHttp('of_platform_token', '/login?role=platform')
