// Extiende app.json. El tráfico HTTP sin cifrar (cleartext) solo se permite fuera de
// producción: las builds de desarrollo/preview apuntan al backend local por http://
// (p. ej. 192.168.1.8), mientras que producción debe usar https://.
module.exports = ({ config }) => ({
  ...config,
  plugins: [
    ...config.plugins,
    [
      "expo-build-properties",
      {
        android: {
          usesCleartextTraffic: process.env.EAS_BUILD_PROFILE !== "production",
        },
      },
    ],
  ],
});
