import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileText, CheckCircle } from 'lucide-react';
import { createPetition } from '../services/OrganizerPetitionService.js';

const OrganizerPetitionForm = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileSelect = (e) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedFile) {
      setError('Selecciona un documento');
      return;
    }

    const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg'];
    if (!allowedTypes.includes(selectedFile.type)) {
      setError('Solo archivos PDF, JPG o PNG');
      return;
    }

    if (selectedFile.size > 5 * 1024 * 1024) {
      setError('El archivo no puede ser mayor a 5MB');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const token = localStorage.getItem('token');
      const userID = localStorage.getItem('userID');

      if (!token || !userID) {
        setError('Sesión expirada. Inicia sesión nuevamente.');
        navigate('/login');
        return;
      }

      const formData = new FormData();
      formData.append('userID', parseInt(userID));
      formData.append('file', selectedFile);

      const result = await createPetition(formData);

      if (result.success) {
        setSuccess(true);
      } else {
        setError(result.message || 'Error al enviar la solicitud');
      }
    } catch (err) {
      setError('Error de conexión. Verifica que el backend esté ejecutándose.');
      console.error('Error submitting petition:', err);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div
        className="min-h-screen flex items-center justify-center px-4"
        style={{ background: 'linear-gradient(160deg, #0f0a1e 0%, #1a0f2e 50%, #120820 100%)' }}
      >
        <div
          className="rounded-2xl p-10 text-center max-w-md w-full"
          style={{
            background: 'rgba(88, 28, 135, 0.2)',
            border: '0.5px solid rgba(167, 139, 250, 0.25)',
            backdropFilter: 'blur(16px)',
          }}
        >
          <div
            className="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6"
            style={{
              background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
              boxShadow: '0 0 32px rgba(124, 58, 237, 0.5)',
            }}
          >
            <CheckCircle className="w-10 h-10 text-white" />
          </div>
          <h2 className="text-2xl font-bold mb-3" style={{ color: '#e9d5ff' }}>
            ¡Solicitud enviada!
          </h2>
          <p className="text-sm mb-8" style={{ color: 'rgba(196, 181, 253, 0.65)' }}>
            Tu solicitud fue enviada exitosamente. El administrador la revisará pronto y recibirás una respuesta.
          </p>
          <button
            onClick={() => navigate('/')}
            className="w-full py-3 rounded-xl font-medium text-sm transition-all hover:brightness-110"
            style={{
              background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
              color: '#fff',
              boxShadow: '0 0 16px rgba(124, 58, 237, 0.4)',
            }}
          >
            Volver al inicio
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      className="min-h-screen text-white"
      style={{ background: 'linear-gradient(160deg, #0f0a1e 0%, #1a0f2e 50%, #120820 100%)' }}
    >
      <div className="px-6 py-10">
        <div className="max-w-2xl mx-auto">

          {error && (
            <div
              className="flex items-center justify-between rounded-xl px-4 py-3 mb-6 text-sm"
              style={{
                background: 'rgba(220, 38, 38, 0.15)',
                border: '0.5px solid rgba(220, 38, 38, 0.4)',
                color: '#fca5a5',
              }}
            >
              <span>{error}</span>
              <button
                onClick={() => setError(null)}
                className="text-lg leading-none ml-4 opacity-60 hover:opacity-100"
              >
                ×
              </button>
            </div>
          )}

          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: 'rgba(88, 28, 135, 0.15)',
              border: '0.5px solid rgba(167, 139, 250, 0.2)',
              backdropFilter: 'blur(16px)',
            }}
          >
            {/* Encabezado */}
            <div
              className="px-8 py-8 text-center"
              style={{ borderBottom: '0.5px solid rgba(167, 139, 250, 0.15)' }}
            >
              <div
                className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
                style={{
                  background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
                  boxShadow: '0 0 24px rgba(124, 58, 237, 0.45)',
                }}
              >
                <FileText className="w-8 h-8 text-white" />
              </div>
              <h2 className="text-xl font-bold mb-2" style={{ color: '#e9d5ff' }}>
                Solicitud de Organizador
              </h2>
              <p className="text-sm" style={{ color: 'rgba(196, 181, 253, 0.6)' }}>
                Sube un documento para ser aprobado como organizador de eventos.
              </p>
            </div>

            <div className="px-8 py-8 space-y-6">

              {/* Zona de upload */}
              <div>
                <label
                  className="block text-sm font-medium mb-3"
                  style={{ color: 'rgba(196, 181, 253, 0.8)' }}
                >
                  Documento <span style={{ color: '#f87171' }}>*</span>
                </label>
                <div
                  className="rounded-xl p-8 text-center transition-all"
                  style={{
                    border: dragActive
                      ? '1.5px dashed rgba(167, 139, 250, 0.8)'
                      : selectedFile
                      ? '1.5px dashed rgba(74, 222, 128, 0.6)'
                      : '1.5px dashed rgba(167, 139, 250, 0.3)',
                    background: dragActive
                      ? 'rgba(124, 58, 237, 0.12)'
                      : selectedFile
                      ? 'rgba(74, 222, 128, 0.05)'
                      : 'rgba(88, 28, 135, 0.1)',
                  }}
                  onDragEnter={handleDrag}
                  onDragLeave={handleDrag}
                  onDragOver={handleDrag}
                  onDrop={handleDrop}
                >
                  {selectedFile ? (
                    <div className="space-y-2">
                      <div
                        className="w-12 h-12 rounded-full flex items-center justify-center mx-auto"
                        style={{ background: 'rgba(74, 222, 128, 0.15)' }}
                      >
                        <FileText className="w-6 h-6" style={{ color: '#4ade80' }} />
                      </div>
                      <p className="font-medium text-sm" style={{ color: '#e9d5ff' }}>
                        {selectedFile.name}
                      </p>
                      <p className="text-xs" style={{ color: 'rgba(196, 181, 253, 0.5)' }}>
                        {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                      </p>
                      <p className="text-xs font-medium" style={{ color: '#4ade80' }}>
                        ✓ Archivo válido
                      </p>
                      <button
                        type="button"
                        onClick={() => setSelectedFile(null)}
                        className="text-xs underline transition-opacity hover:opacity-80 mt-1"
                        style={{ color: 'rgba(196, 181, 253, 0.5)' }}
                      >
                        Cambiar archivo
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      <div
                        className="w-14 h-14 rounded-full flex items-center justify-center mx-auto"
                        style={{ background: 'rgba(124, 58, 237, 0.2)' }}
                      >
                        <Upload className="w-6 h-6" style={{ color: 'rgba(196, 181, 253, 0.7)' }} />
                      </div>
                      <div>
                        <p className="text-sm font-medium mb-1" style={{ color: '#e9d5ff' }}>
                          Arrastrá y soltá tu documento aquí
                        </p>
                        <p className="text-xs mb-4" style={{ color: 'rgba(196, 181, 253, 0.45)' }}>
                          o hacé clic para seleccionar un archivo
                        </p>
                        <input
                          type="file"
                          accept=".pdf,.jpg,.jpeg,.png"
                          onChange={handleFileSelect}
                          className="hidden"
                          id="file-upload"
                        />
                        <label
                          htmlFor="file-upload"
                          className="inline-block text-sm font-medium px-5 py-2 rounded-xl cursor-pointer transition-all hover:brightness-110"
                          style={{
                            background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
                            color: '#fff',
                            boxShadow: '0 0 12px rgba(124, 58, 237, 0.35)',
                          }}
                        >
                          Seleccionar archivo
                        </label>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Info box */}
              <div
                className="rounded-xl p-4"
                style={{
                  background: 'rgba(124, 58, 237, 0.1)',
                  border: '0.5px solid rgba(167, 139, 250, 0.25)',
                }}
              >
                <h3 className="text-sm font-medium mb-2" style={{ color: '#c4b5fd' }}>
                  Documentos requeridos
                </h3>
                <p className="text-xs mb-3" style={{ color: 'rgba(196, 181, 253, 0.6)' }}>
                  Sube tu documento de identidad o registro para que un administrador lo apruebe.
                </p>
                <div
                  className="pt-3 text-xs"
                  style={{
                    borderTop: '0.5px solid rgba(167, 139, 250, 0.2)',
                    color: 'rgba(196, 181, 253, 0.5)',
                  }}
                >
                  Formatos aceptados: PDF, JPG, PNG · Máximo 5MB
                </div>
              </div>

              {/* Botones */}
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => navigate(-1)}
                  className="flex-1 py-3 rounded-xl text-sm font-medium transition-all hover:opacity-80"
                  style={{
                    background: 'transparent',
                    border: '0.5px solid rgba(167, 139, 250, 0.3)',
                    color: 'rgba(196, 181, 253, 0.7)',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  onClick={handleSubmit}
                  disabled={loading || !selectedFile}
                  className="flex-1 py-3 rounded-xl text-sm font-medium transition-all hover:brightness-110 disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{
                    background: 'linear-gradient(135deg, #7c3aed 0%, #9333ea 100%)',
                    color: '#fff',
                    boxShadow: selectedFile ? '0 0 16px rgba(124, 58, 237, 0.4)' : 'none',
                  }}
                >
                  {loading ? 'Enviando...' : 'Enviar solicitud'}
                </button>
              </div>

            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrganizerPetitionForm;