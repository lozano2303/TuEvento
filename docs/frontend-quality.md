# Calidad del frontend (web y móvil)

Ambas apps tienen ESLint 9 (configuración plana), Prettier y tests. Se ejecutan igual en `TuEventoFrontend/web` y en `TuEventoFrontend/movil`:

| Comando | Qué hace |
|---|---|
| `npm run lint` | ESLint sobre todo el proyecto |
| `npm run format:check` / `npm run format` | Comprueba / aplica el formato de Prettier |
| `npm test` | Tests: Vitest + React Testing Library (web), Jest con `jest-expo` + React Native Testing Library (móvil) |

## Dónde van los tests

En `tests/` de cada app, fuera de `src/`. Sonar analiza `src/` (duplicación incluida), así que los tests espejados entre web y móvil no cuentan como código duplicado. Los imports apuntan a `../../src/...`.

- Web: `tests/setup.js` carga los matchers de `@testing-library/jest-dom`.
- Móvil: `tests/setup.js` sustituye AsyncStorage por su mock en memoria. En RNTL 14 `render` es asíncrono: `await render(...)`.

## Estado del lint y plan para endurecerlo

En el CI, `npm run lint` corre con `continue-on-error`: el código existente todavía tiene avisos (al introducirlo, web 48 errores / 114 avisos, móvil 22 / 54). Los tests sí bloquean.

1. Corregir los errores, empezando por `react-hooks/rules-of-hooks` (hooks llamados condicionalmente: hoy en `ThemeCustomizePanel.jsx` y `EventLayoutEditor.jsx` de la web), que pueden provocar bugs reales.
2. Quitar `continue-on-error` del paso de lint cuando no queden errores.
3. Aplicar `npm run format` en un PR propio (solo formato) y añadir `npm run format:check` al CI.

## Próximos tests

Después de los helpers puros (`notificationI18n`, `eventCancellation`, `errorMessages`), seguir con el flujo de pago y el de autenticación, incluido el refresh de `services/httpClient.js`.
