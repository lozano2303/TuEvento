import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Footer from '../../layouts/Footer';

/** Correo de contacto para privacidad y solicitudes de datos (uno solo para todos los idiomas). */
export const CONTACT_EMAIL = 'soporte.capysoft@gmail.com';

const linkClass = 'text-accent hover:text-textPrimary transition-colors underline underline-offset-2';

/**
 * Convierte las marcas del texto traducido en elementos:
 *   **texto**            → negrita
 *   [[email]]            → enlace mailto al correo de contacto
 *   [[link:/ruta|texto]] → enlace interno
 */
export function renderLegalText(text) {
  return text.split(/(\*\*[^*]+\*\*|\[\[[^\]]+\]\])/g).filter(Boolean).map((part, i) => {
    if (part.startsWith('**')) {
      return <strong key={i} className="text-textPrimary font-semibold">{part.slice(2, -2)}</strong>;
    }
    if (part === '[[email]]') {
      return <a key={i} href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>{CONTACT_EMAIL}</a>;
    }
    const link = /^\[\[link:([^|\]]+)\|([^\]]+)\]\]$/.exec(part);
    if (link) {
      return <Link key={i} to={link[1]} className={linkClass}>{link[2]}</Link>;
    }
    return part;
  });
}

const Paragraphs = ({ items = [] }) =>
  items.map((text, i) => (
    <p key={i} className="text-textSecondary text-sm leading-relaxed mb-4">{renderLegalText(text)}</p>
  ));

/**
 * Página legal (privacidad, eliminación de datos…) a partir de una clave de traducción con:
 *   { title, lastUpdate, intro?: string[], sections: [{ title, paragraphs?, items?, steps?, after? }] }
 * items es una lista con viñetas, steps una lista numerada y after los párrafos que siguen a la lista.
 */
export default function LegalPage({ translationKey }) {
  const { t } = useTranslation();
  const content = t(translationKey, { returnObjects: true });
  const sections = Array.isArray(content?.sections) ? content.sections : [];

  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans flex flex-col">
      <main className="flex-1 w-full">
        <article className="max-w-3xl mx-auto px-6 py-16">
          <header className="mb-10">
            <h1 className="text-3xl font-black text-textPrimary tracking-tight mb-3">{content?.title}</h1>
            <p className="text-sm font-medium" style={{ color: 'rgba(196,181,253,0.65)' }}>
              {content?.lastUpdate}
            </p>
          </header>

          <div className="h-px bg-surfaceAlt mb-10" />

          <Paragraphs items={content?.intro} />

          {sections.map((section, i) => (
            <section key={i}>
              <h2 className="text-lg font-bold text-textPrimary mt-10 mb-3 flex items-center gap-2">
                <span className="inline-block w-1 h-5 rounded-full flex-shrink-0" style={{ background: 'var(--color-primary)' }} />
                {section.title}
              </h2>
              <Paragraphs items={section.paragraphs} />
              {section.items?.length > 0 && (
                <ul className="space-y-2 mb-4 pl-2">
                  {section.items.map((item, j) => (
                    <li key={j} className="flex items-start gap-2.5 text-sm text-textSecondary leading-relaxed">
                      <span className="mt-1.5 w-1.5 h-1.5 rounded-full flex-shrink-0 bg-accent" />
                      <span>{renderLegalText(item)}</span>
                    </li>
                  ))}
                </ul>
              )}
              {section.steps?.length > 0 && (
                <ol className="list-decimal space-y-2 mb-4 pl-6 text-sm text-textSecondary leading-relaxed marker:text-accent">
                  {section.steps.map((step, j) => <li key={j}>{renderLegalText(step)}</li>)}
                </ol>
              )}
              <Paragraphs items={section.after} />
            </section>
          ))}

          <div className="h-px bg-surfaceAlt my-10" />

          <Link
            to="/"
            className="inline-flex items-center gap-2 text-sm font-medium text-accent hover:text-textPrimary transition-colors"
          >
            ← {t('legal.backToHome')}
          </Link>
        </article>
      </main>
      <Footer />
    </div>
  );
}
