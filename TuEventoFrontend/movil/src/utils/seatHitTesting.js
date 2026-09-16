/**
 * Encuentra la silla más cercana a una coordenada de tap/touch.
 * 
 * @param {number} tapX - Coordenada X del tap en pantalla
 * @param {number} tapY - Coordenada Y del tap en pantalla
 * @param {Array} seatPositions - Array de posiciones calculadas por distributeSeats: [{x, y, r}, ...]
 * @param {Array} seats - Array de objetos SeatResponse ordenados igual que seatPositions
 * @param {Object} element - Elemento de sección con propiedades {x, y, ...}
 * @param {number} scale - Factor de escala aplicado al canvas
 * @param {number} offsetX - Offset X del canvas (para centrado)
 * @param {number} offsetY - Offset Y del canvas (para centrado)
 * @returns {Object | null} SeatResponse de la silla tocada, o null si no se tocó ninguna
 */
export function findSeatAt(tapX, tapY, seatPositions, seats, element, scale, offsetX, offsetY) {
  const TOLERANCE = 8; // Píxeles de tolerancia para hacer más fácil tocar con el dedo
  
  for (let i = 0; i < seatPositions.length; i++) {
    const pos = seatPositions[i];
    const seat = seats[i];
    
    if (!seat) continue;
    
    // Transformar posición de la silla al espacio de pantalla
    const seatScreenX = (element.x + pos.x) * scale + offsetX;
    const seatScreenY = (element.y + pos.y) * scale + offsetY;
    const seatRadius = pos.r * scale;
    
    // Calcular distancia del tap al centro de la silla
    const dx = tapX - seatScreenX;
    const dy = tapY - seatScreenY;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    // Si está dentro del radio + tolerancia
    if (distance <= seatRadius + TOLERANCE) {
      return seat;
    }
  }
  
  return null;
}
