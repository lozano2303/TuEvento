// Servicio de autenticación - conexión con el backend

import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// Función para iniciar sesión
export const loginUser = async (email, password) => {
  try {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al iniciar sesión');
    }

    return {
      success: true,
      data: {
        token: data.data.accessToken,
        refreshToken: data.data.refreshToken,
        userID: data.data.userId,
        alias: data.data.alias,
        role: data.data.role || 'USER',
      },
    };
    } catch (error) {
      console.error('Error en login:', error);
      // Traducir mensaje específico del backend
      if (error.message === "Account is not activated") {
        throw new Error("Cuenta no activada");
      }
      throw error;
    }
};

// Función para registrar usuario
export const registerUser = async (name, email, password) => {
  try {
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ fullName: name, email, password }),
    });

    const data = await response.json();

    if (!response.ok) {
      const errorMsg = data.message || data.data?.message || "Error al registrar usuario";
      throw new Error(errorMsg);
    }

    return {
      success: true,
      data: data.data.userId,
    };
  } catch (error) {
    console.error('Error en registro:', error);
    throw error;
  }
};
export const changePassword = async (oldPassword, newPassword) => {
  try {
    const response = await httpRequest(`${API_URL}/users/change-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ oldPassword, newPassword }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al cambiar contraseña');
    }

    return data;
  } catch (error) {
    console.error('Error al cambiar contraseña:', error);
    throw error;
  }
};

// Función para verificar código de activación
export const verifyActivationCode = async (email, code) => {
  try {
    const requestBody = { email, activationCode: code };
    console.log('=== LOGIN.JS SENDING ===');
    console.log('Request body:', requestBody);
    
    const response = await fetch(`${API_URL}/auth/activate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
    });

    const data = await response.json();

    if (!response.ok) {
      const errorMsg = data.message || data.data?.message || 'Error al verificar código';
      throw new Error(errorMsg);
    }

    return {
      success: true,
      message: data.message,
    };
    } catch (error) {
      console.error('Error en verifyActivationCode:', error);
      // Si el backend nos dice que la cuenta ya está activada, lanzamos un error con el mensaje exacto para mostrar
      if (error.message && error.message.toLowerCase().includes("cuenta ya está activada")) {
        throw new Error("Esta cuenta ya está activada");
      }
      throw error;
    }
};

// Función para reenviar código de activación
export const resendActivationCode = async (email) => {
  try {
    const response = await fetch(`${API_URL}/auth/resend-activation`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al reenviar código');
    }

    return {
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error en resendActivationCode:', error);
    throw error;
  }
};

// Función para reenviar código de activación por email (alias)
export const resendActivationCodeByEmail = async (email) => {
  return resendActivationCode(email);
};

// Función para recuperar contraseña (olvidé mi contraseña)
export const forgotPassword = async (email) => {
  try {
    const response = await fetch(`${API_URL}/auth/recover-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al recuperar contraseña');
    }

    return {
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error en forgotPassword:', error);
    throw error;
  }
};

// Función para restablecer contraseña con código
export const resetPassword = async (code, newPassword, email) => {
  try {
    const response = await fetch(`${API_URL}/auth/reset-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ 
        email: email, 
        code: code, 
        newPassword: newPassword,
        confirmPassword: newPassword 
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al restablecer contraseña');
    }

    return {
      success: true,
      message: data.message,
    };
  } catch (error) {
    console.error('Error en resetPassword:', error);
    throw error;
  }
};

// ── Google Sign-In (GSI / id_token flow) ─────────────────────────────────────

/**
 * Sends the Google ID Token (credential) received from @react-oauth/google's
 * GoogleLogin component to our backend for server-side verification.
 * Returns the same shape as loginUser() so the caller can handle both
 * flows identically.
 */
export const googleLogin = async (idToken) => {
  const response = await fetch(`${API_URL}/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  });

  const data = await response.json();

  if (!response.ok) {
    // Translate known backend error codes
    const msg = data?.message || '';
    if (msg.includes('EMAIL_ALREADY_EXISTS_AS_LOCAL') || msg.includes('ya está registrado con contraseña')) {
      throw new Error('Este correo ya tiene una cuenta con contraseña. Inicia sesión con tu correo y contraseña.');
    }
    if (msg.includes('GOOGLE_EMAIL_NOT_VERIFIED') || msg.includes('no tiene el correo verificado')) {
      throw new Error('Tu cuenta de Google no tiene el correo verificado. Verifica tu dirección en Google e intenta de nuevo.');
    }
    throw new Error(msg || 'No se pudo iniciar sesión con Google. Intenta de nuevo.');
  }

  return {
    success: true,
    data: {
      token:        data.data.accessToken,
      refreshToken: data.data.refreshToken ?? '',
      userID:       data.data.userId,
      alias:        data.data.alias,
      role:         data.data.role || 'USER',
    },
  };
};

// ── Reactivación de cuenta ───────────────────────────────────────────────────

// Solicita el envío de un código de reactivación al email del usuario.
// Siempre responde con éxito (el backend no revela si el email existe).
export const requestReactivation = async (email) => {
  const response = await fetch(`${API_URL}/auth/reactivate-request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  const data = await response.json();
  // El backend siempre devuelve 200 para no revelar si el email existe.
  // Solo propagamos error en casos de red o validación (@NotBlank, @Email).
  if (!response.ok) throw new Error(data?.message || 'Error al solicitar la reactivación');
  return { success: true, message: data.message };
};

// Confirma la reactivación con el token de 8 caracteres recibido por email.
export const confirmReactivation = async (token) => {
  const response = await fetch(`${API_URL}/auth/reactivate-confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data?.message || 'Error al confirmar la reactivación');
  return { success: true, message: data.message };
};

// Función para verificar si el correo ya existe
export const checkEmailExists = async (email) => {
  try {
    const response = await fetch(`${API_URL}/auth/check-email`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    const data = await response.json();

    if (response.status === 409) {
      // Email ya existe
      return { exists: true };
    }

    if (response.status === 200) {
      // Email no existe
      return { exists: false };
    }

    // Si el endpoint no existe, asumimos que el email no existe y dejamos que el registro falle
    return { exists: false };
  } catch (error) {
    // Si hay error (endpoint no existe, etc.), asumimos que el email no existe
    console.error('Error al verificar email:', error);
    return { exists: false };
  }
};
