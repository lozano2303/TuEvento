import LegalPage from '../components/legal/LegalPage';

/**
 * Eliminación de datos de usuario (/eliminacion-datos). Es la URL que piden Google y Facebook
 * para el inicio de sesión con terceros. El texto vive en las traducciones, clave "dataDeletion".
 */
export default function EliminacionDatos() {
  return <LegalPage translationKey="dataDeletion" />;
}
