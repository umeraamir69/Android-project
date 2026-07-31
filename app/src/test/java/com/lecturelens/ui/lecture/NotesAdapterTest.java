package com.lecturelens.ui.lecture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lecturelens.domain.model.Notes;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Track 5 — notes row flattening dry-run. */
public class NotesAdapterTest {

    @Test
    public void fromNotes_buildsTypedRows() {
        Notes notes = new Notes(
                1L,
                "First paragraph.\n- Bullet line",
                Arrays.asList("lifecycle", "Room"),
                Collections.singletonList("Study more"));

        List<NotesAdapter.NotesRow> rows = NotesAdapter.fromNotes(
                notes, "Summary", "Key terms", "Action items");

        assertTrue(rows.size() >= 5);
        assertEquals(NotesAdapter.RowType.HEADING, rows.get(0).type);
        assertEquals("Summary", rows.get(0).text);

        boolean hasChipGroup = false;
        boolean hasAction = false;
        for (NotesAdapter.NotesRow row : rows) {
            if (row.type == NotesAdapter.RowType.KEY_TERM
                    && row.chips.contains("lifecycle")
                    && row.chips.contains("Room")) {
                hasChipGroup = true;
            }
            if (row.type == NotesAdapter.RowType.BULLET
                    && "Study more".equals(row.text)) {
                hasAction = true;
            }
        }
        assertTrue(hasChipGroup);
        assertTrue(hasAction);
    }
}
