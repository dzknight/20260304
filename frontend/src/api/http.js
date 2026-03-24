import axios from "axios";

const envApiBase = import.meta?.env?.VITE_API_BASE_URL;
export const API_BASE_URL = envApiBase && envApiBase.trim() ? envApiBase.trim() : "http://localhost:8080/api";

export const emitToast = (message, type = "error", duration = 3500) => {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent("app-toast", {
      detail: { message, type, duration }
    }));
  }
};

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: false
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const resolveErrorMessage = (error) => {
  const status = error?.response?.status;
  const serverMessage = error?.response?.data?.message;

  if (status === 401) return "인증이 필요하거나 로그인 정보가 만료되었습니다.";
  if (status === 403) return "해당 작업에 대한 권한이 없습니다.";
  if (status === 404) return "요청한 데이터가 없습니다.";
  if (status === 400) return serverMessage || "입력값이 올바르지 않습니다.";
  if (serverMessage) return serverMessage;
  if (error?.message) return error.message;
  return "요청을 처리하지 못했습니다.";
};

api.interceptors.response.use(
  (response) => response,
  (error) => {
    emitToast(resolveErrorMessage(error), "error");
    return Promise.reject(error);
  }
);

export default api;
