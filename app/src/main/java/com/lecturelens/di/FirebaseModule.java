package com.lecturelens.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.lecturelens.data.repository.FirestoreCloudShareRepository;
import com.lecturelens.domain.repository.CloudShareRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class FirebaseModule {

    @Provides
    @Singleton
    static FirebaseFirestore provideFirestore() {
        return FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    static FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    static FirebaseStorage provideFirebaseStorage() {
        return FirebaseStorage.getInstance();
    }

    @Binds
    @Singleton
    abstract CloudShareRepository bindCloudShareRepository(FirestoreCloudShareRepository impl);
}
