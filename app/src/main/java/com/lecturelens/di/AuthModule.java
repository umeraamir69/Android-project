package com.lecturelens.di;

import com.lecturelens.data.auth.SecureKeyStore;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.CredentialsStore;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Track 1 — auth wiring. {@link SecureKeyStore} backs both interfaces, so
 * Track 3's consent check and the Login/Settings screens read the same flag.
 * (The temporary PermissiveConsentGate binding in UploadModule was removed
 * when this landed.)
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class AuthModule {

    @Binds
    abstract ConsentGate bindConsentGate(SecureKeyStore impl);

    @Binds
    abstract CredentialsStore bindCredentialsStore(SecureKeyStore impl);
}
