import { useState, useEffect } from 'react';
import { Users, Calendar, CheckCircle, XCircle } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export default function AdminDashboard() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processing, setProcessing] = useState(null);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/admin/organizer-requests`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await response.json();
      if (response.ok) {
        setRequests(data.data || []);
      } else {
        setError(data.message || 'Error al cargar solicitudes');
      }
    } catch (err) {
      setError('Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    setProcessing(id);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/admin/organizer-requests/${id}/approve`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        setRequests(requests.filter(r => r.id !== id));
      } else {
        setError('Error al aprobar');
      }
    } catch (err) {
      setError('Error de conexión');
    } finally {
      setProcessing(null);
    }
  };

  const handleReject = async (id) => {
    setProcessing(id);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/admin/organizer-requests/${id}/reject`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        setRequests(requests.filter(r => r.id !== id));
      } else {
        setError('Error al rechazar');
      }
    } catch (err) {
      setError('Error de conexión');
    } finally {
      setProcessing(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-accent text-xl">Cargando...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold text-textPrimary mb-8">Panel de Administración</h1>
        
        {error && (
          <div className="bg-error/10 border border-error rounded-lg p-4 mb-4">
            <p className="text-error">{error}</p>
          </div>
        )}

        <div className="bg-surface rounded-xl p-6">
          <h2 className="text-xl font-semibold text-textPrimary mb-4 flex items-center gap-2">
            <Users className="w-5 h-5" />
            Solicitudes de Organizadores
          </h2>
          
          {requests.length === 0 ? (
            <p className="text-textMuted">No hay solicitudes pendientes</p>
          ) : (
            <div className="space-y-4">
              {requests.map((request) => (
                <div key={request.id} className="bg-surfaceAlt rounded-lg p-4 flex items-center justify-between">
                  <div>
                    <p className="text-textPrimary font-medium">{request.fullName}</p>
                    <p className="text-textMuted text-sm">{request.email}</p>
                    <p className="text-textMuted text-xs mt-1">
                      Fecha: {new Date(request.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleApprove(request.id)}
                      disabled={processing === request.id}
                      className="bg-success hover:bg-success disabled:opacity-50 text-textPrimary px-4 py-2 rounded-lg flex items-center gap-2"
                    >
                      <CheckCircle className="w-4 h-4" />
                      Aprobar
                    </button>
                    <button
                      onClick={() => handleReject(request.id)}
                      disabled={processing === request.id}
                      className="bg-error hover:bg-error disabled:opacity-50 text-textPrimary px-4 py-2 rounded-lg flex items-center gap-2"
                    >
                      <XCircle className="w-4 h-4" />
                      Rechazar
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
