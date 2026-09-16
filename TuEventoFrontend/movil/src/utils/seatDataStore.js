/**
 * Data-Oriented Design para manejo eficiente de 100k+ sillas
 * Usa Structures of Arrays (SoA) con TypedArrays para performance
 */

// Enum de estados (para Uint8Array)
export const SeatStatus = {
  AVAILABLE: 0,
  RESERVED: 1,
  SOLD: 2,
  COURTESY: 3,
};

const StatusFromEnum = ['AVAILABLE', 'RESERVED', 'SOLD', 'COURTESY'];

// Colores como enteros para comparación rápida
const Colors = {
  GREEN: 0x10B981,    // AVAILABLE
  BLUE: 0x3B82F6,     // MY_RESERVATION
  YELLOW: 0xFBBF24,   // OTHER_RESERVATION
  GRAY: 0x6B7280,     // SOLD
  PURPLE: 0x8B5CF6,   // COURTESY
};

// Convertir entero a string hex color
function colorToHex(colorInt) {
  return '#' + colorInt.toString(16).padStart(6, '0').toUpperCase();
}

export class SeatDataStore {
  constructor(capacity = 150000) {
    this.capacity = capacity;
    this.count = 0;
    
    // Arrays de datos (SoA - Structure of Arrays)
    this.seatIds = new Uint32Array(capacity);
    this.statuses = new Uint8Array(capacity);
    this.reservedBy = new Uint32Array(capacity);
    this.sectionIds = new Uint16Array(capacity);
    this.codes = new Array(capacity);
    this.reservedUntil = new Array(capacity);
    
    // Datos de renderizado
    this.posX = new Float32Array(capacity);
    this.posY = new Float32Array(capacity);
    this.posR = new Float32Array(capacity);
    this.colors = new Uint32Array(capacity);
    
    // Índices para lookups O(1)
    this.seatIdToIndex = new Map();
    this.sectionIndex = new Map();
    this.reservedByIndex = new Map();
  }

  /**
   * Carga masiva de sillas
   */
  loadSeats(seatsArray, currentUserId = null) {
    const start = performance.now();
    
    this.count = Math.min(seatsArray.length, this.capacity);
    
    // Clear indices
    this.seatIdToIndex.clear();
    this.sectionIndex.clear();
    this.reservedByIndex.clear();
    
    for (let i = 0; i < this.count; i++) {
      const seat = seatsArray[i];
      
      this.seatIds[i] = seat.seatId;
      this.statuses[i] = this._statusToEnum(seat.status);
      this.reservedBy[i] = seat.reservedBy || 0;
      this.sectionIds[i] = seat.eventSectionId;
      this.codes[i] = seat.code;
      this.reservedUntil[i] = seat.reservedUntil;
      
      // Índice seatId -> index
      this.seatIdToIndex.set(seat.seatId, i);
      
      // Índice por sección
      if (!this.sectionIndex.has(seat.eventSectionId)) {
        this.sectionIndex.set(seat.eventSectionId, []);
      }
      this.sectionIndex.get(seat.eventSectionId).push(i);
      
      // Índice por usuario
      if (seat.reservedBy) {
        if (!this.reservedByIndex.has(seat.reservedBy)) {
          this.reservedByIndex.set(seat.reservedBy, []);
        }
        this.reservedByIndex.get(seat.reservedBy).push(i);
      }
    }
    
    // Calcular colores
    this.recalculateColors(currentUserId);
    
    console.log(`[SeatDataStore] Loaded ${this.count} seats in ${(performance.now() - start).toFixed(2)}ms`);
  }

  /**
   * Actualiza una silla (WebSocket o API)
   */
  updateSeat(seatId, updates, currentUserId) {
    const idx = this.seatIdToIndex.get(seatId);
    if (idx === undefined) return false;
    
    let changed = false;
    
    if (updates.status !== undefined) {
      this.statuses[idx] = this._statusToEnum(updates.status);
      changed = true;
    }
    
    if (updates.reservedBy !== undefined) {
      const oldUserId = this.reservedBy[idx];
      const newUserId = updates.reservedBy || 0;
      
      this.reservedBy[idx] = newUserId;
      
      // Actualizar índice
      if (oldUserId && this.reservedByIndex.has(oldUserId)) {
        const arr = this.reservedByIndex.get(oldUserId);
        const i = arr.indexOf(idx);
        if (i > -1) arr.splice(i, 1);
      }
      
      if (newUserId) {
        if (!this.reservedByIndex.has(newUserId)) {
          this.reservedByIndex.set(newUserId, []);
        }
        this.reservedByIndex.get(newUserId).push(idx);
      }
      
      changed = true;
    }
    
    if (updates.reservedUntil !== undefined) {
      this.reservedUntil[idx] = updates.reservedUntil;
      changed = true;
    }
    
    if (changed) {
      this.colors[idx] = this._computeColor(idx, currentUserId);
    }
    
    return changed;
  }

