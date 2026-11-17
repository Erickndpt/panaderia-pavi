export const environment = {
  apiBaseUrl: 'http://localhost:8081',
  keycloak: {
    issuer: 'http://localhost:38080/realms/panaderia',
    clientId: 'panaderia-frontend',
    redirectUri: 'http://localhost:4200/auth/callback',
    postLogoutRedirectUri: 'http://localhost:4200/',
  },
};
