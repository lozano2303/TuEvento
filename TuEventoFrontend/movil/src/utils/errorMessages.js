import i18n from "../i18n";

// Mensajes del backend → clave i18n (namespace mobileErrors). Se traducen al
// momento de llamar para respetar el idioma activo de la UI.
export const mapErrorMessage = (message) => {
  if (!message) return i18n.t("mobileErrors.unexpected");

  const map = {
    // Login
    "Invalid email or password": "invalidCredentials",
    "Account is not activated": "notActivated",
    "Account is locked until": "locked",
    "Account locked due to too many failed attempts": "tooManyAttempts",
    // Registro — BusinessException desde ValidationUtils y UseCase
    "Only @gmail.com emails are accepted": "gmailOnly",
    "Password must be at least 8 characters and include uppercase, lowercase, number and special character":
      "passwordWeak",
    "Name must contain at least two words with letters only":
      "nameInvalid",
    "Email is already registered": "emailRegistered",
    "User already exists": "emailRegistered",
    // Activación
    "Invalid activation code": "invalidActivationCode",
    "Activation code has expired": "activationExpired",
    "Account is already activated": "alreadyActivated",
    "User not found": "userNotFound",
    // Recuperación de contraseña
    "This email is not registered in the system": "userNotFound",
    "Invalid recovery code": "invalidRecoveryCode",
    "Recovery code has already been used": "recoveryUsed",
    "Recovery code has expired": "recoveryExpired",
    "Passwords do not match": "passwordsNoMatch",
    // QR de tickets / escaneo
    "Ticket is not in a state that can generate a QR":
      "ticketNotValidatable",
    "TICKET_NOT_VALIDATABLE": "ticketNotValidatable",
    "The event is not active": "eventNotActive",
    "EVENT_NOT_ACTIVE": "eventNotActive",
    "This order has no remaining tickets to validate":
      "orderNoTickets",
    "ORDER_NO_VALIDATABLE_TICKETS": "orderNoTickets",
    "QR code is invalid or has expired": "qrInvalid",
    "QR_INVALID": "qrInvalid",
    "QR code is no longer valid": "qrReplaced",
    "QR_EXPIRED_OR_REPLACED": "qrReplaced",
    "You are not the organizer of this event":
      "notOrganizer",
    // OAuth (errores lanzados por oauthService)
    "Usuario canceló el inicio de sesión": "oauthCancelled",
    "GOOGLE_SIGNIN_EXPO_GO": "googleExpoGo",
    "GOOGLE_SIGNIN_DEVELOPER_ERROR": "googleDeveloperError",
    "OAuth state mismatch": "oauthFailed",
    "Error en el flujo OAuth": "oauthFailed",
    "Error OAuth": "oauthFailed",
    // Genéricos
    "An unexpected error occurred": "unexpectedRetry",
    "Malformed JSON request": "badRequest",
  };

  // Coincidencia exacta
  if (map[message]) return i18n.t(`mobileErrors.${map[message]}`);

  // Coincidencia parcial
  for (const [key, value] of Object.entries(map)) {
    if (message.toLowerCase().includes(key.toLowerCase())) return i18n.t(`mobileErrors.${value}`);
  }

  return message;
};

/**
 * Parsea errores de validación del backend (formato "field: msg, field: msg")
 * y los convierte en un objeto { field: mensajeTraducido }.
 * Retorna null si el mensaje no es de validación por campos.
 */
export const parseValidationErrors = (message) => {
  if (!message || !message.includes(":")) return null;

  // El GlobalExceptionHandler une los errores con ", " → "email: must not be blank, password: size must be..."
  const parts = message.split(", ");
  const fieldErrors = {};

  const fieldMap = {
    email: {
      "must not be blank": "fields.emailRequired",
      "must be a well-formed email address": "fields.emailInvalid",
      "size must be between": "fields.emailTooLong",
    },
    password: {
      "must not be blank": "fields.passwordRequired",
      "size must be between": "fields.passwordSize",
    },
    fullName: {
      "must not be blank": "fields.fullNameRequired",
      "size must be between": "fields.fullNameTooLong",
    },
    activationCode: {
      "must not be blank": "fields.activationCodeRequired",
      "size must be between": "fields.activationCodeLength",
    },
  };

  for (const part of parts) {
    const colonIdx = part.indexOf(":");
    if (colonIdx === -1) continue;
    const field = part.substring(0, colonIdx).trim();
    const constraint = part.substring(colonIdx + 1).trim().toLowerCase();

    if (fieldMap[field]) {
      for (const [key, msg] of Object.entries(fieldMap[field])) {
        if (constraint.includes(key.toLowerCase())) {
          fieldErrors[field] = i18n.t(`mobileErrors.${msg}`);
          break;
        }
      }
      if (!fieldErrors[field]) {
        fieldErrors[field] = i18n.t("mobileErrors.fields.invalidField", { field });
      }
    }
  }

  return Object.keys(fieldErrors).length > 0 ? fieldErrors : null;
};

export const parseApiError = async (response) => {
  try {
    const data = await response.json();
    if (!data.success && data.message) {
      return mapErrorMessage(data.message);
    }
    return null;
  } catch {
    return i18n.t("mobileErrors.connection");
  }
};
