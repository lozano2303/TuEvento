// Prueba de carrera: N usuarios intentan reservar LA MISMA silla en el mismo instante.
// Resultado correcto: exactamente 1 respuesta 200 y el resto 400 SEAT_NOT_AVAILABLE.
//
// Variables (las pasa run.sh):
//   BASE_URL   URL del backend (por defecto http://localhost:8080)
//   SEAT_ID    silla objetivo (debe estar AVAILABLE)
//   USER_IDS   ids de app_user separados por coma; un VU por usuario
//   JWT_SECRET secreto con el que el backend valida los tokens (app.jwt.secret)
import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SEAT_ID = __ENV.SEAT_ID;
const USER_IDS = (__ENV.USER_IDS || '').split(',').filter(Boolean).map(Number);
const JWT_SECRET = __ENV.JWT_SECRET;

if (!SEAT_ID || USER_IDS.length === 0 || !JWT_SECRET) {
  throw new Error('Faltan SEAT_ID, USER_IDS o JWT_SECRET');
}

const reservasExitosas = new Counter('reservas_exitosas');
const rechazosEsperados = new Counter('rechazos_esperados');
const erroresInesperados = new Counter('errores_inesperados');

export const options = {
  scenarios: {
    carrera: {
      executor: 'per-vu-iterations',
      vus: USER_IDS.length,
      iterations: 1,
      maxDuration: '60s',
    },
  },
  thresholds: {
    reservas_exitosas: ['count==1'],
    errores_inesperados: ['count==0'],
  },
};

// Firma un access token HS256 con los mismos claims que JwtTokenGenerator
function firmarToken(userId) {
  const ahora = Math.floor(Date.now() / 1000);
  const header = encoding.b64encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }), 'rawurl');
  const payload = encoding.b64encode(JSON.stringify({
    jti: `loadtest-${userId}-${ahora}`,
    sub: `loadtest_${userId}`,
    userId,
    role: 'USER',
    iat: ahora,
    exp: ahora + 600,
  }), 'rawurl');
  const firma = encoding.b64encode(
    crypto.hmac('sha256', JWT_SECRET, `${header}.${payload}`, 'binary'), 'rawurl');
  return `${header}.${payload}.${firma}`;
}

export function setup() {
  // Todos los VUs esperan hasta este instante para disparar a la vez
  return {
    disparoEn: Date.now() + 3000,
    tokens: USER_IDS.map(firmarToken),
  };
}

export default function (data) {
  const indice = __VU - 1;
  const espera = (data.disparoEn - Date.now()) / 1000;
  if (espera > 0) sleep(espera);

  const res = http.post(`${BASE_URL}/api/v1/seats/${SEAT_ID}/reserve`, null, {
    headers: { Authorization: `Bearer ${data.tokens[indice]}` },
    tags: { name: 'reserve' },
  });

  let codigo = '';
  try { codigo = res.json('code') || ''; } catch (_) { /* cuerpo no JSON */ }

  if (res.status === 200) {
    reservasExitosas.add(1);
    console.log(`userId=${USER_IDS[indice]} -> 200 RESERVADA`);
  } else if (res.status === 400 && codigo === 'SEAT_NOT_AVAILABLE') {
    rechazosEsperados.add(1);
  } else {
    erroresInesperados.add(1);
    console.warn(`userId=${USER_IDS[indice]} -> ${res.status} ${codigo} ${res.body}`);
  }
}
