import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileText, CheckCircle, ArrowLeft, Send, Info, Clock, XCircle, AlertCircle } from 'lucide-react';
import Footer from '../layouts/Footer';
import { createPetition, getPetitionStatus } from '../services/OrganizerPetitionService';


const Organizador = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [petitionStatus, setPetitionStatus] = useState(null);

  useEffect(() => {
    checkPetitionStatus();
  }, []);

  const checkPetitionStatus = async () => {
    try {
      const result = await getPetitionStatus();
      if (result.data) {
        setPetitionStatus(result.data);
        if (result.data.status === 'APPROVED') {
          setSuccess(true);
        }
      }
    } catch (err) {
      setPetitionStatus(null);
    }
  };

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

  const handleSubmit = async () => {
    if (!selectedFile) {
      setError('Selecciona un documento');
      return;
    }
    setLoading(true);
    setError(null);
    
    try {
      const formData = new FormData();
      formData.append('document', selectedFile);
      
      await createPetition(formData);
      
      setSuccess(true);
      await checkPetitionStatus();
    } catch (err) {
      setError(err.message || 'Error al enviar la solicitud');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
  };

  return (
    <div className="min-h-screen bg-[#191022] text-slate-100 font-sans">
      <main className="max-w-[1024px] mx-auto w-full px-6 py-10">
        <section className="mb-10">
          <div className="flex flex-col gap-2">
            <h1 className="text-white text-4xl md:text-5xl font-black leading-tight tracking-tight">Solicitud de Organizador</h1>
            <p className="text-slate-400 text-lg max-w-2xl">
              Sube un documento de identidad para verificar tu cuenta y poder crear eventos dentro de la plataforma.
            </p>
          </div>
        </section>

        {petitionStatus && petitionStatus.status === 'PENDING' && (
          <div className="mb-8 bg-[#7f13ec]/10 border border-[#7f13ec]/20 rounded-xl p-5 flex items-center gap-4">
            <Clock className="text-[#7f13ec] shrink-0 !text-2xl" />
            <div>
              <p className="text-white font-semibold">Tu solicitud está en revisión</p>
              <p className="text-slate-400 text-sm">Enviada el {new Date(petitionStatus.applicationDate).toLocaleDateString('es-CO')}</p>
            </div>
          </div>
        )}

        {petitionStatus && petitionStatus.status === 'REJECTED' && (
          <div className="mb-8 bg-red-500/10 border border-red-500/20 rounded-xl p-5 flex items-center gap-4">
            <XCircle className="text-red-400 shrink-0 !text-2xl" />
            <div>
              <p className="text-white font-semibold">Tu solicitud fue rechazada</p>
              <p className="text-slate-400 text-sm">Puedes intentarlo nuevamente</p>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 flex flex-col gap-6">
            {error && (
              <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-4 flex items-center gap-3">
                <AlertCircle className="text-red-400 shrink-0" />
                <p className="text-red-300 text-sm">{error}</p>
              </div>
            )}
            
            {(success || petitionStatus?.status === 'PENDING' || petitionStatus?.status === 'APPROVED') ? (
              <div>
                <h3 className="text-white text-lg font-bold leading-tight tracking-tight mb-4 flex items-center gap-2">
                  <FileText className="text-[#7f13ec]" />
                  {petitionStatus?.status === 'APPROVED' ? 'Documento verificado' : 'Archivo cargado'}
                </h3>
                <div className="flex items-center gap-4 bg-[#251a31] border border-[#3d2a4d] rounded-xl px-4 py-3 justify-between">
                  <div className="flex items-center gap-4">
                    <div className="text-[#7f13ec] flex items-center justify-center rounded-lg bg-[#7f13ec]/10 shrink-0 size-12">
                      <FileText className="!text-2xl" />
                    </div>
                    <div className="flex flex-col justify-center">
                      <p className="text-white text-base font-semibold leading-none">Documento de identidad</p>
                      <p className="text-slate-400 text-sm mt-1">
                        {petitionStatus?.status === 'APPROVED' 
                          ? 'Aprobado' 
                          : petitionStatus?.applicationDate 
                            ? `Enviado el ${new Date(petitionStatus.applicationDate).toLocaleDateString('es-CO')}`
                            : 'Subido'}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {petitionStatus?.status === 'PENDING' && (
                      <div className="text-yellow-400 flex size-8 items-center justify-center bg-yellow-400/10 rounded-full">
                        <Clock className="!text-xl" />
                      </div>
                    )}
                    {petitionStatus?.status === 'APPROVED' && (
                      <div className="text-emerald-400 flex size-8 items-center justify-center bg-emerald-400/10 rounded-full">
                        <CheckCircle className="!text-xl" />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div 
                className={`bg-[#251a31]/40 border border-[#3d2a4d] rounded-xl p-8 transition-all hover:bg-[#251a31]/60 ${dragActive ? 'border-[#7f13ec]' : ''}`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
              >
                <div className={`flex flex-col items-center gap-6 rounded-xl border-2 border-dashed ${dragActive ? 'border-[#7f13ec] bg-[#7f13ec]/5' : 'border-[#7f13ec]/40 bg-[#7f13ec]/5'} px-6 py-12`}>
                  <div className="flex flex-col items-center gap-4 text-center">
                    <div className="size-16 bg-[#7f13ec]/20 rounded-full flex items-center justify-center text-[#7f13ec]">
                      <Upload className="!text-4xl" />
                    </div>
                    <div className="flex flex-col gap-1">
                      <p className="text-white text-xl font-bold">Arrastra y suelta tu documento aquí</p>
                      <p className="text-slate-400 text-sm">O haz clic para seleccionar un archivo manualmente</p>
                    </div>
                  </div>
                  <label className="flex min-w-[200px] cursor-pointer items-center justify-center overflow-hidden rounded-full h-12 px-6 bg-[#7f13ec] text-white text-sm font-bold leading-normal tracking-wide transition-transform active:scale-95 shadow-lg shadow-[#7f13ec]/20">
                    <input 
                      type="file" 
                      className="hidden" 
                      accept=".pdf,.jpg,.jpeg,.png"
                      onChange={handleFileSelect}
                    />
                    Seleccionar archivo
                  </label>
                  <p className="text-slate-500 text-xs font-medium uppercase tracking-widest">
                    PDF, JPG, PNG • Máx 5MB
</p>
                </div>
              </div>
            )}
            
            <div className="mt-6">
              <button 
                onClick={handleSubmit}
                disabled={loading || !selectedFile || petitionStatus?.status === 'PENDING' || petitionStatus?.status === 'APPROVED'}
                className="w-full bg-[#7f13ec] hover:bg-[#7f13ec]/90 text-white px-10 py-4 rounded-full font-bold text-lg transition-all transform hover:-translate-y-1 active:scale-95 flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ boxShadow: '0 0 20px 2px rgba(127, 19, 236, 0.3)' }}
              >
                {loading ? (
                  'Enviando...'
                ) : petitionStatus?.status === 'PENDING' ? (
                  <>
                    En revisión
                    <Clock />
                  </>
                ) : petitionStatus?.status === 'APPROVED' ? (
                  <>
                    Aprobado
                    <CheckCircle />
                  </>
                ) : (
                  <>
                    Enviar solicitud
                    <Send />
                  </>
                )}
              </button>
            </div>
          </div>

          <aside className="flex flex-col gap-6">
            <div className="bg-[#251a31] border border-[#3d2a4d] rounded-xl p-6">
              <h3 className="text-white text-lg font-bold leading-tight mb-6 flex items-center gap-2">
                <CheckCircle className="text-[#7f13ec]" />
                Documentos requeridos
              </h3>
              <div className="mb-6">
                <p className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Obligatorios</p>
                <ul className="space-y-3">
                  <li className="flex items-start gap-3">
                    <span className="!text-[10px] mt-1.5 text-slate-600">•</span>
                    <span className="text-slate-300 text-sm">Cédula de ciudadanía / ID</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="!text-[10px] mt-1.5 text-slate-600">•</span>
                    <span className="text-slate-300 text-sm">Pasaporte válido (Extranjeros)</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="!text-[10px] mt-1.5 text-slate-600">•</span>
                    <span className="text-slate-300 text-sm">Documento de identidad oficial</span>
                  </li>
                </ul>
              </div>
              <div className="pt-6 border-t border-[#3d2a4d]">
                <p className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Opcionales</p>
                <ul className="space-y-3">
                  <li className="flex items-start gap-3">
                    <span className="!text-[10px] mt-1.5 text-slate-600">•</span>
                    <span className="text-slate-400 text-sm italic">Certificado de antecedentes</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="!text-[10px] mt-1.5 text-slate-600">•</span>
                    <span className="text-slate-400 text-sm italic">Certificados de cursos</span>
                  </li>
                  <li className="flex items-start gap-3">
                    <span className="!text-[10px] mt-1.5 text-slate-600">•</span>
                    <span className="text-slate-400 text-sm italic">Referencias laborales</span>
                  </li>
                </ul>
              </div>
            </div>
            <div className="bg-[#7f13ec]/10 border border-[#7f13ec]/20 rounded-xl p-5">
              <div className="flex gap-3">
                <Info className="text-[#7f13ec] shrink-0" />
                <p className="text-xs text-slate-300 leading-relaxed">
                  Tu información será procesada en un plazo de 24 a 48 horas hábiles por nuestro equipo de seguridad.
                </p>
              </div>
            </div>
</aside>
        </div>
      </main>

      <Footer />
    </div>
  );
};

export default Organizador;