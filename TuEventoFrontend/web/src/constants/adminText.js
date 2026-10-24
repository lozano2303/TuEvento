/**
 * Textos del panel de administración. El panel admin es solo en español (no usa i18n);
 * las páginas de traducciones son la excepción. Misma firma que t() de i18next:
 * adminText('clave.anidada', { variable, defaultValue }) con interpolación {{variable}}.
 */
const TEXT = {
  "adminUsers": {
    "title": "Gestión de usuarios",
    "subtitle": "Busca usuarios y gestiona sus roles. Al quitar el rol de organizador, el usuario recibe el motivo por notificación.",
    "searchPlaceholder": "Buscar por nombre, alias o correo...",
    "loading": "Cargando usuarios...",
    "empty": "No hay usuarios que coincidan con la búsqueda.",
    "roles": {
      "ALL": "Todos",
      "ORGANIZER": "Organizadores",
      "USER": "Usuarios",
      "ADMIN": "Administradores"
    },
    "statuses": {
      "PENDING": "Pendiente",
      "ACTIVE": "Activo",
      "BLOCKED": "Bloqueado",
      "INACTIVE": "Inactivo",
      "DELETED": "Eliminado"
    },
    "columns": {
      "user": "Usuario",
      "email": "Correo",
      "role": "Rol",
      "status": "Estado",
      "joined": "Registro",
      "actions": "Acciones"
    },
    "revokeButton": "Quitar organizador",
    "revokeTitle": "¿Quitar el rol de organizador a {{name}}?",
    "revokeMessage": "La cuenta volverá a ser de usuario normal y ya no podrá crear ni gestionar eventos. Sus eventos publicados se cancelarán (los compradores recibirán el reembolso en su cartera) y los que estén en revisión se rechazarán. Le enviaremos el motivo por notificación y correo.",
    "reasonLabel": "Motivo",
    "reasonPlaceholder": "Explica al usuario por qué se le quita el rol...",
    "reasonMin": "Mínimo {{count}} caracteres",
    "cancel": "Cancelar",
    "revokeConfirm": "Quitar rol",
    "revokeSuccess": "{{name}} ya no es organizador.",
    "revokeError": "No se pudo quitar el rol de organizador."
  },
  "adminRefunds": {
    "title": "Reembolsos",
    "subtitle": "Reembolsos de pagos con su desglose y acreditación manual de saldo a la cartera de un usuario.",
    "refresh": "Actualizar",
    "loading": "Cargando reembolsos...",
    "empty": "No hay reembolsos que coincidan con los filtros.",
    "searchPlaceholder": "Buscar por comprador, correo o evento...",
    "stats": {
      "pending": "Pendientes",
      "pendingSub": "Solicitados, aún sin procesar",
      "processed": "Procesados",
      "processedSub": "Reembolsos completados",
      "total": "Total reembolsado",
      "totalSub": "Monto devuelto en toda la plataforma",
      "toWallet": "Procesado en el listado",
      "toWalletSub": "Suma de los reembolsos procesados mostrados"
    },
    "status": {
      "ALL": "Todos",
      "PROCESSED": "Procesado",
      "REQUESTED": "Solicitado",
      "FAILED": "Fallido"
    },
    "columns": {
      "date": "Fecha",
      "buyer": "Comprador",
      "event": "Evento",
      "amount": "Monto",
      "status": "Estado",
      "reason": "Motivo"
    },
    "processedAt": "Procesado: {{date}}",
    "order": "Orden #{{id}}",
    "gatewayPart": "Pasarela: {{amount}}",
    "walletPart": "Cartera: {{amount}}",
    "credit": {
      "open": "Agregar crédito",
      "title": "Agregar crédito a una cartera",
      "subtitle": "El saldo se suma de inmediato y el usuario recibe una notificación con el motivo.",
      "user": "Usuario",
      "userPlaceholder": "Selecciona un usuario",
      "userSearch": "Buscar por nombre, alias o correo...",
      "amount": "Monto (COP)",
      "reason": "Motivo",
      "reasonPlaceholder": "Ej.: Compensación por inconveniente con el evento",
      "submit": "Acreditar",
      "success": "Se acreditaron {{amount}} a {{name}}.",
      "error": "No se pudo acreditar el saldo."
    }
  }
};

export function adminText(key, vars = {}) {
  const value = key.split('.').reduce((node, k) => (node && typeof node === 'object' ? node[k] : undefined), TEXT);
  if (typeof value !== 'string') return vars.defaultValue ?? key;
  return value.replace(/\{\{(\w+)\}\}/g, (_, name) => (vars[name] ?? ''));
}
