import { useState, useEffect } from 'react';

/**
 * Componente de debug para visualizar el sistema de token refresh
 * 
 * ⚠️ SOLO PARA DESARROLLO - ELIMINAR EN PRODUCCIÓN
 */
export default function TokenRefreshDebug() {
  const [countdown, setCountdown] = useState(15);
  const [lastRefresh, setLastRefresh] = useState(null);
  const [refreshCount, setRefreshCount] = useState(0);
  const [logs, setLogs] = useState([]);

  const addLog = (message, type = 'info') => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs(prev => [{
      timestamp,
      message,
      type
    }, ...prev].slice(0, 10)); // Mantener últimos 10 logs
  };

  // Interceptar console.log para capturar logs de refresh
  useEffect(() => {
    const originalLog = console.log;
    const originalError = console.error;

    console.log = (...args) => {
      originalLog(...args);
      const message = args.join(' ');
      
      if (message.includes('[Token Refresh] Refreshing token')) {
        addLog('🔄 Iniciando refresh...', 'info');
      } else if (message.includes('[Token Refresh] Token refreshed successfully')) {
        setLastRefresh(new Date().toLocaleTimeString());
        setRefreshCount(prev => prev + 1);
        setCountdown(15); // Resetear countdown
        addLog('✅ Token refrescado exitosamente', 'success');
      } else if (message.includes('[WebSocket] Reconnecting')) {
        addLog('🔌 Reconectando WebSocket...', 'info');
      } else if (message.includes('[WebSocket] Reconnected successfully')) {
        addLog('✅ WebSocket reconectado', 'success');
      }
    };

    console.error = (...args) => {
      originalError(...args);
      const message = args.join(' ');
      if (message.includes('[Token Refresh]')) {
        addLog(`❌ Error: ${message}`, 'error');
      }
    };

    return () => {
      console.log = originalLog;
      console.error = originalError;
    };
  }, []);

  // Countdown timer
  useEffect(() => {
    const interval = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          return 15; // Resetear
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  return (
    <div className="fixed bottom-4 right-4 w-80 bg-background border-2 border-primary rounded-lg shadow-2xl p-4 z-50">
      {/* Header */}
      <div className="flex items-center justify-between mb-3 pb-2 border-b border-surfaceAlt">
        <h3 className="text-sm font-bold text-textPrimary">
          🔧 Token Refresh Debug
        </h3>
        <span className="text-xs px-2 py-1 bg-warning/20 text-warning rounded">
          DEV ONLY
        </span>
      </div>

      {/* Countdown */}
      <div className="mb-3">
        <div className="flex items-center justify-between mb-1">
          <span className="text-xs text-textMuted">Próximo refresh:</span>
          <span className={`text-lg font-bold ${
            countdown <= 5 ? 'text-warning' : 'text-accent'
          }`}>
            {countdown}s
          </span>
        </div>
        <div className="h-2 bg-surfaceAlt rounded-full overflow-hidden">
          <div 
            className={`h-full transition-all duration-1000 ${
              countdown <= 5 ? 'bg-warning' : 'bg-accent'
            }`}
            style={{ width: `${(countdown / 15) * 100}%` }}
          />
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-2 mb-3">
        <div className="bg-surfaceAlt rounded p-2">
          <div className="text-xs text-textMuted">Refreshes</div>
          <div className="text-lg font-bold text-accent">{refreshCount}</div>
        </div>
        <div className="bg-surfaceAlt rounded p-2">
          <div className="text-xs text-textMuted">Último</div>
          <div className="text-xs font-mono text-textPrimary">
            {lastRefresh || '--:--:--'}
          </div>
        </div>
      </div>

      {/* Logs */}
      <div className="mb-2">
        <div className="text-xs font-semibold text-textMuted mb-1">Activity Log:</div>
        <div className="bg-surfaceAlt rounded p-2 max-h-32 overflow-y-auto space-y-1">
          {logs.length === 0 ? (
            <div className="text-xs text-textMuted italic">Esperando actividad...</div>
          ) : (
            logs.map((log, i) => (
              <div key={i} className="text-xs">
                <span className="text-textMuted font-mono">{log.timestamp}</span>
                {' '}
                <span className={
                  log.type === 'success' ? 'text-success' :
                  log.type === 'error' ? 'text-error' :
                  'text-textSecondary'
                }>
                  {log.message}
                </span>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Instructions */}
      <div className="text-xs text-textMuted bg-surfaceAlt rounded p-2">
        <strong>Testing:</strong> El token se refresca cada 15s. 
        Abre la consola para ver logs detallados.
      </div>
    </div>
  );
}
