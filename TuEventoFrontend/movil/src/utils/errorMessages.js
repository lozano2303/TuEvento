export const mapErrorMessage = (message) => {
  if (!message) return "Ocurrió un error inesperado";

  const map = {
    // Login
    "Invalid email or password": "Correo o contraseña incorrectos",
    "Account is not activated": "Tu cuenta no está activada. Revisa tu correo",
    "Account is locked until": "Tu cuenta está bloqueada temporalmente. Intenta más tarde",
    "Account locked due to too many failed attempts": "Demasiados intentos fallidos. Cuenta bloqueada temporalmente",
    // Registro — BusinessException desde ValidationUtils y UseCase
    "Only @gmail.com emails are accepted": "Solo se aceptan correos @gmail.com",
    "Password must be at least 8 characters and include uppercase, lowercase, number and special character":
      "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial",
    "Name must contain at least two words with letters only":
      "El nombre debe tener al menos dos palabras y solo letras",
    "Email is already registered": "Ya existe una cuenta con ese correo",
    "User already exists": "Ya existe una cuenta con ese correo",
    // Activación
    "Invalid activation code": "El código de activación es incorrecto",
    "Activation code has expired": "El código de activación expiró. Solicita uno nuevo",
    "Account is already activated": "Tu cuenta ya está activada. Puedes iniciar sesión",
    "User not found": "No encontramos una cuenta con ese correo",
    // Recuperación de contraseña
    "This email is not registered in the system": "No encontramos una cuenta con ese correo",
    "Invalid recovery code": "El código de recuperación es incorrecto",
    "Recovery code has already been used": "Este código ya fue utilizado. Solicita uno nuevo",
    "Recovery code has expired": "El código de recuperación expiró. Solicita uno nuevo",
    "Passwords do not match": "Las contraseñas no coinciden",
    // Genéricos
    "An unexpected error occurred": "Error inesperado. Intenta de nuevo",
    "Malformed JSON request": "Error en la solicitud. Intenta de nuevo",
  };

  // Coincidencia exacta
  if (map[message]) return map[message];

  // Coincidencia parcial
  for (const [key, value] of Object.entries(map)) {
    if (message.toLowerCase().includes(key.toLowerCase())) return value;
  }

  return message;
};

/**
 * Parsea errores de validación del backend (formato "field: msg, field: msg")
 * y los convierte en un objeto { field: mensajeEnEspañol }.
 * Retorna null si el mensaje no es de validación por campos.
 */
export const parseValidationErrors = (message) => {
  if (!message || !message.includes(":")) return null;

  // El GlobalExceptionHandler une los errores con ", " → "email: must not be blank, password: size must be..."
  const parts = message.split(", ");
  const fieldErrors = {};

  const fieldMap = {
    email: {
      "must not be blank": "El correo es requerido",
      "must be a well-formed email address": "El correo no tiene un formato válido",
      "size must be between": "El correo es demasiado largo (máx. 255 caracteres)",
    },
    password: {
      "must not be blank": "La contraseña es requerida",
      "size must be between": "La contraseña debe tener entre 8 y 100 caracteres",
    },
    fullName: {
      "must not be blank": "El nombre completo es requerido",
      "size must be between": "El nombre es demasiado largo (máx. 100 caracteres)",
    },
    activationCode: {
      "must not be blank": "El código de activación es requerido",
      "size must be between": "El código debe tener exactamente 8 caracteres",
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
          fieldErrors[field] = msg;
          break;
        }
      }
      if (!fieldErrors[field]) {
        fieldErrors[field] = `Campo inválido: ${field}`;
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
    return "Error de conexión. Verifica tu internet";
  }
};
