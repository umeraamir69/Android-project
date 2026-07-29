package com.lecturelens.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.local.SearchHit;
import com.lecturelens.data.local.dao.SearchDao;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Flexible search: FTS sanitization, LIKE queries, merged sources, suggestions. */
public class SearchLecturesUseCaseTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private FakeSearchDao dao;
    private SearchLecturesUseCase useCase;

    @Before
    public void setUp() {
        dao = new FakeSearchDao();
        useCase = new SearchLecturesUseCase(dao, new DirectExecutors());
    }

    @Test
    public void blankQuery_returnsEmpty() {
        assertEquals("", SearchLecturesUseCase.toFtsQuery(null));
        assertEquals("", SearchLecturesUseCase.toFtsQuery("   "));
        assertEquals("", SearchLecturesUseCase.toFtsQuery("***"));
    }

    @Test
    public void singleToken_getsPrefixStar() {
        assertEquals("lifecycle*", SearchLecturesUseCase.toFtsQuery("lifecycle"));
        assertEquals("life*", SearchLecturesUseCase.toFtsQuery("Life"));
    }

    @Test
    public void multiToken_eachPrefixed() {
        assertEquals("activity* lifecycle*",
                SearchLecturesUseCase.toFtsQuery("activity lifecycle"));
    }

    @Test
    public void stripsFtsOperators() {
        String q = SearchLecturesUseCase.toFtsQuery("life* \"OR\" (cycle)");
        assertTrue(q.contains("life*"));
        assertTrue(q.contains("or*") || q.contains("cycle*"));
        assertFalse(q.contains("\""));
        assertFalse(q.contains("("));
    }

    @Test
    public void likeQuery_wrapsLowercasedTerm() {
        assertEquals("", SearchLecturesUseCase.toLikeQuery(null));
        assertEquals("", SearchLecturesUseCase.toLikeQuery("  "));
        assertEquals("%lifecycle%", SearchLecturesUseCase.toLikeQuery("Lifecycle"));
        assertEquals("%activity lifecycle%",
                SearchLecturesUseCase.toLikeQuery(" activity lifecycle "));
    }

    @Test
    public void likeQuery_stripsWildcards() {
        assertEquals("%100%", SearchLecturesUseCase.toLikeQuery("100%"));
        assertEquals("%foobar%", SearchLecturesUseCase.toLikeQuery("foo_bar"));
    }

    @Test
    public void execute_blank_returnsEmptyWithoutDaoCalls() {
        LiveData<List<SearchHit>> live = useCase.execute("   ");
        List<SearchHit> value = live.getValue();
        assertTrue(value == null || value.isEmpty());
        assertEquals(0, dao.transcriptCalls);
        assertEquals(0, dao.notesCalls);
        assertEquals(0, dao.chatCalls);
    }

    @Test
    public void execute_mergesTranscriptNotesAndChat() {
        dao.transcript = Collections.singletonList(
                hit(1, SearchHit.SOURCE_TRANSCRIPT, "heard lifecycle"));
        dao.notesSummary = Collections.singletonList(
                hit(1, SearchHit.SOURCE_NOTES, "summary about lifecycle"));
        dao.keyTerms = Collections.singletonList(
                hit(1, SearchHit.SOURCE_KEY_TERM, "[\"lifecycle\"]"));
        dao.actionItems = Collections.singletonList(
                hit(1, SearchHit.SOURCE_ACTION, "review lifecycle"));
        dao.chat = Collections.singletonList(
                hit(1, SearchHit.SOURCE_CHAT, "What is the lifecycle?"));

        List<SearchHit> hits = useCase.execute("lifecycle").getValue();
        assertEquals(5, hits.size());
        assertEquals(1, dao.transcriptCalls);
        assertEquals(1, dao.notesCalls);
        assertEquals(1, dao.keyTermCalls);
        assertEquals(1, dao.actionCalls);
        assertEquals(1, dao.chatCalls);
        assertTrue(dao.lastFtsQuery.contains("lifecycle"));
        assertTrue(dao.lastLikeQuery.contains("lifecycle"));
    }

    @Test
    public void suggest_shortPrefix_returnsEmpty() {
        List<String> suggestions = useCase.suggest("a").getValue();
        assertTrue(suggestions == null || suggestions.isEmpty());
        assertEquals(0, dao.titleSuggestCalls);
    }

    @Test
    public void suggest_mergesTitlesAndChatQuestions() {
        dao.titleSuggestions = Collections.singletonList("Android Lifecycle");
        dao.chatSuggestions = Collections.singletonList(
                "Explain the activity lifecycle in detail please");

        List<String> suggestions = useCase.suggest("li").getValue();
        assertEquals(2, suggestions.size());
        assertEquals("Android Lifecycle", suggestions.get(0));
        assertEquals("Explain the activity lifecycle in detail please", suggestions.get(1));
        assertEquals(1, dao.titleSuggestCalls);
        assertEquals(1, dao.chatSuggestCalls);
    }

    @Test
    public void suggest_truncatesLongChatQuestions() {
        String longQ = "What does the professor say about the Android activity "
                + "lifecycle and configuration changes in week three?";
        dao.chatSuggestions = Collections.singletonList(longQ);

        List<String> suggestions = useCase.suggest("wh").getValue();
        assertEquals(1, suggestions.size());
        assertTrue(suggestions.get(0).endsWith("…"));
        assertEquals(61, suggestions.get(0).length());
    }

    @NonNull
    private static SearchHit hit(long lectureId, @NonNull String type, @NonNull String snippet) {
        SearchHit hit = new SearchHit();
        hit.lectureId = lectureId;
        hit.segmentId = lectureId;
        hit.startMs = SearchHit.SOURCE_TRANSCRIPT.equals(type) ? 1000L : -1L;
        hit.lectureTitle = "Lecture";
        hit.snippet = snippet;
        hit.sourceType = type;
        hit.sourceLabel = type;
        return hit;
    }

    private static final class DirectExecutors extends AppExecutors {
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

    private static final class FakeSearchDao implements SearchDao {
        List<SearchHit> transcript = new ArrayList<>();
        List<SearchHit> notesSummary = new ArrayList<>();
        List<SearchHit> keyTerms = new ArrayList<>();
        List<SearchHit> actionItems = new ArrayList<>();
        List<SearchHit> chat = new ArrayList<>();
        List<String> titleSuggestions = new ArrayList<>();
        List<String> chatSuggestions = new ArrayList<>();

        int transcriptCalls;
        int notesCalls;
        int keyTermCalls;
        int actionCalls;
        int chatCalls;
        int titleSuggestCalls;
        int chatSuggestCalls;
        @Nullable String lastFtsQuery;
        @Nullable String lastLikeQuery;

        @Override
        public List<SearchHit> searchTranscriptSync(String ftsQuery) {
            transcriptCalls++;
            lastFtsQuery = ftsQuery;
            return transcript;
        }

        @Override
        public List<SearchHit> searchNotesSummarySync(String like) {
            notesCalls++;
            lastLikeQuery = like;
            return notesSummary;
        }

        @Override
        public List<SearchHit> searchKeyTermsSync(String like) {
            keyTermCalls++;
            lastLikeQuery = like;
            return keyTerms;
        }

        @Override
        public List<SearchHit> searchActionItemsSync(String like) {
            actionCalls++;
            lastLikeQuery = like;
            return actionItems;
        }

        @Override
        public List<SearchHit> searchChatSync(String like) {
            chatCalls++;
            lastLikeQuery = like;
            return chat;
        }

        @Override
        public List<String> suggestLectureTitles(String prefix) {
            titleSuggestCalls++;
            return titleSuggestions;
        }

        @Override
        public List<String> suggestChatQuestions(String prefix) {
            chatSuggestCalls++;
            return chatSuggestions;
        }

        @Override
        public LiveData<List<SearchHit>> search(String query) {
            return null;
        }

        @Override
        public List<SearchHit> searchSync(String query) {
            return transcript;
        }
    }
}
