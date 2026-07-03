package com.lecturelens.di;

import com.lecturelens.core.AppExecutors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/** Complete (Track 1). One AppExecutors instance for the whole app. */
@Module
@InstallIn(SingletonComponent.class)
public class ExecutorsModule {

    @Provides
    @Singleton
    public AppExecutors provideAppExecutors() {
        return new AppExecutors();
    }
}
