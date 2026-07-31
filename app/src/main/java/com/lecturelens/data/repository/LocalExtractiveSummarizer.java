package com.lecturelens.data.repository;

import androidx.annotation.NonNull;

import com.lecturelens.domain.model.Notes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-device extractive notes — frequency + cue-phrase scoring (not a local LLM).
 * Strong enough for offline study outlines without shipping a TFLite model.
 */
final class LocalExtractiveSummarizer {

    private static final Pattern SENTENCE = Pattern.compile("[^.!?\\n]+[.!?]?");
    private static final Pattern WORD = Pattern.compile("[a-zA-Z]{3,}");
    private static final Set<String> STOP = new HashSet<>();
    private static final String[] CUE_PHRASES = {
            "important", "remember", "key", "exam", "homework", "assignment",
            "definition", "means that", "in other words", "summary", "conclude",
            "first", "second", "third", "finally", "note that", "for example"
    };

    static {
        String[] stops = {
                "the", "and", "for", "that", "this", "with", "from", "have", "has",
                "was", "were", "are", "been", "being", "will", "would", "could",
                "should", "about", "into", "than", "then", "them", "they", "their",
                "there", "what", "when", "where", "which", "while", "your", "you",
                "just", "like", "also", "very", "more", "some", "such", "only"
        };
        Collections.addAll(STOP, stops);
    }

    private LocalExtractiveSummarizer() {
    }

    @NonNull
    static Notes summarize(long lectureId, @NonNull String transcriptText) {
        String text = transcriptText.trim();
        List<ScoredSentence> scored = scoreSentences(text);
        List<String> bullets = new ArrayList<>();
        int limit = Math.min(6, scored.size());
        for (int i = 0; i < limit; i++) {
            bullets.add(scored.get(i).text);
        }
        if (bullets.isEmpty()) {
            bullets.add(text.substring(0, Math.min(280, text.length())));
        }

        StringBuilder summary = new StringBuilder();
        summary.append("## On-device summary\n\n");
        summary.append("_Extractive outline (no cloud LLM). Switch to Cloud mode for Gemini notes._\n\n");
        for (String s : bullets) {
            summary.append("- ").append(s).append('\n');
        }

        List<String> actions = extractActions(text);
        if (actions.isEmpty()) {
            actions.add("Review the transcript for details");
            actions.add("Re-run with Cloud mode for Gemini study notes");
        }

        return new Notes(lectureId, summary.toString().trim(), extractKeyTerms(text), actions);
    }

    /**
     * Keyword / chat-aware extractive answer for offline Ask AI.
     */
    @NonNull
    static String answerLocally(@NonNull String question,
                                @NonNull String transcriptOrNotes,
                                @NonNull String chatHistory) {
        String corpus = (chatHistory + "\n" + transcriptOrNotes).trim();
        if (corpus.isEmpty()) {
            return "No lecture text is available yet for an on-device answer.";
        }
        Set<String> qWords = tokens(question);
        List<ScoredSentence> scored = scoreSentences(corpus);
        List<String> hits = new ArrayList<>();
        for (ScoredSentence s : scored) {
            Set<String> overlap = tokens(s.text);
            overlap.retainAll(qWords);
            if (overlap.size() >= 1) {
                hits.add(s.text);
            }
            if (hits.size() >= 4) {
                break;
            }
        }
        if (hits.isEmpty()) {
            for (int i = 0; i < Math.min(3, scored.size()); i++) {
                hits.add(scored.get(i).text);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("**On-device answer** (extractive; not a generative model)\n\n");
        if (!chatHistory.isEmpty()) {
            sb.append("_Prior chat was considered for keywords._\n\n");
        }
        for (String h : hits) {
            sb.append("- ").append(h).append('\n');
        }
        return sb.toString().trim();
    }

    @NonNull
    private static List<ScoredSentence> scoreSentences(@NonNull String text) {
        Map<String, Integer> df = new HashMap<>();
        List<String> sentences = new ArrayList<>();
        Matcher m = SENTENCE.matcher(text);
        while (m.find() && sentences.size() < 80) {
            String s = m.group().trim();
            if (s.length() < 25) {
                continue;
            }
            sentences.add(s);
            for (String w : tokens(s)) {
                df.put(w, df.getOrDefault(w, 0) + 1);
            }
        }
        List<ScoredSentence> out = new ArrayList<>();
        int n = sentences.size();
        for (int i = 0; i < n; i++) {
            String s = sentences.get(i);
            double score = 0;
            Set<String> toks = tokens(s);
            for (String w : toks) {
                int c = df.getOrDefault(w, 0);
                if (c > 0) {
                    score += 1.0 + Math.log(1 + c);
                }
            }
            // Prefer early + mid lecture sentences slightly.
            score += (n - i) / (double) Math.max(1, n) * 0.8;
            String lower = s.toLowerCase(Locale.US);
            for (String cue : CUE_PHRASES) {
                if (lower.contains(cue)) {
                    score += 2.5;
                }
            }
            if (s.length() > 220) {
                score *= 0.85;
            }
            out.add(new ScoredSentence(s, score));
        }
        out.sort(Comparator.comparingDouble((ScoredSentence a) -> a.score).reversed());
        return out;
    }

    @NonNull
    private static List<String> extractKeyTerms(@NonNull String text) {
        Map<String, Integer> counts = new HashMap<>();
        for (String w : tokens(text)) {
            if (w.length() < 5) {
                continue;
            }
            counts.put(w, counts.getOrDefault(w, 0) + 1);
        }
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> terms = new ArrayList<>();
        for (Map.Entry<String, Integer> e : ranked) {
            if (terms.size() >= 10) {
                break;
            }
            String w = e.getKey();
            terms.add(w.substring(0, 1).toUpperCase(Locale.US) + w.substring(1));
        }
        return terms;
    }

    @NonNull
    private static List<String> extractActions(@NonNull String text) {
        List<String> actions = new ArrayList<>();
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("homework") || lower.contains("assignment")) {
            actions.add("Complete the mentioned homework / assignment");
        }
        if (lower.contains("exam") || lower.contains("quiz") || lower.contains("midterm")) {
            actions.add("Review material flagged for exam / quiz");
        }
        if (lower.contains("read") && lower.contains("chapter")) {
            actions.add("Read the referenced chapter");
        }
        Matcher m = SENTENCE.matcher(text);
        while (m.find() && actions.size() < 5) {
            String s = m.group().trim();
            String l = s.toLowerCase(Locale.US);
            if (l.contains("you should") || l.contains("make sure") || l.contains("don't forget")
                    || l.startsWith("remember") || l.contains("due ")) {
                if (s.length() > 20 && s.length() < 180 && !actions.contains(s)) {
                    actions.add(s);
                }
            }
        }
        return actions;
    }

    @NonNull
    private static Set<String> tokens(@NonNull String text) {
        Set<String> out = new HashSet<>();
        Matcher m = WORD.matcher(text.toLowerCase(Locale.US));
        while (m.find()) {
            String w = m.group();
            if (!STOP.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }

    private static final class ScoredSentence {
        final String text;
        final double score;

        ScoredSentence(String text, double score) {
            this.text = text;
            this.score = score;
        }
    }
}
