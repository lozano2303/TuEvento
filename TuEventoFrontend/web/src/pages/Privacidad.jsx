import { Link } from 'react-router-dom';
import Footer from '../layouts/Footer';

// ── Helpers compartidos (mismo patrón que TerminosDeUso) ─────────────────────
const H2 = ({ children }) => (
  <h2 className="text-lg font-bold text-textPrimary mt-10 mb-3 flex items-center gap-2">
    <span
      className="inline-block w-1 h-5 rounded-full flex-shrink-0"
      style={{ background: 'var(--color-primary)' }}
    />
    {children}
  </h2>
);

const P = ({ children }) => (
  <p className="text-textSecondary text-sm leading-relaxed mb-4">{children}</p>
);

const Ul = ({ items }) => (
  <ul className="space-y-2 mb-4 pl-2">
    {items.map((item, i) => (
      <li key={i} className="flex items-start gap-2.5 text-sm text-textSecondary leading-relaxed">
        <span className="mt-1.5 w-1.5 h-1.5 rounded-full flex-shrink-0 bg-accent" />
        <span>{item}</span>
      </li>
    ))}
  </ul>
);

// Bloque de sub-grupo dentro de una sección (ej. "Al registrarte:")
const SubGroup = ({ title, items }) => (
  <div className="mb-4">
    <p className="text-sm font-semibold text-textPrimary mb-2">{title}</p>
    <Ul items={items} />
  </div>
);

