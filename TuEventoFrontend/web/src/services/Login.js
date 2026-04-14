// Servicio de autenticación - conexión con el backend

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
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_URL}/users/change-password`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
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
    const response = await fetch(`${API_URL}/auth/activate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, activationCode: code }),
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
    throw error;
  }
};

// Función para reenviar código de activación
export const resendActivationCode = async (email) => {
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
