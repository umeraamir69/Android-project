package com.lecturelens.ui.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.model.AuthUser;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.CredentialsStore;
import com.lecturelens.domain.repository.LibrarySyncRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Executor;

public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

    private FakeAuth auth;
    private FakeStore store;
    private FakeSeeder seeder;
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        auth = new FakeAuth();
        store = new FakeStore();
        seeder = new FakeSeeder();
        viewModel = new LoginViewModel(auth, store, seeder, new FakeSync(), new DirectExecutors());
    }

    @Test
    public void alreadySignedIn_skipsLogin() {
        auth.signedIn = true;
        LoginViewModel vm = new LoginViewModel(auth, store, seeder, new FakeSync(), new DirectExecutors());
        assertTrue(Boolean.TRUE.equals(vm.getSignedIn().getValue()));
    }

    @Test
    public void signIn_rejectsInvalidEmail() {
        viewModel.signInWithPassword("not-an-email", "secret", false);
        assertNotNull(viewModel.getEmailError().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
    }

    @Test
    public void signIn_persistsConsentAndSeeds() {
        viewModel.signInWithPassword("zee@uni.ca", "secret123", true);
        assertTrue(store.consent);
        assertTrue(seeder.seeded);
        assertTrue(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
    }

    @Test
    public void createAccount_requiresPasswordLength() {
        viewModel.createAccount("zee@uni.ca", "123", true);
        assertNotNull(viewModel.getPasswordError().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSignedIn().getValue()));
    }

    private static class FakeAuth implements AuthRepository {
        boolean signedIn;

        @Nullable
        @Override
        public AuthUser getCurrentUser() {
            return signedIn ? new AuthUser("uid", "zee@uni.ca", null) : null;
        }

        @Override
        public boolean isSignedIn() {
            return signedIn;
        }

        @NonNull
        @Override
        public LiveData<AuthUser> observeUser() {
            return new MutableLiveData<>();
        }

        @Override
        public void signInWithEmailPassword(@NonNull String email,
                                           @NonNull String password,
                                           @NonNull Callback callback) {
            signedIn = true;
            callback.onSuccess();
        }

        @Override
        public void createAccount(@NonNull String email,
                                  @NonNull String password,
                                  @NonNull Callback callback) {
            signedIn = true;
            callback.onSuccess();
        }

        @Override
        public void signOut() {
            signedIn = false;
        }
    }

    private static class FakeStore implements CredentialsStore {
        boolean consent;

        @NonNull
        @Override
        public String getEmail() {
            return "";
        }

        @Override
        public void setEmail(@NonNull String email) {
        }

        @NonNull
        @Override
        public String getApiKey() {
            return "";
        }

        @Override
        public void setApiKey(@NonNull String apiKey) {
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
            return false;
        }
    }

    private static class FakeSync implements LibrarySyncRepository {
        @Override
        public void pushAll(@NonNull Callback callback) {
            callback.onDone();
        }

        @Override
        public void pullAll(@NonNull Callback callback) {
            callback.onDone();
        }

        @Override
        public void pushLecture(long lectureId) {
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
