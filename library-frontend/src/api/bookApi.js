import axiosInstance from './axiosInstance'

export const getAllBooks = () => {
  return axiosInstance.get('/books')
}

export const getBookById = (id) => {
  return axiosInstance.get(`/books/${id}`)
}

export const createBook = (bookData) => {
  return axiosInstance.post('/books', bookData)
}

export const updateBook = (id, bookData) => {
  return axiosInstance.put(`/books/${id}`, bookData)
}

export const deleteBook = (id) => {
  return axiosInstance.delete(`/books/${id}`)
}