package com.lecturelens.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.lecturelens.BuildConfig;
import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.repository.CredentialsStore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Executor;

/**
 * Track 1 — JVM tests for the login flow: validation, persistence, seeding.
 * Everything runs synchronously via direct executors + InstantTaskExecutorRule.
 */
public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

    private FakeStore store;
    private FakeSeeder seeder;
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        store = new FakeStore();
        seeder = new FakeSeeder();
        viewModel = new LoginViewModel(store, seeder, new DirectExecutors());
    }

    @Test
    public void prefill_loadsStoredValues() {
        store.email = "zee@uni.ca";
        store.apiKey = "key-123";
        store.consent = true;

        LoginViewModel vm = new LoginViewModel(store, seeder, new DirectExecutors());
        LoginViewModel.Prefill prefill = vm.getPrefill().getValue();

        assertNotNull(prefill);
        assertEquals("zee@uni.ca", prefill.email);
        assertEquals("key-123", prefill.apiKey);
        assertTrue(prefill.consent);
    }

    @Test
    public void alreadySignedIn_skipsLogin() {
        store.email = "zee@uni.ca";
        store.apiKey = "key-123";

        LoginViewModel vm = new LoginViewModel(store, seeder, new DirectExecutors());

        assertTrue(Boolean.TRUE.equals(vm.getSignedIn().getValue()));
    }

    @Test
    public void notSignedIn_staysOnLogin() {
        assertFalse(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
    }

    @Test
    public void signIn_rejectsInvalidEmail() {
        viewModel.signIn("not-an-email", "key-123", false);

        assertNotNull(viewModel.getEmailError().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
        assertEquals("", store.email); // nothing persisted
    }

    @Test
    public void signIn_emptyApiKey_allowedOnlyWithLocalDevKeys() {
        viewModel.signIn("zee@uni.ca", "   ", true);

        boolean hasDevKeys = BuildConfig.STT_API_KEY != null && !BuildConfig.STT_API_KEY.isEmpty()
                && BuildConfig.GEMINI_API_KEY != null && !BuildConfig.GEMINI_API_KEY.isEmpty();
        if (hasDevKeys) {
            // testing branch: local.properties keys cover STT + Gemini
            assertNull(viewModel.getApiKeyError().getValue());
            assertTrue(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
            assertEquals("", store.apiKey);
        } else {
            assertNotNull(viewModel.getApiKeyError().getValue());
            assertFalse(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
            assertEquals("", store.apiKey);
        }
    }

    @Test
    public void signIn_persistsSeedsAndSignals() {
        viewModel.signIn(" zee@uni.ca ", " key-123 ", true);

        assertNull(viewModel.getEmailError().getValue());
        assertNull(viewModel.getApiKeyError().getValue());
        assertEquals("zee@uni.ca", store.email);   // trimmed
        assertEquals("key-123", store.apiKey);     // trimmed
        assertTrue(store.consent);
        assertTrue(seeder.seeded);
        assertTrue(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getLoading().getValue()));
    }

    @Test
    public void signIn_withoutConsent_stillSignsIn() {
        viewModel.signIn("zee@uni.ca", "key-123", false);

        assertFalse(store.consent);
        assertTrue(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
    }

    // ---- Fakes ----

    private static class FakeStore implements CredentialsStore {
        String email = "";
        String apiKey = "";
        boolean consent;

        @NonNull
        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public void setEmail(@NonNull String email) {
            this.email = email;
        }

        @NonNull
        @Override
        public String getApiKey() {
            return apiKey;
        }

        @Override
        public void setApiKey(@NonNull String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public boolean hasCloudConsent() {
            return consent;
        }

        @Override
        public void setCloudConsent(boolean granted) {
            this.consent = granted;
        }

        @Override
        public boolean isSignedIn() {
            return !email.isEmpty() && !apiKey.isEmpty();
        }
    }

    private static class FakeSeeder extends DatabaseSeeder {
        boolean seeded;

        @Override
        public boolean seedIfEmpty() {
            seeded = true;
            return true;
        }
    }

    /** Runs everything on the calling thread. */
    private static class DirectExecutors extends AppExecutors {
        @NonNull
        @Override
        public Executor diskIO() {
            return Runnable::run;
        }

        @NonNull
        @Override
        public Executor networkIO() {
            return Runnable::run;
        }

        @NonNull
        @Override
        public Executor mainThread() {
            return Runnable::run;
        }
    }
}
