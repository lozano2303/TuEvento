// Configuración plana de ESLint 9 con las reglas de Expo (React, React Hooks, imports).
// En el CI corre sin bloquear mientras se limpian los avisos del código existente.
const { defineConfig } = require("eslint/config");
const expoConfig = require("eslint-config-expo/flat");

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ["dist/*", "android/*", "ios/*", ".expo/*", "node_modules/*"],
  },
  {
    files: ["tests/**/*.js"],
    languageOptions: {
      globals: Object.fromEntries(
        ["jest", "describe", "it", "expect", "beforeAll", "beforeEach", "afterAll", "afterEach"].map(
          (name) => [name, "readonly"],
        ),
      ),
    },
  },
]);