  /**
   * Obtiene silla como objeto (compatibilidad)
   */
  getSeat(seatId) {
    const idx = this.seatIdToIndex.get(seatId);
    if (idx === undefined) return null;
    
    return {
      seatId: this.seatIds[idx],
      status: StatusFromEnum[this.statuses[idx]],
      reservedBy: this.reservedBy[idx] || null,
      eventSectionId: this.sectionIds[idx],
      code: this.codes[idx],
      reservedUntil: this.reservedUntil[idx],
    };
  }

  /**
   * Obtiene todas las sillas como objeto (para compatibilidad)
   */
  getAllSeatsAsObject() {
    const obj = {};
    for (let i = 0; i < this.count; i++) {
      const seatId = this.seatIds[i];
      obj[seatId] = {
        seatId,
        status: StatusFromEnum[this.statuses[i]],
        reservedBy: this.reservedBy[i] || null,
        eventSectionId: this.sectionIds[i],
        code: this.codes[i],
        reservedUntil: this.reservedUntil[i],
      };
    }
    return obj;
  }

  /**
   * Obtiene índices de sillas por sección
   */
  getSectionIndices(sectionId) {
    return this.sectionIndex.get(sectionId) || [];
  }

  /**
   * Obtiene sillas reservadas por usuario
   */
  getUserReservations(userId) {
    const indices = this.reservedByIndex.get(userId) || [];
    return indices.map(idx => this.getSeat(this.seatIds[idx]));
  }

  /**
   * Recalcula todos los colores
   */
  recalculateColors(currentUserId) {
    for (let i = 0; i < this.count; i++) {
      this.colors[i] = this._computeColor(i, currentUserId);
    }
  }

  /**
   * Computa color de una silla
   */
  _computeColor(idx, currentUserId) {
    const status = this.statuses[idx];
    const reservedBy = this.reservedBy[idx];
    
    if (status === SeatStatus.AVAILABLE) return Colors.GREEN;
    if (status === SeatStatus.RESERVED) {
      return reservedBy === currentUserId ? Colors.BLUE : Colors.YELLOW;
    }
    if (status === SeatStatus.SOLD) return Colors.GRAY;
    if (status === SeatStatus.COURTESY) return Colors.PURPLE;
    
    return Colors.GRAY;
  }

  /**
   * Convierte status string a enum
   */
  _statusToEnum(status) {
    switch (status) {
      case 'AVAILABLE': return SeatStatus.AVAILABLE;
      case 'RESERVED': return SeatStatus.RESERVED;
      case 'SOLD': return SeatStatus.SOLD;
      case 'COURTESY': return SeatStatus.COURTESY;
      default: return SeatStatus.AVAILABLE;
    }
  }

  /**
   * Obtiene datos de renderizado por índices (para windowing 10x10)
   */
  getRenderData(indices) {
    const data = {
      positions: [],
      colors: [],
      seatIds: [],
    };
    
    for (const idx of indices) {
      data.positions.push({
        x: this.posX[idx],
        y: this.posY[idx],
        r: this.posR[idx],
      });
      data.colors.push(colorToHex(this.colors[idx]));
      data.seatIds.push(this.seatIds[idx]);
    }
    
    return data;
  }

  /**
   * Asigna posiciones de renderizado a sillas de una sección
   */
  setPositions(sectionId, positions) {
    const indices = this.sectionIndex.get(sectionId) || [];
    
    for (let i = 0; i < Math.min(indices.length, positions.length); i++) {
      const idx = indices[i];
      const pos = positions[i];
      
      this.posX[idx] = pos.x;
      this.posY[idx] = pos.y;
      this.posR[idx] = pos.r;
    }
  }
}
