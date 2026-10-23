import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();

  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans flex flex-col">
      <main className="flex-1 w-full">
        <div className="max-w-3xl mx-auto px-6 py-16">

          {/* Encabezado */}
          <div className="mb-10">
            <h1 className="text-3xl font-black text-textPrimary tracking-tight mb-3">
              {t('terms.title')} —{' '}
              <span className="text-accent">{t('terms.subtitle')}</span>
            </h1>
            <p className="text-sm font-medium" style={{ color: 'rgba(196,181,253,0.65)' }}>
              {t('terms.lastUpdate')}
            </p>
          </div>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt mb-10" />

          {/* ── Sección 1 ── */}
          <H2>{t('terms.section1.title')}</H2>
          <P>{t('terms.section1.p1')}</P>
          <P>{t('terms.section1.p2')}</P>

          {/* ── Sección 2 ── */}
          <H2>{t('terms.section2.title')}</H2>
          <P>{t('terms.section2.intro')}</P>
          <Ul items={[
            t('terms.section2.item1'),
            t('terms.section2.item2'),
          ]} />

          {/* ── Sección 3 ── */}
          <H2>{t('terms.section3.title')}</H2>
          <P>{t('terms.section3.p1')}</P>
          <P>{t('terms.section3.intro')}</P>
          <Ul items={[
            t('terms.section3.item1'),
            t('terms.section3.item2'),
            t('terms.section3.item3'),
          ]} />
          <P>{t('terms.section3.p2')}</P>

          {/* ── Sección 4 ── */}
          <H2>{t('terms.section4.title')}</H2>
          <P>{t('terms.section4.intro')}</P>
          <Ul items={[
            t('terms.section4.item1'),
            t('terms.section4.item2'),
            t('terms.section4.item3'),
          ]} />
          <P>{t('terms.section4.p1')}</P>

          {/* ── Sección 5 ── */}
          <H2>{t('terms.section5.title')}</H2>
          <Ul items={[
            t('terms.section5.item1'),
            t('terms.section5.item2'),
            t('terms.section5.item3'),
            t('terms.section5.item4'),
          ]} />

          {/* ── Sección 6 ── */}
          <H2>{t('terms.section6.title')}</H2>
          <P>{t('terms.section6.intro')}</P>
          <Ul items={[
            t('terms.section6.item1'),
            t('terms.section6.item2'),
            t('terms.section6.item3'),
          ]} />

          {/* ── Sección 7 ── */}
          <H2>{t('terms.section7.title')}</H2>
          <P>{t('terms.section7.intro')}</P>
          <Ul items={[
            t('terms.section7.item1'),
            t('terms.section7.item2'),
            t('terms.section7.item3'),
            t('terms.section7.item4'),
          ]} />
          <P>{t('terms.section7.p1')}</P>

          {/* ── Sección 8 ── */}
          <H2>{t('terms.section8.title')}</H2>
          <P>{t('terms.section8.p1')}</P>

          {/* ── Sección 9 ── */}
          <H2>{t('terms.section9.title')}</H2>
          <P>{t('terms.section9.p1')}</P>

          {/* ── Sección 10 ── */}
          <H2>{t('terms.section10.title')}</H2>
          <P>{t('terms.section10.p1')}</P>

          {/* ── Sección 11 ── */}
          <H2>{t('terms.section11.title')}</H2>
          <P>
            {t('terms.section11.p1')}{' '}
            <a
              href={`mailto:${t('terms.section11.email')}`}
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              {t('terms.section11.email')}
            </a>
          </P>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt my-10" />

          {/* Disclaimer */}
          <p
            className="text-xs leading-relaxed italic"
            style={{ color: 'rgba(196,181,253,0.55)' }}
          >
            {t('terms.disclaimer')}
          </p>

          {/* Volver */}
          <div className="mt-8">
            <Link
              to="/"
              className="inline-flex items-center gap-2 text-sm font-medium text-accent hover:text-textPrimary transition-colors"
            >
              ← {t('terms.backToHome')}
            </Link>
          </div>

        </div>
      </main>
      <Footer />
    </div>
  );
}
