export const mapErrorMessage = (message) => {
  if (!message) return "Ocurrió un error inesperado";

  const map = {
    // Login
    "Invalid email or password": "Correo o contraseña incorrectos",
    "Account is not activated": "Tu cuenta no está activada. Revisa tu correo",
    "Account is locked until": "Tu cuenta está bloqueada temporalmente. Intenta más tarde",
    "Account locked due to too many failed attempts": "Demasiados intentos fallidos. Cuenta bloqueada temporalmente",
    // Registro
    "Email is already registered": "Ya existe una cuenta con ese correo",
    "User already exists": "Ya existe una cuenta con ese correo",
    // Activación
    "Invalid activation code": "El código de activación es incorrecto",
    "Activation code has expired": "El código de activación expiró. Solicita uno nuevo",
    "Account is already activated": "Tu cuenta ya está activada. Puedes iniciar sesión",
    "User not found": "No encontramos una cuenta con ese correo",
    // Recuperación de contraseña
    "Invalid recovery code": "El código de recuperación es incorrecto",
    "Recovery code has already been used": "Este código ya fue utilizado",
    "Recovery code has expired": "El código de recuperación expiró. Solicita uno nuevo",
    "Passwords do not match": "Las contraseñas no coinciden",
    // Genéricos
    "An unexpected error occurred": "Error inesperado. Intenta de nuevo",
    "Malformed JSON request": "Error en la solicitud. Intenta de nuevo",
  };

  // Busca coincidencia exacta
  if (map[message]) return map[message];

  // Busca coincidencia parcial
  for (const [key, value] of Object.entries(map)) {
    if (message.toLowerCase().includes(key.toLowerCase())) return value;
  }

  // Si viene en formato de validación "field: msg, field: msg"
  if (message.includes(":")) {
    return "Verifica los datos ingresados";
  }

  return message;
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
