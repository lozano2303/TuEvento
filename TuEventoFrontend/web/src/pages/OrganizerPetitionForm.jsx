import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Upload, FileText, UserCheck, Info, Send, Delete, CheckCircle, Clock, XCircle, AlertCircle } from 'lucide-react';
import { createPetition, getPetitionStatus } from '../services/OrganizerPetitionService';

const OrganizerPetitionForm = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [petitionStatus, setPetitionStatus] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => { checkPetitionStatus(); }, []);

  const checkPetitionStatus = async () => {
    try {
      const result = await getPetitionStatus();
      if (result.data) {
        setPetitionStatus(result.data);
        if (result.data.status === 'APPROVED') setSuccess(true);
      }
    } catch { setPetitionStatus(null); }
  };

  const handleDrag = (e) => {
    e.preventDefault(); e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') setDragActive(true);
    else if (e.type === 'dragleave') setDragActive(false);
  };
  const handleDrop = (e) => {
    e.preventDefault(); e.stopPropagation(); setDragActive(false);
    if (e.dataTransfer.files?.[0]) setSelectedFile(e.dataTransfer.files[0]);
  };
  const handleFileSelect = (e) => { if (e.target.files?.[0]) setSelectedFile(e.target.files[0]); };
  const handleRemoveFile = () => setSelectedFile(null);

  const handleSubmit = async () => {
    if (!selectedFile) { setError(t('organizerPetition.errors.selectDocument')); return; }
    setLoading(true); setError(null);
    try {
      const formData = new FormData();
      formData.append('document', selectedFile);
      await createPetition(formData);
      setSuccess(true);
      await checkPetitionStatus();
    } catch (err) {
      setError(err.message || t('organizerPetition.errors.submitError'));
    } finally {
      setLoading(false);
    }
  };

  const showFileUpload = !petitionStatus || petitionStatus.status === 'REJECTED';

  return (
    <div className="min-h-screen bg-background text-textPrimary font-sans">
      <main className="max-w-[1024px] mx-auto w-full px-6 py-10">

        {/* Header */}
        <section className="mb-10">
          <div className="flex flex-col gap-2">
            <h1 className="text-textPrimary text-4xl md:text-5xl font-black leading-tight tracking-tight">
              {t('organizerPetition.title')}
            </h1>
            <p className="text-textSecondary text-lg max-w-2xl">
              {t('organizerPetition.subtitle')}
            </p>
          </div>
        </section>

        {/* Status banners */}
        {petitionStatus?.status === 'PENDING' && (
          <div className="mb-8 rounded-xl p-5 flex items-center gap-4"
            style={{ background: 'color-mix(in srgb, var(--color-primary) 10%, transparent)', border: '1px solid color-mix(in srgb, var(--color-primary) 20%, transparent)' }}>
            <Clock className="text-primary shrink-0 w-6 h-6" />
            <div>
              <p className="text-textPrimary font-semibold">{t('organizerPetition.status.pending')}</p>
              <p className="text-textMuted text-sm">{t('organizerPetition.status.pendingSince', { date: new Date(petitionStatus.applicationDate).toLocaleDateString() })}</p>
            </div>
          </div>
        )}
        {petitionStatus?.status === 'REJECTED' && (
          <div className="mb-8 rounded-xl p-5 flex items-center gap-4"
            style={{ background: 'color-mix(in srgb, var(--color-error) 10%, transparent)', border: '1px solid color-mix(in srgb, var(--color-error) 20%, transparent)' }}>
            <XCircle className="text-error shrink-0 w-6 h-6" />
            <div>
              <p className="text-textPrimary font-semibold">{t('organizerPetition.status.rejected')}</p>
              <p className="text-textMuted text-sm">{t('organizerPetition.status.rejectedRetry')}</p>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 flex flex-col gap-6">

            {error && (
              <div className="rounded-xl p-4 flex items-center gap-3"
                style={{ background: 'color-mix(in srgb, var(--color-error) 10%, transparent)', border: '1px solid color-mix(in srgb, var(--color-error) 20%, transparent)' }}>
                <AlertCircle className="text-error shrink-0" />
                <p className="text-error text-sm">{error}</p>
              </div>
            )}

            {(success || petitionStatus?.status === 'PENDING' || petitionStatus?.status === 'APPROVED') && (
              <div>
                <h3 className="text-textPrimary text-lg font-bold leading-tight tracking-tight mb-4 flex items-center gap-2">
                  <FileText className="text-primary" />
                  {petitionStatus?.status === 'APPROVED' ? t('organizerPetition.status.approved') : t('organizerPetition.fileUpload.fileLoaded')}
                </h3>
                <div className="flex items-center gap-4 rounded-xl px-4 py-3 justify-between"
                  style={{ background: 'var(--color-surface)', border: '1px solid var(--color-surfaceAlt)' }}>
                  <div className="flex items-center gap-4">
                    <div className="text-primary flex items-center justify-center rounded-lg shrink-0 size-12"
                      style={{ background: 'color-mix(in srgb, var(--color-primary) 10%, transparent)' }}>
                      <FileText className="w-6 h-6" />
                    </div>
                    <div className="flex flex-col justify-center">
                      <p className="text-textPrimary text-base font-semibold leading-none">{t('organizerPetition.documents.items.officialId')}</p>
                      <p className="text-textMuted text-sm mt-1">
                        {petitionStatus?.status === 'APPROVED'
                          ? t('organizerPetition.buttons.approved')
                          : petitionStatus?.applicationDate
                            ? t('organizerPetition.status.pendingSince', { date: new Date(petitionStatus.applicationDate).toLocaleDateString() })
                            : t('organizerPetition.fileUpload.uploaded')}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {petitionStatus?.status === 'PENDING' && (
                      <div className="text-primary flex size-8 items-center justify-center rounded-full"
                        style={{ background: 'color-mix(in srgb, var(--color-primary) 15%, transparent)' }}>
                        <Clock className="w-5 h-5" />
                      </div>
                    )}
                    {petitionStatus?.status === 'APPROVED' && (
                      <div className="text-success flex size-8 items-center justify-center rounded-full"
                        style={{ background: 'color-mix(in srgb, var(--color-success) 15%, transparent)' }}>
                        <CheckCircle className="w-5 h-5" />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {showFileUpload && (
              <>
                <div
                  className="rounded-xl p-8 transition-all"
                  style={{
                    background: 'color-mix(in srgb, var(--color-surface) 40%, transparent)',
                    border: `1px solid ${dragActive ? 'var(--color-primary)' : 'var(--color-surfaceAlt)'}`,
                  }}
                  onDragEnter={handleDrag} onDragLeave={handleDrag}
                  onDragOver={handleDrag} onDrop={handleDrop}
                >
                  <div
                    className="flex flex-col items-center gap-6 rounded-xl border-2 border-dashed px-6 py-12"
                    style={{
                      borderColor: dragActive ? 'var(--color-primary)' : 'color-mix(in srgb, var(--color-primary) 40%, transparent)',
                      background:  'color-mix(in srgb, var(--color-primary) 5%, transparent)',
                    }}
                  >
                    <div className="flex flex-col items-center gap-4 text-center">
                      <div className="size-16 rounded-full flex items-center justify-center"
                        style={{ background: 'color-mix(in srgb, var(--color-primary) 20%, transparent)' }}>
                        <Upload className="w-10 h-10 text-primary" />
                      </div>
                      <div className="flex flex-col gap-1">
                        <p className="text-textPrimary text-xl font-bold">{t('organizerPetition.fileUpload.dragDrop')}</p>
                        <p className="text-textMuted text-sm">{t('organizerPetition.fileUpload.clickSelect')}</p>
                      </div>
                    </div>
                    <input type="file" className="hidden" accept=".pdf"
                      onChange={handleFileSelect} id="file-upload" />
                    <label htmlFor="file-upload"
                      className="flex min-w-[200px] cursor-pointer items-center justify-center overflow-hidden rounded-full h-12 px-6 text-textPrimary text-sm font-bold leading-normal tracking-wide transition-transform active:scale-95 shadow-lg"
                      style={{ background: 'var(--color-primary)', boxShadow: 'var(--color-primary) 0 0 20px -8px' }}>
                      {t('organizerPetition.fileUpload.selectFile')}
                    </label>
                    <p className="text-textMuted text-xs font-medium uppercase tracking-widest">{t('organizerPetition.fileUpload.formatInfo')}</p>
                  </div>
                </div>

                {selectedFile && (
                  <div className="rounded-xl p-5 flex items-center justify-between"
                    style={{ background: 'color-mix(in srgb, var(--color-surface) 40%, transparent)', border: '1px solid var(--color-surfaceAlt)' }}>
                    <div className="flex items-center gap-3">
                      <FileText className="text-primary w-5 h-5" />
                      <div>
                        <p className="text-textPrimary text-sm font-medium">{selectedFile.name}</p>
                        <p className="text-textMuted text-xs">{(selectedFile.size / 1024 / 1024).toFixed(2)} MB</p>
                      </div>
                      <button onClick={handleRemoveFile}
                        className="ml-2 p-1 rounded transition-colors"
                        onMouseOver={e => e.currentTarget.style.background = 'color-mix(in srgb, var(--color-surfaceAlt) 60%, transparent)'}
                        onMouseOut={e => e.currentTarget.style.background = 'transparent'}>
                        <Delete className="text-textMuted w-4 h-4" />
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}

            <button onClick={handleSubmit}
              disabled={!selectedFile || loading || petitionStatus?.status === 'PENDING' || petitionStatus?.status === 'APPROVED'}
              className="flex items-center justify-center gap-2 w-full h-14 rounded-xl text-textPrimary font-bold text-lg tracking-wide transition-all active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
              style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-accent))' }}>
              {loading ? (
                <><div className="w-5 h-5 border-2 border-textPrimary/30 border-t-textPrimary rounded-full animate-spin" />{t('organizerPetition.buttons.sending')}</>
              ) : petitionStatus?.status === 'PENDING' ? (
                <>{t('organizerPetition.buttons.inReview')}<Clock className="w-5 h-5" /></>
              ) : petitionStatus?.status === 'APPROVED' ? (
                <>{t('organizerPetition.buttons.approved')}<CheckCircle className="w-5 h-5" /></>
              ) : (
                <><Send className="w-5 h-5" />{t('organizerPetition.buttons.send')}</>
              )}
            </button>
          </div>

          {/* Sidebar */}
          <aside className="flex flex-col gap-6">
            <div className="rounded-xl p-6"
              style={{ background: 'var(--color-surface)', border: '1px solid var(--color-surfaceAlt)' }}>
              <h3 className="text-textPrimary text-lg font-bold leading-tight mb-6 flex items-center gap-2">
                <UserCheck className="text-primary w-5 h-5" />
                {t('organizerPetition.documents.title')}
              </h3>
              <div className="mb-6">
                <p className="text-xs font-bold text-textMuted uppercase tracking-widest mb-3">{t('organizerPetition.documents.required')}</p>
                <ul className="space-y-3">
                  {[
                    t('organizerPetition.documents.items.idCard'),
                    t('organizerPetition.documents.items.passport'),
                    t('organizerPetition.documents.items.officialId')
                  ].map(item => (
                    <li key={item} className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-textMuted rounded-full mt-1.5 shrink-0" />
                      <span className="text-textSecondary text-sm">{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="pt-6" style={{ borderTop: '1px solid var(--color-surfaceAlt)' }}>
                <p className="text-xs font-bold text-textMuted uppercase tracking-widest mb-3">{t('organizerPetition.documents.optional')}</p>
                <ul className="space-y-3">
                  {[
                    t('organizerPetition.documents.items.backgroundCheck'),
                    t('organizerPetition.documents.items.courseCerts'),
                    t('organizerPetition.documents.items.references')
                  ].map(item => (
                    <li key={item} className="flex items-start gap-3">
                      <span className="w-1 h-1 bg-textMuted rounded-full mt-1.5 shrink-0" />
                      <span className="text-textMuted text-sm italic">{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="rounded-xl p-5"
              style={{ background: 'color-mix(in srgb, var(--color-primary) 10%, transparent)', border: '1px solid color-mix(in srgb, var(--color-primary) 20%, transparent)' }}>
              <div className="flex gap-3">
                <Info className="text-primary w-4 h-4 shrink-0 mt-0.5" />
                <p className="text-xs text-textSecondary leading-relaxed">
                  {t('organizerPetition.info.processingTime')}
                </p>
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
};

export default OrganizerPetitionForm;
