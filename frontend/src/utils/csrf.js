const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

const readCookie = (name) => {
  const prefix = `${encodeURIComponent(name)}=`;
  const match = document.cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));
  return match ? decodeURIComponent(match.slice(prefix.length)) : "";
};

export const buildUploadHeaders = () => {
  const headers = {};
  const token = localStorage.getItem("token");
  const csrfToken = readCookie(CSRF_COOKIE_NAME);

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (csrfToken) {
    headers[CSRF_HEADER_NAME] = csrfToken;
  }
  return headers;
};

export { CSRF_COOKIE_NAME, CSRF_HEADER_NAME };
