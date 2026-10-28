/**
 * Detección de que el organizador ya validó el QR que se está mostrando.
 * `mode` es "order" (QR de una orden) o "ticket" (QR de una entrada); `id` el orderId o ticketId.
 */

/** ¿El aviso del WebSocket ({ type, scope, targetId, ticketIds }) corresponde a este QR? */
export function isValidationFor(mode, id, message) {
  if (message?.type !== "QR_VALIDATED") return false;
  const targetId = Number(id);
  if (mode === "order") {
    return message.scope === "ORDER" && Number(message.targetId) === targetId;
  }
  return (message.ticketIds ?? []).some((ticketId) => Number(ticketId) === targetId);
}

/**
 * ¿Los tickets consultados muestran el QR ya validado?
 * - Entrada: el ticket quedó USED.
 * - Orden: alguno quedó USED y ya no le queda ninguno por validar (PAID).
 */
export function isTargetValidated(mode, id, tickets) {
  const list = Array.isArray(tickets) ? tickets : [];
  if (mode === "order") {
    return list.some((t) => t.status === "USED") && !list.some((t) => t.status === "PAID");
  }
  const ticket = list.find((t) => Number(t.ticketId) === Number(id));
  return ticket?.status === "USED";
}
