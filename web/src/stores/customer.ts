import { defineStore } from 'pinia'
import { login as customerLogin } from '../api/customer'

interface CustomerState {
  token: string
  userId: number
  username: string
  roles: string[]
}

export const useCustomerStore = defineStore('customer', {
  state: (): CustomerState => {
    const raw = localStorage.getItem('of_customer_user')
    const token = localStorage.getItem('of_customer_token') || ''
    if (raw) {
      const u = JSON.parse(raw)
      return { token, ...u }
    }
    return { token, userId: 0, username: '', roles: [] }
  },
  getters: {
    isCustomer: (s) => s.roles.includes('CUSTOMER')
  },
  actions: {
    async login(username: string, password: string) {
      const data = await customerLogin(username, password)
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.roles = data.roles || []
      localStorage.setItem('of_customer_token', data.token)
      localStorage.setItem('of_customer_user', JSON.stringify({
        userId: this.userId, username: this.username, roles: this.roles
      }))
    },
    logout() {
      this.$reset()
      localStorage.removeItem('of_customer_token')
      localStorage.removeItem('of_customer_user')
    }
  }
})
