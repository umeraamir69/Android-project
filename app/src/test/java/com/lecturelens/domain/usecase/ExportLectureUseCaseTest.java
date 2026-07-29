package com.lecturelens.domain.usecase;

import static org.junit.Assert.assertTrue;

import com.lecturelens.domain.model.Notes;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Track 5 — Markdown export content dry-run. */
public class ExportLectureUseCaseTest {

    @Test
    public void buildMarkdown_includesSections() {
        Notes notes = new Notes(
                1L,
                "Lifecycle overview.",
                Arrays.asList("onCreate", "onResume"),
                Collections.singletonList("Review the diagram"));

        String md = ExportLectureUseCase.buildMarkdown(
                "Week 6",
                "Welcome to lifecycle.",
                notes);

        assertTrue(md.contains("# Week 6"));
        assertTrue(md.contains("## Summary"));
        assertTrue(md.contains("Lifecycle overview."));
        assertTrue(md.contains("## Key terms"));
        assertTrue(md.contains("- onCreate"));
        assertTrue(md.contains("## Action items"));
        assertTrue(md.contains("## Transcript"));
        assertTrue(md.contains("Welcome to lifecycle."));
        assertTrue(md.contains("Exported from"));
        assertTrue(md.contains("LectureLens"));
    }

    @Test
    public void buildMarkdown_includesStudentAttribution() {
        String md = ExportLectureUseCase.buildMarkdown(
                "Week 6",
                "Transcript",
                null,
                "### Student / course\n\n- Student: Morgan Lee\n- Professor: Dr. Ada");
        assertTrue(md.contains("Student: Morgan Lee"));
        assertTrue(md.contains("Professor: Dr. Ada"));
        assertTrue(md.contains("## Summary"));
    }

    @Test
    public void buildMarkdown_handlesMissingNotes() {
        String md = ExportLectureUseCase.buildMarkdown(
                "Empty",
                "Just transcript.",
                null);
        assertTrue(md.contains("# Empty"));
        assertTrue(md.contains("## Transcript"));
        assertTrue(md.contains("Just transcript."));
        assertTrue(md.contains("Exported from"));
        assertTrue(md.contains("No summary available yet."));
    }

    @Test
    public void buildPlainText_includesFooterAndTranscript() {
        String text = ExportLectureUseCase.buildPlainText(
                "Demo",
                "Full transcript here.",
                null);
        assertTrue(text.contains("TRANSCRIPT"));
        assertTrue(text.contains("Full transcript here."));
        assertTrue(text.contains("Exported from LectureLens"));
    }
}
