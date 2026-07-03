package com.lecturelens;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Application entry point.
 *
 * {@code @HiltAndroidApp} triggers Hilt's code generation so use cases and
 * repositories declared in the data/domain layers can be injected into
 * ViewModels without per-screen wiring.
 */
@HiltAndroidApp
public class LectureLensApp extends Application {
}
