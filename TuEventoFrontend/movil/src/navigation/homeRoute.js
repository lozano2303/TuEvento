/**
 * Pantalla de inicio según el rol. El admin entra directo a su panel (igual que la web,
 * que lo lleva a /admin-panel); el resto de usuarios, a las pestañas principales.
 */
export const homeRouteFor = (role) => (role === "ADMIN" ? "AdminHome" : "Main");

// Pantallas previas a la sesión: se permiten a cualquiera (p. ej. al cerrar sesión)
const PUBLIC_ROUTES = new Set([
  "Landing", "Login", "Register", "Terms", "ForgotPassword", "ResetPassword", "Activate",
]);

/** Pantallas que puede abrir el admin; las de usuario (eventos, perfil, cartera…) le están vedadas. */
export const isAdminAllowedRoute = (name) =>
  PUBLIC_ROUTES.has(name) || name.startsWith("Admin") || name.startsWith("OrganizerRequest");
