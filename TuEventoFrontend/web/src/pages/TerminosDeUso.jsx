import { Link } from 'react-router-dom';
import Footer from '../layouts/Footer';

// ── Helpers de tipografía para documentos legales ─────────────────────────────
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

const Strong = ({ children }) => (
  <strong className="text-textPrimary font-semibold">{children}</strong>
);

export default function TerminosDeUso() {
  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans flex flex-col">
      <main className="flex-1 w-full">
        <div className="max-w-3xl mx-auto px-6 py-16">

          {/* Encabezado */}
          <div className="mb-10">
            <h1 className="text-3xl font-black text-textPrimary tracking-tight mb-3">
              Términos de Uso —{' '}
              <span className="text-accent">Tu Evento</span>
            </h1>
            <p className="text-sm font-medium" style={{ color: 'rgba(196,181,253,0.65)' }}>
              Última actualización: 24 de septiembre de 2026
            </p>
          </div>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt mb-10" />

          {/* ── Sección 1 ── */}
          <H2>1. Aceptación de los términos</H2>
          <P>
            Al registrarte y usar la plataforma "Tu Evento" (en adelante, "la Plataforma"),
            operada por el equipo CapySoft, aceptas los presentes Términos de Uso. Si no estás
            de acuerdo con alguno de estos puntos, te pedimos no utilizar la Plataforma.
          </P>
          <P>
            Tu Evento es un proyecto desarrollado en el marco de una formación del SENA
            (Servicio Nacional de Aprendizaje), actualmente disponible para usuarios en Colombia.
          </P>

          {/* ── Sección 2 ── */}
          <H2>2. Descripción del servicio</H2>
          <P>Tu Evento es una plataforma web y móvil que permite:</P>
          <Ul items={[
            'A organizadores: crear, publicar y administrar eventos, definir precios, zonas y disponibilidad de boletas, y gestionar la venta de las mismas.',
            'A asistentes: explorar eventos, comprar boletas, recibir su ticket digital con código QR, y validar su ingreso al evento mediante ese código.',
          ]} />

          {/* ── Sección 3 ── */}
          <H2>3. Registro de cuenta</H2>
          <P>
            Para usar ciertas funciones de la Plataforma debes crear una cuenta, ya sea con
            correo electrónico y contraseña, o vinculando tu cuenta de Google o Facebook.
          </P>
          <P>Eres responsable de:</P>
          <Ul items={[
            'Proporcionar información veraz al registrarte.',
            'Mantener la confidencialidad de tu contraseña.',
            'Todo uso que se haga de tu cuenta.',
          ]} />
          <P>
            Puedes solicitar la desactivación o eliminación de tu cuenta en cualquier momento
            desde la Plataforma.
          </P>

          {/* ── Sección 4 ── */}
          <H2>4. Contenido publicado por los usuarios</H2>
          <P>
            Si publicas contenido en la Plataforma (foto de perfil, imágenes de un evento,
            descripción, etc.), declaras que:
          </P>
          <Ul items={[
            'Tienes los derechos necesarios para publicar ese contenido.',
            'El contenido no infringe derechos de terceros ni la ley colombiana.',
            'El contenido no incluye material sexual, violento, discriminatorio o que de cualquier forma viole las normas de convivencia de la Plataforma.',
          ]} />
          <P>
            Todas las imágenes subidas a la Plataforma pasan por un proceso automático de
            moderación de contenido. Nos reservamos el derecho de rechazar o eliminar cualquier
            contenido que no cumpla con estas condiciones, así como de suspender cuentas que
            las incumplan de forma reiterada.
          </P>

          {/* ── Sección 5 ── */}
          <H2>5. Compra de boletas y pagos</H2>
          <Ul items={[
            'Los pagos dentro de la Plataforma se procesan a través de la pasarela de pagos Wompi. Tu Evento no almacena los datos completos de tu tarjeta o medio de pago; ese procesamiento lo realiza Wompi bajo sus propias políticas de seguridad.',
            'Al comprar una boleta, recibirás un ticket digital con un código QR único, que será validado en el ingreso al evento.',
            'Las políticas de reembolso, cambios o cancelación de un evento son definidas por cada organizador, salvo que la ley aplicable disponga lo contrario. Te recomendamos revisar las condiciones específicas de cada evento antes de comprar.',
            'Tu Evento actúa como intermediario tecnológico entre organizadores y asistentes; la responsabilidad sobre la realización, calidad y condiciones del evento corresponde al organizador que lo publica.',
          ]} />

          {/* ── Sección 6 ── */}
          <H2>6. Responsabilidades del organizador</H2>
          <P>Si publicas un evento como organizador, te comprometes a:</P>
          <Ul items={[
            'Brindar información veraz sobre el evento (fecha, lugar, precio, cupo disponible).',
            'Cumplir con la normativa vigente aplicable a la realización de eventos públicos en Colombia.',
            'Responder ante los asistentes por la correcta ejecución del evento publicado.',
          ]} />

          {/* ── Sección 7 ── */}
          <H2>7. Uso indebido de la plataforma</H2>
          <P>Está prohibido:</P>
          <Ul items={[
            'Crear eventos falsos o fraudulentos.',
            'Usar la Plataforma para actividades ilegales.',
            'Intentar vulnerar la seguridad del sistema.',
            'Suplantar la identidad de otra persona u organización.',
          ]} />
          <P>
            El incumplimiento de estos puntos puede resultar en la suspensión o eliminación
            de tu cuenta.
          </P>

          {/* ── Sección 8 ── */}
          <H2>8. Propiedad intelectual</H2>
          <P>
            El nombre "Tu Evento", su logo, diseño e interfaz son propiedad del equipo CapySoft.
            El contenido que tú publicas (fotos, descripciones de eventos) sigue siendo de tu
            propiedad, pero nos otorgas una licencia para mostrarlo dentro de la Plataforma
            con el fin de operar el servicio.
          </P>

          {/* ── Sección 9 ── */}
          <H2>9. Limitación de responsabilidad</H2>
          <P>
            Tu Evento se ofrece "tal cual" (as-is). Al ser un proyecto en desarrollo continuo,
            no garantizamos disponibilidad ininterrumpida del servicio. No somos responsables
            por daños derivados de la cancelación de un evento por parte del organizador, ni
            por el mal uso que un tercero haga de la información publicada en la Plataforma.
          </P>

          {/* ── Sección 10 ── */}
          <H2>10. Modificaciones</H2>
          <P>
            Podemos actualizar estos Términos de Uso en cualquier momento. Los cambios
            importantes serán notificados dentro de la Plataforma. El uso continuado del
            servicio después de una actualización implica la aceptación de los nuevos términos.
          </P>

          {/* ── Sección 11 ── */}
          <H2>11. Contacto</H2>
          <P>
            Para dudas sobre estos términos, puedes escribirnos a:{' '}
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
