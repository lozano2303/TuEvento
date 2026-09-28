/**
 * LoginIllustration — Ilustración principal del panel izquierdo del login.
 *
 * Representa visualmente la propuesta de Tu Evento:
 * un venue con escenario iluminado, filas de asientos en curva,
 * un ticket con código QR y destellos decorativos.
 *
 * Paleta: violetas y lavandas del tema PRINCIPAL.
 * Estilo: vectorial limpio, sin rellenos de stock.
 */
export default function LoginIllustration({ className }) {
  return (
    <svg
      viewBox="0 0 420 480"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-label="Ilustración de plataforma de eventos"
      role="img"
      className={className ?? "w-full max-w-md drop-shadow-2xl"}
    >
      <defs>
        {/* Gradientes de fondo del venue */}
        <radialGradient id="stageGlow" cx="50%" cy="100%" r="70%">
          <stop offset="0%" stopColor="#A78BFA" stopOpacity="0.35" />
          <stop offset="100%" stopColor="#1E0A3C" stopOpacity="0" />
        </radialGradient>

        <linearGradient id="stageSurface" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#7C3AED" />
          <stop offset="100%" stopColor="#4C1D95" />
        </linearGradient>

        <linearGradient id="stageFloor" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#5B21B6" stopOpacity="0.6" />
          <stop offset="100%" stopColor="#2E1065" stopOpacity="0.9" />
        </linearGradient>

        <linearGradient id="curtainLeft" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#4C1D95" />
          <stop offset="100%" stopColor="#6D28D9" stopOpacity="0.5" />
        </linearGradient>

        <linearGradient id="curtainRight" x1="1" y1="0" x2="0" y2="0">
          <stop offset="0%" stopColor="#4C1D95" />
          <stop offset="100%" stopColor="#6D28D9" stopOpacity="0.5" />
        </linearGradient>

        <linearGradient id="ticketGrad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#7C3AED" />
          <stop offset="100%" stopColor="#5B21B6" />
        </linearGradient>

        <linearGradient id="seatRow1" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#6D28D9" />
          <stop offset="100%" stopColor="#4C1D95" />
        </linearGradient>

        <linearGradient id="seatRow2" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#5B21B6" />
          <stop offset="100%" stopColor="#3B0764" />
        </linearGradient>

        <linearGradient id="spotlightL" x1="0.5" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#DDD6FE" stopOpacity="0.5" />
          <stop offset="100%" stopColor="#A78BFA" stopOpacity="0" />
        </linearGradient>

        <linearGradient id="spotlightR" x1="0.5" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#DDD6FE" stopOpacity="0.5" />
          <stop offset="100%" stopColor="#A78BFA" stopOpacity="0" />
        </linearGradient>

        <filter id="glow" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="4" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        <filter id="softGlow" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="2.5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        <filter id="ticketShadow" x="-15%" y="-15%" width="130%" height="130%">
          <feDropShadow dx="0" dy="6" stdDeviation="8" floodColor="#4C1D95" floodOpacity="0.7" />
        </filter>

        <clipPath id="stageClip">
          <rect x="80" y="210" width="260" height="80" rx="4" />
        </clipPath>
      </defs>

      {/* ── Resplandor de fondo del escenario ───────────────────────────────── */}
      <ellipse cx="210" cy="420" rx="200" ry="80" fill="url(#stageGlow)" />

      {/* ══════════════════════════════════════════════════════════════════════
          VENUE — paredes laterales y arco del proscenio
      ══════════════════════════════════════════════════════════════════════ */}

      {/* Arco exterior del proscenio */}
      <path
        d="M 50 290 Q 210 160 370 290"
        stroke="#6D28D9"
        strokeWidth="2"
        fill="none"
        opacity="0.4"
      />

      {/* Cortinas — izquierda */}
      <path
        d="M 80 170 Q 95 230 88 290 L 110 290 Q 118 225 108 170 Z"
        fill="url(#curtainLeft)"
        opacity="0.9"
      />
      {/* Pliegue cortina izquierda */}
      <path
        d="M 92 175 Q 100 235 94 290"
        stroke="#A78BFA"
        strokeWidth="1"
        opacity="0.3"
        fill="none"
      />

      {/* Cortinas — derecha */}
      <path
        d="M 340 170 Q 325 230 332 290 L 310 290 Q 302 225 312 170 Z"
        fill="url(#curtainRight)"
        opacity="0.9"
      />
      {/* Pliegue cortina derecha */}
      <path
        d="M 328 175 Q 320 235 326 290"
        stroke="#A78BFA"
        strokeWidth="1"
        opacity="0.3"
        fill="none"
      />

      {/* Cenefa superior del proscenio */}
      <path
        d="M 78 168 Q 210 145 342 168 L 342 178 Q 210 155 78 178 Z"
        fill="#4C1D95"
        stroke="#7C3AED"
        strokeWidth="1"
      />
      {/* Detalle de franja dorada en la cenefa */}
      <path
        d="M 82 172 Q 210 150 338 172"
        stroke="#A78BFA"
        strokeWidth="1.5"
        opacity="0.7"
        fill="none"
      />

      {/* ══════════════════════════════════════════════════════════════════════
          FOCOS DE ESCENARIO
      ══════════════════════════════════════════════════════════════════════ */}

      {/* Spotlight izquierdo */}
      <polygon
        points="120,178 88,290 152,290"
        fill="url(#spotlightL)"
        opacity="0.45"
      />
      {/* Spotlight derecho */}
      <polygon
        points="300,178 268,290 332,290"
        fill="url(#spotlightR)"
        opacity="0.45"
      />
      {/* Spotlight central */}
      <polygon
        points="210,165 178,290 242,290"
        fill="url(#spotlightL)"
        opacity="0.25"
      />

      {/* Focos físicos — izquierdo */}
      <g filter="url(#softGlow)">
        <rect x="112" y="162" width="16" height="10" rx="3" fill="#6D28D9" />
        <circle cx="120" cy="162" r="4" fill="#DDD6FE" opacity="0.9" />
      </g>
      {/* Focos físicos — derecho */}
      <g filter="url(#softGlow)">
        <rect x="292" y="162" width="16" height="10" rx="3" fill="#6D28D9" />
        <circle cx="300" cy="162" r="4" fill="#DDD6FE" opacity="0.9" />
      </g>
      {/* Foco central */}
      <g filter="url(#softGlow)">
        <rect x="202" y="156" width="16" height="10" rx="3" fill="#6D28D9" />
        <circle cx="210" cy="156" r="4" fill="#EDE9FE" opacity="0.85" />
      </g>

      {/* ══════════════════════════════════════════════════════════════════════
          ESCENARIO
      ══════════════════════════════════════════════════════════════════════ */}

      {/* Piso del escenario */}
      <rect x="80" y="277" width="260" height="60" rx="3" fill="url(#stageFloor)" />
      {/* Borde frontal del escenario (luz de borde) */}
      <rect x="80" y="277" width="260" height="4" rx="2" fill="#7C3AED" opacity="0.8" />

      {/* Superficie principal del escenario con reflejo */}
      <rect x="88" y="281" width="244" height="52" rx="2" fill="url(#stageSurface)" opacity="0.35" />

      {/* Figura estilizada — artista/presentador en escena */}
      {/* Cuerpo */}
      <ellipse cx="210" cy="272" rx="8" ry="14" fill="#DDD6FE" opacity="0.95" />
      {/* Cabeza */}
      <circle cx="210" cy="255" r="7" fill="#DDD6FE" opacity="0.95" />
      {/* Brazo izquierdo levantado */}
      <path d="M202 266 Q192 258 187 250" stroke="#DDD6FE" strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.9" />
      {/* Brazo derecho */}
      <path d="M218 266 Q226 268 232 263" stroke="#DDD6FE" strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.9" />
      {/* Micrófono */}
      <line x1="232" y1="263" x2="236" y2="258" stroke="#A78BFA" strokeWidth="2" strokeLinecap="round" />
      <circle cx="237" cy="257" r="3" fill="#A78BFA" filter="url(#softGlow)" />
      {/* Piernas */}
      <path d="M205 286 Q204 296 202 302" stroke="#DDD6FE" strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.9" />
      <path d="M215 286 Q216 296 218 302" stroke="#DDD6FE" strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.9" />

      {/* Resplandor debajo del artista */}
      <ellipse cx="210" cy="302" rx="18" ry="4" fill="#A78BFA" opacity="0.25" />

      {/* Destellos de escenario */}
      {[
        { cx: 105, cy: 290 },
        { cx: 160, cy: 285 },
        { cx: 260, cy: 285 },
        { cx: 315, cy: 290 },
      ].map((p, i) => (
        <circle
          key={i}
          cx={p.cx}
          cy={p.cy}
          r="3"
          fill="#EDE9FE"
          opacity="0.5"
          filter="url(#softGlow)"
        />
      ))}

      {/* ══════════════════════════════════════════════════════════════════════
          FILAS DE ASIENTOS — semicírculo con perspectiva
      ══════════════════════════════════════════════════════════════════════ */}

      {/* Fila 1 — más cercana al escenario */}
      {[130, 152, 174, 196, 218, 240, 262, 284].map((x, i) => (
        <g key={`r1-${i}`}>
          <rect
            x={x - 7}
            y={342}
            width={13}
            height={12}
            rx="2.5"
            fill="#8B5CF6"
            stroke="#A78BFA"
            strokeWidth="0.9"
            opacity={i === 3 || i === 4 ? 0.45 : 0.92}
          />
          {/* respaldo */}
          <rect x={x - 6} y={338} width={11} height={6} rx="2" fill="#6D28D9" opacity={i === 3 || i === 4 ? 0.35 : 0.82} />
        </g>
      ))}

      {/* Fila 2 — intermedia */}
      {[110, 133, 157, 180, 204, 228, 251, 274, 298].map((x, i) => (
        <g key={`r2-${i}`}>
          <rect
            x={x - 8}
            y={368}
            width={14}
            height={13}
            rx="2.5"
            fill="#7C3AED"
            stroke="#A78BFA"
            strokeWidth="0.9"
            opacity={0.88}
          />
          <rect x={x - 7} y={364} width={12} height={6} rx="2" fill="#6D28D9" opacity="0.78" />
        </g>
      ))}

      {/* Fila 3 — más lejana */}
      {[88, 113, 138, 163, 188, 213, 238, 263, 288, 313].map((x, i) => (
        <g key={`r3-${i}`}>
          <rect
            x={x - 9}
            y={396}
            width={16}
            height={14}
            rx="3"
            fill="#6D28D9"
            stroke="#A78BFA"
            strokeWidth="0.8"
            opacity={0.82}
          />
          <rect x={x - 8} y={392} width={14} height={6} rx="2.5" fill="#5B21B6" opacity="0.72" />
        </g>
      ))}

      {/* Fila 4 — última fila */}
      {[70, 97, 124, 151, 178, 205, 232, 259, 286, 313, 340].map((x, i) => (
        <rect
          key={`r4-${i}`}
          x={x - 10}
          y={423}
          width={17}
          height={14}
          rx="3"
          fill="#5B21B6"
          stroke="#7C3AED"
          strokeWidth="0.7"
          opacity="0.65"
        />
      ))}

      {/* Pasillo central entre filas 2 y 3 */}
      <line
        x1="210"
        y1="334"
        x2="210"
        y2="420"
        stroke="#A78BFA"
        strokeWidth="0.8"
        strokeDasharray="3 4"
        opacity="0.2"
      />

      {/* ══════════════════════════════════════════════════════════════════════
          TICKET FLOTANTE — integrado sobre el escenario, centrado horizontalmente
          Anclaje de rotación: centro del ticket (210, 128)
      ══════════════════════════════════════════════════════════════════════ */}
      <g filter="url(#ticketShadow)" transform="rotate(-6, 210, 128)">
        {/* Cuerpo del ticket — centrado: x=150, ancho=120 → centro en x=210 */}
        <rect x="150" y="92" width="120" height="72" rx="8" fill="url(#ticketGrad)" />

        {/* Borde decorativo */}
        <rect x="150" y="92" width="120" height="72" rx="8" fill="none" stroke="#A78BFA" strokeWidth="1.5" opacity="0.65" />

        {/* Perforaciones izquierda y derecha */}
        <circle cx="150" cy="128" r="6" fill="#1E0A3C" />
        <circle cx="270" cy="128" r="6" fill="#1E0A3C" />

        {/* Línea divisoria punteada */}
        <line x1="164" y1="128" x2="256" y2="128" stroke="#A78BFA" strokeWidth="1" strokeDasharray="4 3" opacity="0.55" />

        {/* Nombre del evento */}
        <text x="164" y="112" fontFamily="system-ui, sans-serif" fontSize="8" fontWeight="700" fill="#F5F3FF" letterSpacing="1.2">TU EVENTO</text>

        {/* Subtexto */}
        <text x="164" y="124" fontFamily="system-ui, sans-serif" fontSize="6" fill="#C4B5FD" opacity="0.9">Entrada General · Fila A</text>

        {/* Mini QR (patrón simplificado) */}
        <rect x="236" y="99" width="26" height="26" rx="2" fill="#EDE9FE" opacity="0.12" />
        {/* Esquinas del QR */}
        <rect x="238" y="101" width="8" height="8" rx="1" fill="none" stroke="#EDE9FE" strokeWidth="1.3" opacity="0.85" />
        <rect x="252" y="101" width="8" height="8" rx="1" fill="none" stroke="#EDE9FE" strokeWidth="1.3" opacity="0.85" />
        <rect x="238" y="115" width="8" height="8" rx="1" fill="none" stroke="#EDE9FE" strokeWidth="1.3" opacity="0.85" />
        {/* Puntos QR internos */}
        <rect x="240" y="103" width="4" height="4" rx="0.5" fill="#EDE9FE" opacity="0.75" />
        <rect x="254" y="103" width="4" height="4" rx="0.5" fill="#EDE9FE" opacity="0.75" />
        <rect x="240" y="117" width="4" height="4" rx="0.5" fill="#EDE9FE" opacity="0.75" />
        {/* Puntos de datos QR */}
        <rect x="248" y="110" width="2.5" height="2.5" fill="#EDE9FE" opacity="0.55" />
        <rect x="252" y="113" width="2.5" height="2.5" fill="#EDE9FE" opacity="0.55" />
        <rect x="256" y="109" width="2.5" height="2.5" fill="#EDE9FE" opacity="0.55" />

        {/* Número del ticket */}
        <text x="164" y="150" fontFamily="monospace, system-ui" fontSize="6.5" fill="#A78BFA" letterSpacing="1.5" opacity="0.85">#00247 · 20:00 HS</text>
      </g>

      {/* ══════════════════════════════════════════════════════════════════════
          DESTELLOS Y PARTÍCULAS DECORATIVAS
      ══════════════════════════════════════════════════════════════════════ */}

      {/* Estrella de 4 puntas — arriba izquierda */}
      <g filter="url(#glow)">
        <path d="M 65 80 L 68 72 L 71 80 L 79 83 L 71 86 L 68 94 L 65 86 L 57 83 Z" fill="#A78BFA" opacity="0.7" />
      </g>

      {/* Estrella pequeña */}
      <g filter="url(#softGlow)">
        <path d="M 48 135 L 50 130 L 52 135 L 57 137 L 52 139 L 50 144 L 48 139 L 43 137 Z" fill="#DDD6FE" opacity="0.5" />
      </g>

      {/* Estrella grande arriba centro-derecha */}
      <g filter="url(#glow)">
        <path d="M 355 50 L 359 40 L 363 50 L 373 54 L 363 58 L 359 68 L 355 58 L 345 54 Z" fill="#EDE9FE" opacity="0.65" />
      </g>

      {/* Puntos brillantes dispersos */}
      {[
        { cx: 38, cy: 200, r: 2.5, op: 0.4 },
        { cx: 55, cy: 265, r: 1.8, op: 0.35 },
        { cx: 380, cy: 160, r: 2, op: 0.45 },
        { cx: 395, cy: 220, r: 1.5, op: 0.3 },
        { cx: 378, cy: 330, r: 2.2, op: 0.4 },
        { cx: 42, cy: 370, r: 1.8, op: 0.3 },
        { cx: 140, cy: 50, r: 2, op: 0.4 },
        { cx: 72, cy: 48, r: 1.5, op: 0.35 },
        { cx: 370, cy: 110, r: 1.8, op: 0.4 },
        { cx: 250, cy: 42, r: 2.2, op: 0.5 },
      ].map((p, i) => (
        <circle
          key={`dot-${i}`}
          cx={p.cx}
          cy={p.cy}
          r={p.r}
          fill="#C4B5FD"
          opacity={p.op}
        />
      ))}

      {/* Anillos de onda / nota musical estilizada */}
      <g opacity="0.18">
        <ellipse cx="80" cy="320" rx="14" ry="6" stroke="#A78BFA" strokeWidth="1.2" fill="none" />
        <ellipse cx="80" cy="320" rx="22" ry="9" stroke="#A78BFA" strokeWidth="0.8" fill="none" />
      </g>
      <g opacity="0.18">
        <ellipse cx="340" cy="320" rx="14" ry="6" stroke="#A78BFA" strokeWidth="1.2" fill="none" />
        <ellipse cx="340" cy="320" rx="22" ry="9" stroke="#A78BFA" strokeWidth="0.8" fill="none" />
      </g>

      {/* ══════════════════════════════════════════════════════════════════════
          TEXTO DE MARCA — parte inferior
      ══════════════════════════════════════════════════════════════════════ */}
      <text
        x="210"
        y="462"
        textAnchor="middle"
        fontFamily="system-ui, -apple-system, sans-serif"
        fontSize="20"
        fontWeight="700"
        fill="#F5F3FF"
        letterSpacing="3"
        opacity="1"
      >
        TU EVENTO
      </text>
      <text
        x="210"
        y="478"
        textAnchor="middle"
        fontFamily="system-ui, -apple-system, sans-serif"
        fontSize="11"
        fill="#F5F3FF"
        letterSpacing="2"
        opacity="0.85"
      >
        VIVE LA EXPERIENCIA
      </text>
    </svg>
  );
}
