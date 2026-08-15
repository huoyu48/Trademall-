import { defineStore } from 'pinia'
import { login as platformLogin } from '../api/platform'

interface PlatformState {
  token: string
  userId: number
  username: string
  roles: string[]
}

export const usePlatformStore = defineStore('platform', {
  state: (): PlatformState => {
    const raw = localStorage.getItem('of_platform_user')
    const token = localStorage.getItem('of_platform_token') || ''
    if (raw) {
      const u = JSON.parse(raw)
      return { token, ...u }
    }
    return { token, userId: 0, username: '', roles: [] }
  },
  getters: {
    isPlatform: (s) => s.roles.includes('PLATFORM_ADMIN')
  },
  actions: {
    async login(username: string, password: string) {
      const data = await platformLogin(username, password)
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.roles = data.roles || []
      localStorage.setItem('of_platform_token', data.token)
      localStorage.setItem('of_platform_user', JSON.stringify({
        userId: this.userId, username: this.username, roles: this.roles
      }))
    },
    logout() {
      this.$reset()
      localStorage.removeItem('of_platform_token')
      localStorage.removeItem('of_platform_user')
    }
  }
})
