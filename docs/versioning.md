# Versionado

TuEvento sigue [Semantic Versioning 2.0.0](https://semver.org/lang/es/): `MAJOR.MINOR.PATCH`.

| Se incrementa | Cuándo | Ejemplo |
|---|---|---|
| **MAJOR** | Cambio incompatible para los clientes: se quita o renombra un endpoint o un campo de la API, cambia el formato de `ApiResponse`, una migración obliga a intervenir datos a mano | `1.4.2` → `2.0.0` |
| **MINOR** | Funcionalidad nueva compatible: endpoint, pantalla, campo opcional, idioma | `1.4.2` → `1.5.0` |
| **PATCH** | Corrección o cambio interno sin efecto en el contrato: bugfix, seguridad, rendimiento, dependencias | `1.4.2` → `1.4.3` |

## Una sola versión para todo el monorepo

Backend y web se publican juntos y llevan la misma versión. La app móvil la adopta cuando sale una build nueva de EAS con esos cambios; mientras tanto conserva la de su última build (por eso `v1.0.1` no cambió la versión de la app):

| Componente | Dónde vive la versión |
|---|---|
| Backend | `TuEventoBackend/tu-evento/pom.xml` (`<version>`) |
| Web | `TuEventoFrontend/web/package.json` (`version`) |
| Móvil | `TuEventoFrontend/movil/package.json` y `app.json` (`expo.version`); el `versionCode`/`buildNumber` lo incrementa EAS (`appVersionSource: remote`) |
| Imágenes Docker | etiqueta `vX.Y.Z` (ver `docs/deployment-vps.md`) |

`fake-payment-gateway` y `nsfw-service` son herramientas de apoyo y versionan por su cuenta.

## Cómo publicar una versión

1. En `develop`, mover las entradas de `## [Unreleased]` del `CHANGELOG.md` a una sección `## [X.Y.Z] - AAAA-MM-DD` y subir la versión en los archivos de la tabla.
2. PR de `develop` a `main`.
3. Sobre el merge en `main`, crear el tag anotado y subirlo:

   ```bash
   git checkout main && git pull
   git tag -a vX.Y.Z -m "TuEvento X.Y.Z"
   git push origin vX.Y.Z
   ```

4. Construir y publicar las imágenes `vX.Y.Z` (y el build de EAS si cambió la app móvil).

Entre versiones, cada PR añade su entrada en `## [Unreleased]`.
