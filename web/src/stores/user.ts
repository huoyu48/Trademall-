import { defineStore } from 'pinia'
import { login as loginApi } from '../api/auth'

interface UserState {
  token: string
  userId: number
  username: string
  tenantId: number
  tenantName: string
  roles: string[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => {
    const raw = localStorage.getItem('of_user')
    const token = localStorage.getItem('of_token') || ''
    if (raw) {
      const u = JSON.parse(raw)
      return { token, ...u }
    }
    return { token, userId: 0, username: '', tenantId: 0, tenantName: '', roles: [] }
  },
  getters: {
    isMerchant: (s) => s.roles.includes('MERCHANT_ADMIN'),
    isPlatform: (s) => s.roles.includes('PLATFORM_ADMIN'),
    isCustomer: (s) => s.roles.includes('CUSTOMER')
  },
  actions: {
    async login(username: string, password: string) {
      const data = await loginApi(username, password)
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.tenantId = data.tenantId
      this.tenantName = data.tenantName || ''
      this.roles = data.roles || []
      localStorage.setItem('of_token', data.token)
      localStorage.setItem('of_user', JSON.stringify({
        userId: this.userId,
        username: this.username,
        tenantId: this.tenantId,
        tenantName: this.tenantName,
        roles: this.roles
      }))
    },
    logout() {
      this.$reset()
      localStorage.removeItem('of_token')
      localStorage.removeItem('of_user')
    }
  }
})
