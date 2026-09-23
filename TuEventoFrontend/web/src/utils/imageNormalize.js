/**
 * normalizeImage — redimensiona una imagen respetando el aspect ratio original.
 *
 * Reglas:
 *  - Solo reduce si supera maxWidth o maxHeight — nunca amplía.
 *  - Sin recorte ni distorsión.
 *  - Convierte siempre a JPEG con la calidad indicada.
 *  - Usa imageSmoothingQuality: 'high' para preservar nitidez al reducir.
 *
 * @param {File}   file      Archivo de imagen original.
 * @param {number} maxWidth  Ancho máximo permitido (default: 1280).
 * @param {number} maxHeight Alto máximo permitido (default: 1280).
 * @param {number} quality   Calidad JPEG 0–1 (default: 0.9).
 * @returns {Promise<File>}  Nuevo File con tipo image/jpeg y dimensiones normalizadas.
 */
export const normalizeImage = (file, maxWidth = 1280, maxHeight = 1280, quality = 0.9) =>
  // Defensive guard: if any numeric param is not a finite number (e.g. an array
  // index injected by Array.map when passed as a bare reference), fall back to
  // the safe defaults. This prevents NaN dimensions and a null blob from toBlob.
  // eslint-disable-next-line no-param-reassign
  new Promise((resolve, reject) => {
    if (typeof maxWidth  !== 'number' || !Number.isFinite(maxWidth))  maxWidth  = 1280;
    if (typeof maxHeight !== 'number' || !Number.isFinite(maxHeight)) maxHeight = 1280;
    if (typeof quality   !== 'number' || !Number.isFinite(quality))   quality   = 0.9;
    const img = new Image();
    const url = URL.createObjectURL(file);

    img.onload = () => {
      const { width, height } = img;

      // Guarda: si el browser cargó la imagen pero no pudo decodificarla
      // (HEIC, AVIF en Safari viejo, etc.) las dimensiones llegan como 0.
      if (!width || !height) {
        URL.revokeObjectURL(url);
        return reject(new Error(
          `No se pudo generar el blob: el navegador no pudo decodificar la imagen (tipo: ${file.type}, dimensiones: ${width}×${height})`,
        ));
      }

      const ratio = Math.min(maxWidth / width, maxHeight / height, 1);
      const targetWidth  = Math.round(width  * ratio);
      const targetHeight = Math.round(height * ratio);

      const canvas = document.createElement('canvas');
      canvas.width  = targetWidth;
      canvas.height = targetHeight;

      const ctx = canvas.getContext('2d');
      ctx.imageSmoothingEnabled = true;
      ctx.imageSmoothingQuality = 'high';
      ctx.drawImage(img, 0, 0, targetWidth, targetHeight);

      // Revocar aquí, no en onload — toBlob es async y revocar antes
      // invalida el bitmap de origen en Chromium/Safari, causando blob null.
      canvas.toBlob(
        (blob) => {
          URL.revokeObjectURL(url);
          if (!blob) return reject(new Error('No se pudo generar el blob'));
          resolve(new File([blob], file.name, { type: 'image/jpeg' }));
        },
        'image/jpeg',
        quality,
      );
    };

    img.onerror = reject;
    img.src = url;
  });
