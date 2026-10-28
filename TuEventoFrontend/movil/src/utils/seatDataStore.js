import { normalizeReservedUntil } from "./seatHold";

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

// Cache de strings hex: hay pocos colores distintos y se piden por cada silla visible
const hexCache = new Map();
function colorToHex(colorInt) {
  let hex = hexCache.get(colorInt);
  if (!hex) {
    hex = '#' + colorInt.toString(16).padStart(6, '0').toUpperCase();
    hexCache.set(colorInt, hex);
  }
  return hex;
}

// Mismo orden que la web (localeCompare): es el que asocia cada silla con su posición
const byCode = (codes) => (a, b) => String(codes[a]).localeCompare(String(codes[b]));

export class SeatDataStore {
  constructor(capacity = 150000) {
    this.capacity = capacity;
    this.count = 0;

    // Arrays de datos (SoA - Structure of Arrays)
    this.seatIds = new Uint32Array(capacity);
    this.statuses = new Uint8Array(capacity);
    this.reservedBy = new Uint32Array(capacity);
    // Uint32: los ids de sección pueden superar 65535
    this.sectionIds = new Uint32Array(capacity);
    this.codes = new Array(capacity);
    this.reservedUntil = new Array(capacity);
    this.colors = new Uint32Array(capacity);

    // Índices para lookups O(1)
    this.seatIdToIndex = new Map();
    this.sectionIndex = new Map();
    this.reservedByIndex = new Map();
    // Índices de cada sección ordenados por código; se invalidan solo al cambiar sus sillas
    this.sortedSectionIndex = new Map();
  }

  /**
   * Carga masiva de sillas (reemplaza todo el contenido)
   */
  loadSeats(seatsArray, currentUserId = null) {
    this.count = 0;
    this.seatIdToIndex.clear();
    this.sectionIndex.clear();
    this.reservedByIndex.clear();
    this.sortedSectionIndex.clear();
    this.upsertSeats(seatsArray, currentUserId);
  }

  /**
   * Agrega sillas nuevas o actualiza las existentes. Permite cargar el evento
   * sección por sección en lugar de todo de una vez.
   *
   * @returns {boolean} true si cambió algo
   */
  upsertSeats(seatsArray, currentUserId = null) {
    if (!seatsArray || seatsArray.length === 0) return false;
    this._ensureCapacity(this.count + seatsArray.length);

    for (const seat of seatsArray) {
      const existing = this.seatIdToIndex.get(seat.seatId);
      if (existing !== undefined) {
        this.updateSeat(seat.seatId, seat, currentUserId);
        continue;
      }

      const i = this.count++;
      this.seatIds[i] = seat.seatId;
      this.statuses[i] = this._statusToEnum(seat.status);
      this.reservedBy[i] = seat.reservedBy || 0;
      this.sectionIds[i] = seat.eventSectionId;
      this.codes[i] = seat.code;
      this.reservedUntil[i] = normalizeReservedUntil(seat.reservedUntil);
      this.colors[i] = this._computeColor(i, currentUserId);

      this.seatIdToIndex.set(seat.seatId, i);

      if (!this.sectionIndex.has(seat.eventSectionId)) {
        this.sectionIndex.set(seat.eventSectionId, []);
      }
      this.sectionIndex.get(seat.eventSectionId).push(i);
      this.sortedSectionIndex.delete(seat.eventSectionId);

      if (seat.reservedBy) {
        if (!this.reservedByIndex.has(seat.reservedBy)) {
          this.reservedByIndex.set(seat.reservedBy, []);
        }
        this.reservedByIndex.get(seat.reservedBy).push(i);
      }
    }
    return true;
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
      this.reservedUntil[idx] = normalizeReservedUntil(updates.reservedUntil);
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
    return this._seatAt(idx);
  }

  /** true si ya hay sillas cargadas de la sección */
  hasSection(sectionId) {
    return this.sectionIndex.has(sectionId);
  }

  /**
   * Índices de las sillas de una sección ordenados por código (cacheado).
   * El orden no depende del estado, así que reservar o liberar no lo invalida.
   */
  getSortedSectionIndices(sectionId) {
    let sorted = this.sortedSectionIndex.get(sectionId);
    if (!sorted) {
      sorted = [...(this.sectionIndex.get(sectionId) || [])].sort(byCode(this.codes));
      this.sortedSectionIndex.set(sectionId, sorted);
    }
    return sorted;
  }

  /** Color de la silla como string hex, listo para Skia */
  colorHexAt(idx) {
    return colorToHex(this.colors[idx]);
  }

  /** true si la silla está reservada por el usuario indicado */
  isReservedBy(idx, userId) {
    return this.statuses[idx] === SeatStatus.RESERVED && !!userId && this.reservedBy[idx] === userId;
  }

  /**
   * Obtiene sillas reservadas por usuario
   */
  getUserReservations(userId) {
    const indices = this.reservedByIndex.get(userId) || [];
    return indices.map((idx) => this._seatAt(idx));
  }

  /**
   * Recalcula todos los colores
   */
  recalculateColors(currentUserId) {
    for (let i = 0; i < this.count; i++) {
      this.colors[i] = this._computeColor(i, currentUserId);
    }
  }

  _seatAt(idx) {
    return {
      seatId: this.seatIds[idx],
      status: StatusFromEnum[this.statuses[idx]],
      reservedBy: this.reservedBy[idx] || null,
      eventSectionId: this.sectionIds[idx],
      code: this.codes[idx],
      reservedUntil: this.reservedUntil[idx],
    };
  }

  /** Duplica la capacidad de los TypedArrays cuando hace falta */
  _ensureCapacity(needed) {
    if (needed <= this.capacity) return;
    let capacity = this.capacity;
    while (capacity < needed) capacity *= 2;

    const grow = (Type, old) => {
      const next = new Type(capacity);
      next.set(old.subarray(0, this.count));
      return next;
    };
    this.seatIds = grow(Uint32Array, this.seatIds);
    this.statuses = grow(Uint8Array, this.statuses);
    this.reservedBy = grow(Uint32Array, this.reservedBy);
    this.sectionIds = grow(Uint32Array, this.sectionIds);
    this.colors = grow(Uint32Array, this.colors);
    this.codes.length = capacity;
    this.reservedUntil.length = capacity;
    this.capacity = capacity;
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
}