export default function Privacidad() {
  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans flex flex-col">
      <main className="flex-1 w-full">
        <div className="max-w-3xl mx-auto px-6 py-16">

          {/* Encabezado */}
          <div className="mb-10">
            <h1 className="text-3xl font-black text-textPrimary tracking-tight mb-3">
              Política de Privacidad —{' '}
              <span className="text-accent">Tu Evento</span>
            </h1>
            <p className="text-sm font-medium" style={{ color: 'rgba(196,181,253,0.65)' }}>
              Última actualización: 24 de septiembre de 2026
            </p>
          </div>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt mb-10" />

          {/* Introducción */}
          <P>
            En Tu Evento, desarrollado por el equipo CapySoft, nos comprometemos a proteger tu
            información personal conforme a la Ley 1581 de 2012 y el Decreto 1377 de 2013 de
            Colombia sobre protección de datos personales.
          </P>

          {/* ── Sección 1 ── */}
          <H2>1. Responsable del tratamiento de datos</H2>
          <P>
            <strong className="text-textPrimary font-semibold">Tu Evento</strong> (equipo
            CapySoft) es responsable del tratamiento de los datos personales que recolecta a
            través de su plataforma web y móvil.
          </P>
          <P>
            Correo de contacto para temas de privacidad:{' '}
            <a
              href="mailto:soporte.capysoft@gmail.com"
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              soporte.capysoft@gmail.com
            </a>
          </P>

          {/* ── Sección 2 ── */}
          <H2>2. Datos que recopilamos</H2>
          <P>Recopilamos los siguientes datos según cómo uses la Plataforma:</P>

          <SubGroup
            title="Al registrarte:"
            items={[
              'Correo electrónico y contraseña (almacenada de forma cifrada), o',
              'Datos básicos de tu cuenta de Google o Facebook si te registras mediante ellas (nombre y correo asociado a esa cuenta).',
            ]}
          />
          <SubGroup
            title="En tu perfil (opcionales, los ingresas tú):"
            items={['Nombre.', 'Descripción/biografía.', 'Foto de perfil.']}
          />
          <SubGroup
            title="Al comprar boletas:"
            items={[
              'Historial de compras y boletas adquiridas dentro de la Plataforma.',
              'Datos de pago: procesados directamente por nuestra pasarela de pagos Wompi; Tu Evento no almacena los datos completos de tu tarjeta.',
            ]}
          />
          <SubGroup
            title="Al publicar un evento (organizadores):"
            items={[
              'Información del evento (fecha, lugar, precio, imágenes) y datos de contacto asociados al evento.',
            ]}
          />
          <SubGroup
            title="Datos técnicos:"
            items={[
              'Preferencias de idioma y tema visual dentro de la Plataforma.',
              'Información de sesión necesaria para mantener tu inicio de sesión activo (tokens de autenticación).',
            ]}
          />

          {/* ── Sección 3 ── */}
          <H2>3. Finalidad del tratamiento</H2>
          <P>Usamos tus datos para:</P>
          <Ul items={[
            'Crear y administrar tu cuenta.',
            'Permitirte comprar o vender boletas de eventos.',
            'Generar y validar tus tickets con código QR.',
            'Enviarte notificaciones relacionadas con tus compras o eventos.',
            'Moderar el contenido publicado (fotos de perfil y de eventos) para evitar contenido inapropiado.',
            'Mejorar la seguridad y el funcionamiento de la Plataforma.',
          ]} />
          <P>No usamos tus datos para fines distintos a los aquí descritos sin tu consentimiento.</P>

          {/* ── Sección 4 ── */}
          <H2>4. Almacenamiento y conservación</H2>
          <Ul items={[
            'Las imágenes que subes (foto de perfil, imágenes de eventos) se almacenan en nuestra infraestructura de almacenamiento de archivos mientras tu cuenta o el evento correspondiente permanezcan activos.',
            'Si eliminas tu cuenta o un evento, procederemos a eliminar o anonimizar los datos asociados, salvo aquellos que debamos conservar por obligación legal (por ejemplo, registros de transacciones de pago).',
          ]} />

          {/* ── Sección 5 ── */}
          <H2>5. Con quién compartimos tus datos</H2>
          <Ul items={[
            'Wompi: para el procesamiento de pagos, bajo sus propias políticas de privacidad y seguridad.',
            'Proveedores de autenticación (Google, Facebook): únicamente para validar tu identidad al iniciar sesión con esas cuentas, si eliges usarlas.',
            'No vendemos ni compartimos tus datos personales con terceros para fines publicitarios.',
          ]} />

          {/* ── Sección 6 ── */}
          <H2>6. Tus derechos</H2>
          <P>Como titular de tus datos, en cualquier momento puedes:</P>
          <Ul items={[
            'Solicitar acceso a los datos que tenemos sobre ti.',
            'Solicitar la corrección de datos inexactos.',
            'Solicitar la eliminación de tu cuenta y tus datos personales.',
            'Revocar tu autorización para el tratamiento de tus datos, salvo que exista una obligación legal de conservarlos.',
          ]} />
          <P>
            Puedes ejercer estos derechos desde tu perfil en la Plataforma o escribiendo a{' '}
            <a
              href="mailto:soporte.capysoft@gmail.com"
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              soporte.capysoft@gmail.com
            </a>.
          </P>

          {/* ── Sección 7 ── */}
          <H2>7. Seguridad de la información</H2>
          <P>Implementamos medidas técnicas para proteger tu información, incluyendo:</P>
          <Ul items={[
            'Contraseñas almacenadas de forma cifrada (nunca en texto plano).',
            'Autenticación mediante tokens (JWT) con expiración y renovación segura.',
            'Moderación automática de contenido para prevenir la publicación de imágenes inapropiadas.',
          ]} />
          <P>
            Ningún sistema es 100% infalible; en caso de un incidente de seguridad que afecte
            tus datos, te notificaremos conforme a lo que exige la normativa aplicable.
          </P>

          {/* ── Sección 8 ── */}
          <H2>8. Menores de edad</H2>
          <P>
            La Plataforma no está dirigida a menores de 18 años. Si tienes conocimiento de que
            un menor ha creado una cuenta sin autorización de sus padres o tutores, contáctanos
            para proceder a su eliminación.
          </P>

          {/* ── Sección 9 ── */}
          <H2>9. Cambios a esta política</H2>
          <P>
            Podemos actualizar esta Política de Privacidad. Cualquier cambio relevante será
            notificado dentro de la Plataforma antes de que entre en vigencia.
          </P>

          {/* ── Sección 10 ── */}
          <H2>10. Contacto</H2>
          <P>
            Para ejercer tus derechos o resolver dudas sobre el tratamiento de tus datos:{' '}
            <a
              href="mailto:soporte.capysoft@gmail.com"
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              soporte.capysoft@gmail.com
            </a>
          </P>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt my-10" />

          {/* Disclaimer */}
          <p
            className="text-xs leading-relaxed italic"
            style={{ color: 'rgba(196,181,253,0.55)' }}
          >
            Este documento es un borrador preliminar elaborado para fines del proyecto académico
            "Tu Evento" (SENA). No constituye asesoría legal certificada. Se recomienda revisión
            por un profesional del derecho antes de un despliegue en producción con usuarios reales.
          </p>

          {/* Volver */}
          <div className="mt-8">
            <Link
              to="/"
              className="inline-flex items-center gap-2 text-sm font-medium text-accent hover:text-textPrimary transition-colors"
            >
              ← Volver al inicio
            </Link>
          </div>

        </div>
      </main>
      <Footer />
    </div>
  );
}
