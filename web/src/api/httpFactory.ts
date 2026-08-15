import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * 响应拦截器已将 `response.data.data` 拆包返回，因此运行时拿到的是业务数据 T，
 * 这里把 Axios 默认 `Promise<AxiosResponse<T>>` 的类型收敛为 `Promise<T>`。
 */
type TypedAxios = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'request' | 'head' | 'patch'> & {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  head<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  request<T = any>(config: AxiosRequestConfig): Promise<T>
}

/**
 * 三套身份（商家/平台/顾客）各自持有独立的 token key 与 401 跳转路径，
 * 避免共用一个 token 导致角色串号。登录失效时各自跳回自己的登录页。
 */
export function createHttp(tokenKey: string, loginPath: string): TypedAxios {
  const service = axios.create({
    baseURL: '/api',
    timeout: 15000
  }) as unknown as TypedAxios

  service.interceptors.request.use((config) => {
    const token = localStorage.getItem(tokenKey)
    if (token) config.headers.Authorization = `Bearer ${token}`
    config.headers['X-Request-Id'] = (crypto as any).randomUUID()
    return config
  })

  service.interceptors.response.use(
    (resp) => {
      const body = resp.data
      const code = body && (body.code === 0 || body.code === '0' || body.code === 'OK') ? 0 : body?.code
      if (code === 0) return body.data
      const msg = body?.message || '请求失败'
      if (code === 40101 || code === 401 || code === '401') {
        localStorage.removeItem(tokenKey)
        router.replace(loginPath)
      }
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    },
    (error) => {
      const status = error.response?.status
      const body = error.response?.data
      if (status === 401) {
        localStorage.removeItem(tokenKey)
        router.replace(loginPath)
      }
      // 403 优先取后端业务 message（如“无权限访问”）
      ElMessage.error(body?.message || error.message || '网络错误')
      return Promise.reject(error)
    }
  )

  return service
}
