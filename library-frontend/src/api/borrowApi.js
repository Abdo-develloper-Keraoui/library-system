import axiosInstance from './axiosInstance'

export const borrowBook = (bookId) => {
  return axiosInstance.post(`/borrows/${bookId}/borrow`)
}

export const returnBook = (borrowId) => {
  return axiosInstance.put(`/borrows/${borrowId}/return`)
}

export const getMyBorrows = () => {
  return axiosInstance.get('/borrows/my')
}

export const getAllBorrows = () => {
  return axiosInstance.get('/borrows')
}