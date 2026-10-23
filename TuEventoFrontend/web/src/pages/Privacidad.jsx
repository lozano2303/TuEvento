import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();

  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans flex flex-col">
      <main className="flex-1 w-full">
        <div className="max-w-3xl mx-auto px-6 py-16">

          {/* Encabezado */}
          <div className="mb-10">
            <h1 className="text-3xl font-black text-textPrimary tracking-tight mb-3">
              {t('privacy.title')} —{' '}
              <span className="text-accent">{t('privacy.subtitle')}</span>
            </h1>
            <p className="text-sm font-medium" style={{ color: 'rgba(196,181,253,0.65)' }}>
              {t('privacy.lastUpdate')}
            </p>
          </div>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt mb-10" />

          {/* Introducción */}
          <P>{t('privacy.intro')}</P>

          {/* ── Sección 1 ── */}
          <H2>{t('privacy.section1.title')}</H2>
          <P>
            <strong className="text-textPrimary font-semibold">{t('privacy.section1.p1')}</strong>
          </P>
          <P>
            {t('privacy.section1.p2')}{' '}
            <a
              href={`mailto:${t('privacy.section1.email')}`}
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              {t('privacy.section1.email')}
            </a>
          </P>

          {/* ── Sección 2 ── */}
          <H2>{t('privacy.section2.title')}</H2>
          <P>{t('privacy.section2.intro')}</P>

          <SubGroup
            title={t('privacy.section2.register.title')}
            items={[
              t('privacy.section2.register.item1'),
              t('privacy.section2.register.item2'),
            ]}
          />
          <SubGroup
            title={t('privacy.section2.profile.title')}
            items={[
              t('privacy.section2.profile.item1'),
              t('privacy.section2.profile.item2'),
              t('privacy.section2.profile.item3'),
            ]}
          />
          <SubGroup
            title={t('privacy.section2.purchase.title')}
            items={[
              t('privacy.section2.purchase.item1'),
              t('privacy.section2.purchase.item2'),
            ]}
          />
          <SubGroup
            title={t('privacy.section2.event.title')}
            items={[t('privacy.section2.event.item1')]}
          />
          <SubGroup
            title={t('privacy.section2.technical.title')}
            items={[
              t('privacy.section2.technical.item1'),
              t('privacy.section2.technical.item2'),
            ]}
          />

          {/* ── Sección 3 ── */}
          <H2>{t('privacy.section3.title')}</H2>
          <P>{t('privacy.section3.intro')}</P>
          <Ul items={[
            t('privacy.section3.item1'),
            t('privacy.section3.item2'),
            t('privacy.section3.item3'),
            t('privacy.section3.item4'),
            t('privacy.section3.item5'),
            t('privacy.section3.item6'),
          ]} />
          <P>{t('privacy.section3.p1')}</P>

          {/* ── Sección 4 ── */}
          <H2>{t('privacy.section4.title')}</H2>
          <Ul items={[
            t('privacy.section4.item1'),
            t('privacy.section4.item2'),
          ]} />

          {/* ── Sección 5 ── */}
          <H2>{t('privacy.section5.title')}</H2>
          <Ul items={[
            t('privacy.section5.item1'),
            t('privacy.section5.item2'),
            t('privacy.section5.item3'),
          ]} />

          {/* ── Sección 6 ── */}
          <H2>{t('privacy.section6.title')}</H2>
          <P>{t('privacy.section6.intro')}</P>
          <Ul items={[
            t('privacy.section6.item1'),
            t('privacy.section6.item2'),
            t('privacy.section6.item3'),
            t('privacy.section6.item4'),
          ]} />
          <P>
            {t('privacy.section6.p1')}{' '}
            <a
              href={`mailto:${t('privacy.section6.email')}`}
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              {t('privacy.section6.email')}
            </a>.
          </P>

          {/* ── Sección 7 ── */}
          <H2>{t('privacy.section7.title')}</H2>
          <P>{t('privacy.section7.intro')}</P>
          <Ul items={[
            t('privacy.section7.item1'),
            t('privacy.section7.item2'),
            t('privacy.section7.item3'),
          ]} />
          <P>{t('privacy.section7.p1')}</P>

          {/* ── Sección 8 ── */}
          <H2>{t('privacy.section8.title')}</H2>
          <P>{t('privacy.section8.p1')}</P>

          {/* ── Sección 9 ── */}
          <H2>{t('privacy.section9.title')}</H2>
          <P>{t('privacy.section9.p1')}</P>

          {/* ── Sección 10 ── */}
          <H2>{t('privacy.section10.title')}</H2>
          <P>
            {t('privacy.section10.p1')}{' '}
            <a
              href={`mailto:${t('privacy.section10.email')}`}
              className="text-accent hover:text-textPrimary transition-colors underline underline-offset-2"
            >
              {t('privacy.section10.email')}
            </a>
          </P>

          {/* Separador */}
          <div className="h-px bg-surfaceAlt my-10" />

          {/* Disclaimer */}
          <p
            className="text-xs leading-relaxed italic"
            style={{ color: 'rgba(196,181,253,0.55)' }}
          >
            {t('privacy.disclaimer')}
          </p>

          {/* Volver */}
          <div className="mt-8">
            <Link
              to="/"
              className="inline-flex items-center gap-2 text-sm font-medium text-accent hover:text-textPrimary transition-colors"
            >
              ← {t('privacy.backToHome')}
            </Link>
          </div>

        </div>
      </main>
      <Footer />
    </div>
  );
}
