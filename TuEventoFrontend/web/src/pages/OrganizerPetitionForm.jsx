import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileText, UserCheck, Info, ArrowLeft, Send, Delete, CheckCircle, Bell } from 'lucide-react';

const OrganizerPetitionForm = () => {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "mouseenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "mouseleave" || e.type === "dragleave") {
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

  const handleRemoveFile = () => {
    setSelectedFile(null);
  };

  const handleSubmit = () => {
    if (!selectedFile) {
      alert('Por favor selecciona un archivo');
      return;
    }
    
    setLoading(true);
    // Aquí iría la lógica para enviar el archivo
    setTimeout(() => {
      setLoading(false);
      alert('Solicitud enviada exitosamente');
      navigate('/');
    }, 2000);
  };

  return (
    <div className="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden bg-[#191022] text-slate-100 font-sans">
      
      <div className="flex h-full grow flex-col">

        {/* Main Content */}
        <main className="flex-1 max-w-[1024px] mx-auto w-full px-6 py-10">
          <section className="mb-10">
            <div className="flex flex-col gap-2">
              <h1 className="text-white text-4xl md:text-5xl font-black leading-tight tracking-tight">Solicitud de Organizador</h1>
              <p className="text-slate-400 text-lg max-w-2xl">
                Sube un documento de identidad para verificar tu cuenta y poder crear eventos dentro de la plataforma.
              </p>
            </div>
          </section>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 flex flex-col gap-6">
              {/* Upload Area */}
              <div className="bg-[#251a31]/40 border border-[#3d2a4d] rounded-xl p-8 transition-all hover:bg-[#251a31]/60">
                <div className={`flex flex-col items-center gap-6 rounded-xl border-2 border-dashed ${dragActive ? 'border-[#7f13ec]' : 'border-[#7f13ec]/40'} px-6 py-12 bg-[#7f13ec]/5`}
                     onDragEnter={handleDrag}
                     onDragLeave={handleDrag}
                     onDragOver={handleDrag}
                     onDrop={handleDrop}>
                  <div className="flex flex-col items-center gap-4 text-center">
                    <div className="size-16 bg-[#7f13ec]/20 rounded-full flex items-center justify-center text-[#7f13ec]">
                      <Upload className="w-10 h-10" />
                    </div>
                    <div className="flex flex-col gap-1">
                      <p className="text-white text-xl font-bold">Arrastra y suelta tu documento aquí</p>
                      <p className="text-slate-400 text-sm">O haz clic para seleccionar un archivo manualmente</p>
                    </div>
                  </div>
                  <input 
                    type="file" 
                    className="hidden" 
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={handleFileSelect}
                    id="file-upload"
                  />
                  <label 
                    htmlFor="file-upload"
                    className="flex min-w-[200px] cursor-pointer items-center justify-center overflow-hidden rounded-full h-12 px-6 bg-[#7f13ec] text-white text-sm font-bold leading-normal tracking-wide transition-transform active:scale-95 shadow-lg shadow-[#7f13ec]/20"
                  >
                    Seleccionar archivo
                  </label>
                  <p className="text-slate-500 text-xs font-medium uppercase tracking-widest">
                    PDF, JPG, PNG • Máx 5MB
                  </p>
                </div>
              </div>

              </div>

            {/* Sidebar */}
            <aside className="flex flex-col gap-6">
              <div className="bg-[#251a31] border border-[#3d2a4d] rounded-xl p-6">
                <h3 className="text-white text-lg font-bold leading-tight mb-6 flex items-center gap-2">
                  <UserCheck className="text-[#7f13ec] w-5 h-5" />
                  Documentos requeridos
                </h3>
                <div className="mb-6">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Obligatorios</p>
                  <ul className="space-y-3">
                    <li className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-slate-600 rounded-full mt-1.5"></span>
                      <span className="text-slate-300 text-sm">Cédula de ciudadanía / ID</span>
                    </li>
                    <li className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-slate-600 rounded-full mt-1.5"></span>
                      <span className="text-slate-300 text-sm">Pasaporte válido (Extranjeros)</span>
                    </li>
                    <li className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-slate-600 rounded-full mt-1.5"></span>
                      <span className="text-slate-300 text-sm">Documento de identidad oficial</span>
                    </li>
                  </ul>
                </div>
                <div className="pt-6 border-t border-[#3d2a4d]">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Opcionales</p>
                  <ul className="space-y-3">
                    <li className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-slate-600 rounded-full mt-1.5"></span>
                      <span className="text-slate-400 text-sm italic">Certificado de antecedentes</span>
                    </li>
                    <li className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-slate-600 rounded-full mt-1.5"></span>
                      <span className="text-slate-400 text-sm italic">Certificados de cursos</span>
                    </li>
                    <li className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-slate-600 rounded-full mt-1.5"></span>
                      <span className="text-slate-400 text-sm italic">Referencias laborales</span>
                    </li>
                  </ul>
                </div>
              </div>
              <div className="bg-[#7f13ec]/10 border border-[#7f13ec]/20 rounded-xl p-5">
                <div className="flex gap-3">
                  <Info className="text-[#7f13ec] w-4 h-4" />
                  <p className="text-xs text-slate-300 leading-relaxed">
                    Tu información será procesada en un plazo de 24 a 48 horas hábiles por nuestro equipo de seguridad.
                  </p>
                </div>
              </div>
            </aside>
          </div>

          </main>
      </div>
    </div>
  );
};

export default OrganizerPetitionForm;
