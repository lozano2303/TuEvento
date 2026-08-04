import { useState, useRef } from 'react';
import { Upload, X, ImagePlus } from 'lucide-react';
import { uploadEventMedia } from '../../services/EventMediaService';

/**
 * StepEventMedia — Paso 3 del wizard de creación de evento.
 * Permite seleccionar imágenes en local (con preview), quitarlas antes de subir,
 * y subirlas todas al confirmar. Las imágenes son opcionales — "Omitir" salta al editor.
 *
 * Props:
 *   createdEventId — Long: ID del evento ya creado en el paso 2
 *   onFinish       — () => void: navega al editor de layout
 */
export default function StepEventMedia({ createdEventId, onFinish }) {
  const [files,        setFiles]        = useState([]); // { file: File, preview: string }[]
  const [isUploading,  setIsUploading]  = useState(false);
  const [uploadError,  setUploadError]  = useState(null);
  const inputRef = useRef(null);

  const handleFileSelect = (e) => {
    const selected = Array.from(e.target.files ?? []).map((file) => ({
      file,
      preview: URL.createObjectURL(file),
    }));
    setFiles((prev) => [...prev, ...selected]);
    // Reset input so the same file can be re-seleccionado si se quitó
    if (inputRef.current) inputRef.current.value = '';
  };

  const handleRemove = (idx) => {
    setFiles((prev) => {
      URL.revokeObjectURL(prev[idx].preview); // liberar memoria
      return prev.filter((_, i) => i !== idx);
    });
  };

  const handleUploadAndFinish = async () => {
    if (files.length === 0) { onFinish(); return; }
    setIsUploading(true);
    setUploadError(null);
    try {
      for (const { file } of files) {
        await uploadEventMedia(createdEventId, file);
      }
      onFinish();
    } catch (err) {
      setUploadError(
        `Algunas imágenes no se pudieron subir: ${err.message}. Puedes agregarlas después desde "Mis Eventos".`
      );
    } finally {
      setIsUploading(false);
    }
  };

  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors';

  return (
    <div className="space-y-5 mt-6">
      <div>
        <p className="text-sm text-textSecondary">
          Agrega fotos para que los asistentes conozcan el evento. Puedes subir varias imágenes.
        </p>
        <p className="text-xs text-textMuted mt-1">
          Este paso es opcional — puedes omitirlo y agregar imágenes más tarde desde "Mis Eventos".
        </p>
      </div>

      {/* Input de archivos */}
      <div>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          multiple
          id="media-upload"
          className="hidden"
          onChange={handleFileSelect}
        />
        <label
          htmlFor="media-upload"
          className="flex items-center justify-center gap-2 w-full py-3 rounded-xl border-2 border-dashed cursor-pointer transition-colors border-surfaceAlt hover:border-accent text-textMuted hover:text-accent text-sm"
        >
          <ImagePlus className="w-4 h-4" />
          Seleccionar imágenes
        </label>
      </div>

      {/* Grid de previews */}
      {files.length > 0 && (
        <div className="grid grid-cols-3 gap-2">
          {files.map(({ preview }, idx) => (
            <div key={idx} className="relative rounded-lg overflow-hidden aspect-square">
              <img src={preview} alt={`Preview ${idx + 1}`} className="w-full h-full object-cover" />
              <button
                onClick={() => handleRemove(idx)}
                className="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/60 flex items-center justify-center text-white hover:bg-error transition-colors"
                title="Quitar imagen"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Error de subida */}
      {uploadError && (
        <div className="text-xs text-error bg-error/10 border border-error/30 rounded-xl px-3 py-2">
          {uploadError}
        </div>
      )}

      {/* Acciones */}
      <div className="flex gap-3 pt-2">
        <button
          onClick={onFinish}
          disabled={isUploading}
          className="flex-1 py-2.5 rounded-xl text-sm font-medium text-textSecondary bg-surfaceAlt hover:bg-surfaceAlt/80 transition-colors disabled:opacity-50"
        >
          Omitir por ahora
        </button>
        <button
          onClick={handleUploadAndFinish}
          disabled={isUploading || files.length === 0}
          className="flex-1 py-2.5 rounded-xl text-sm font-medium bg-primary text-white hover:bg-primaryDark transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {isUploading ? (
            <>
              <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              Subiendo…
            </>
          ) : (
            <>
              <Upload className="w-4 h-4" />
              Subir y continuar
            </>
          )}
        </button>
      </div>
    </div>
  );
}
