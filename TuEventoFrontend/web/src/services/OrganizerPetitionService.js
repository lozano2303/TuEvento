import { httpRequest } from './httpClient.js';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const createPetition = async (formData) => {
  try {
    const response = await httpRequest(`${API_URL}/users/organizer-request`, {
      method: 'POST',
      body: formData,
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
      throw new Error(data?.message || response.statusText || 'Error al crear solicitud');
    }

    return {
      success: true,
      data: data?.data,
    };
  } catch (error) {
    console.error('Error en createPetition:', error);
    throw error;
  }
};

export const approveOrganizerRequest = async (petitionId) => {
  try {
    const response = await httpRequest(`${API_URL}/admin/organizer-requests/${petitionId}/approve`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
      throw new Error(data?.message || response.statusText || 'Error al aprobar solicitud');
    }

    return { success: true };
  } catch (error) {
    console.error('Error en approveOrganizerRequest:', error);
    throw error;
  }
};

export const rejectOrganizerRequest = async (petitionId) => {
  try {
    const response = await httpRequest(`${API_URL}/admin/organizer-requests/${petitionId}/reject`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
      throw new Error(data?.message || response.statusText || 'Error al rechazar solicitud');
    }

    return {
      success: true,
    };
  } catch (error) {
    console.error('Error en rejectOrganizerRequest:', error);
    throw error;
  }
};

export const getPetitionStatus = async () => {
  try {
    const response = await httpRequest(`${API_URL}/users/organizer-petition`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (response.status === 404 || response.status === 200) {
      const text = await response.text();
      const data = text ? JSON.parse(text) : null;
      return {
        success: true,
        data: data?.data || null,
      };
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
      throw new Error(data?.message || response.statusText || 'Error al obtener estado');
    }

    return {
      success: true,
      data: data?.data,
    };
  } catch (error) {
    console.error('Error en getPetitionStatus:', error);
    throw error;
  }
};