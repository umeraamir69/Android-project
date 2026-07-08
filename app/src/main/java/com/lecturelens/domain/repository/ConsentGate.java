package com.lecturelens.domain.repository;

/**
 * Read-only consent check consumed by Track 3's {@code RecordLectureUseCase}
 * before the first pipeline enqueue (arch doc §1.1 "Privacy &amp; consent":
 * audio only leaves the device after explicit consent).
 *
 * <p><b>Ownership:</b> the authoritative consent flag lives in Track 1's
 * {@code SecureKeyStore} (EncryptedSharedPreferences, week 2). Track 3 only
 * reads it. This interface is the seam so the two tracks don't collide on a
 * concrete class: Track 1 makes {@code SecureKeyStore} implement it (or adds a
 * {@code @Binds}); until then a permissive stub is bound (see
 * {@code di/ConsentModule}) so the record → enqueue path is exercisable in dev.
 */
public interface ConsentGate {

    /**
     * @return {@code true} once the user has accepted cloud processing of their
     *         audio. When {@code false}, the lecture row is still persisted as
     *         {@code RECORDED} but the cloud pipeline is not enqueued.
     */
    boolean hasCloudConsent();
}
