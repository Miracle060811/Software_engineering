let accessToken = "";

export const getAccessToken = () => accessToken;

export const setAccessToken = (token) => {
  accessToken = typeof token === "string" ? token : "";
};

export const clearAccessToken = () => {
  accessToken = "";
};
