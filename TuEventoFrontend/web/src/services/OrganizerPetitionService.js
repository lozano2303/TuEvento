const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const createPetition = async (formData) => {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_URL}/organizer-petition`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al crear solicitud');
    }

    return {
      success: true,
      data: data.data,
    };
  } catch (error) {
    console.error('Error en createPetition:', error);
    throw error;
  }
};

export const getPetitionStatus = async () => {
  try {
    const token = localStorage.getItem('token');
    const userID = localStorage.getItem('userID');
    const response = await fetch(`${API_URL}/organizer-petition/user/${userID}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error al obtener estado');
    }

    return {
      success: true,
      data: data.data,
    };
  } catch (error) {
    console.error('Error en getPetitionStatus:', error);
    throw error;
  }
};