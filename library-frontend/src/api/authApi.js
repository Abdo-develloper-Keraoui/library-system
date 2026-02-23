import axiosInstance from './axiosInstance'

export const login = (email, password) => {
  return axiosInstance.post('/auth/login', { email, password })
}

export const register = (email, password, firstName, lastName) => {
  return axiosInstance.post('/auth/register', { email, password, firstName, lastName })
}