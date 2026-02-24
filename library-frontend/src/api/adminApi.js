import axiosInstance from './axiosInstance'

export const getAllUsers = () => {
  return axiosInstance.get('/admin/users')
}

export const toggleSuspend = (userId) => {
  return axiosInstance.put(`/admin/users/${userId}/suspend`)
}

export const deleteUser = (userId) => {
  return axiosInstance.delete(`/admin/users/${userId}`)
}