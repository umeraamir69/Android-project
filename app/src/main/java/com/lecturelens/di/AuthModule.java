package com.lecturelens.di;

import com.lecturelens.data.auth.SecureKeyStore;
import com.lecturelens.data.repository.FirebaseAuthRepository;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.CredentialsStore;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Track 1 — auth wiring. {@link SecureKeyStore} backs credentials/consent;
 * Firebase Auth backs identity via {@link AuthRepository}.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class AuthModule {

    @Binds
    abstract ConsentGate bindConsentGate(SecureKeyStore impl);

    @Binds
    abstract CredentialsStore bindCredentialsStore(SecureKeyStore impl);

    @Binds
    abstract AuthRepository bindAuthRepository(FirebaseAuthRepository impl);
}
