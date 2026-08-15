import { createHttp } from './httpFactory'

/** 平台后台专用 http 实例（token 存 of_platform_token，失效跳 /platform/login）。 */
export default createHttp('of_platform_token', '/platform/login')
