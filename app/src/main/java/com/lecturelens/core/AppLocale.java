package com.lecturelens.core;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import java.util.Locale;

/** Applies English / French UI locale for the course bilingual requirement. */
public final class AppLocale {

    public static final String EN = "en";
    public static final String FR = "fr";

    private AppLocale() {
    }

    @NonNull
    public static Context wrap(@NonNull Context context, @NonNull String languageTag) {
        Locale locale = FR.equals(languageTag) ? Locale.FRENCH : Locale.ENGLISH;
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(config);
    }

    public static void recreate(@NonNull Activity activity) {
        activity.recreate();
    }
}
