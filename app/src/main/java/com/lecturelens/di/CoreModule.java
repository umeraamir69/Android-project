package com.lecturelens.di;

import com.lecturelens.core.AndroidLogger;
import com.lecturelens.core.Logger;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class CoreModule {

    @Binds
    abstract Logger bindLogger(AndroidLogger impl);
}
