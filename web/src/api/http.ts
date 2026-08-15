import { createHttp } from './httpFactory'

/**
 * 商家后台专用 http 实例（token 存 of_token，失效跳 /login）。
 * 保持默认导出，兼容所有现有 api 模块的 `import http from './http'`。
 */
export default createHttp('of_token', '/login')
