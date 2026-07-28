package com.lecturelens.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Student profile attribution used on exports / shared notes. */
public class UserProfileTest {

    @Test
    public void emptyProfile_hasEmptyAttribution() {
        UserProfile profile = new UserProfile("", "", "", "", "", "");
        assertTrue(profile.isEmpty());
        assertEquals("", profile.attributionPlain(null));
        assertEquals("", profile.attributionMarkdown(""));
    }

    @Test
    public void attribution_includesNameUniversityAndProfessor() {
        UserProfile profile = new UserProfile(
                "mlee",
                "Morgan Lee",
                "1999-07-15",
                "Concordia University",
                "Computer Science",
                "S12345");

        String plain = profile.attributionPlain("Dr. Ada Lovelace");
        assertTrue(plain.contains("Student: Morgan Lee (@mlee)"));
        assertTrue(plain.contains("University: Concordia University"));
        assertTrue(plain.contains("Program: Computer Science"));
        assertTrue(plain.contains("Student ID: S12345"));
        assertTrue(plain.contains("DOB: 1999-07-15"));
        assertTrue(plain.contains("Professor: Dr. Ada Lovelace"));

        String md = profile.attributionMarkdown("Dr. Ada Lovelace");
        assertTrue(md.contains("### Student / course"));
        assertTrue(md.contains("- Student: Morgan Lee (@mlee)"));
        assertTrue(md.contains("- Professor: Dr. Ada Lovelace"));
    }

    @Test
    public void displayName_prefersFullNameThenUsername() {
        assertEquals("Morgan Lee",
                new UserProfile("mlee", "Morgan Lee", "", "", "", "").displayName());
        assertEquals("mlee",
                new UserProfile("mlee", "", "", "", "", "").displayName());
        assertEquals("",
                new UserProfile("", "  ", "", "", "", "").displayName());
        assertFalse(new UserProfile("mlee", "", "", "", "", "").isEmpty());
    }

    @Test
    public void course_storesProfessor() {
        Course course = new Course(3L, "Mobile", 0xFF112233, 10L, "Prof. Turing");
        assertEquals("Prof. Turing", course.getProfessor());
        assertEquals("", new Course(1L, "X", 0, 0L).getProfessor());
    }
}
