package com.lecturelens.data.consent;

import com.lecturelens.domain.repository.ConsentGate;

import javax.inject.Inject;

/**
 * TEMPORARY STUB — bound to {@link ConsentGate} until Track 1's
 * {@code SecureKeyStore} (week 2) provides the real, user-driven consent flag.
 *
 * <p>Returns {@code true} so the record → enqueue path is exercisable in dev.
 * Track 1 replaces the {@code @Binds} in {@code di/UploadModule} (or points it at
 * {@code SecureKeyStore}) once the Login/Settings consent UX lands — <b>this stub
 * must not ship in a build that talks to the cloud without real consent.</b>
 */
public class PermissiveConsentGate implements ConsentGate {

    @Inject
    public PermissiveConsentGate() {
    }

    @Override
    public boolean hasCloudConsent() {
        return true; // TODO(Track 1): read the real flag from SecureKeyStore.
    }
}
