package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Student profile used on Settings, exports, and shared notes attribution.
 */
public final class UserProfile {

    @NonNull public final String username;
    @NonNull public final String fullName;
    @NonNull public final String dateOfBirth;
    @NonNull public final String university;
    @NonNull public final String program;
    @NonNull public final String studentId;

    public UserProfile(@Nullable String username,
                       @Nullable String fullName,
                       @Nullable String dateOfBirth,
                       @Nullable String university,
                       @Nullable String program,
                       @Nullable String studentId) {
        this.username = safe(username);
        this.fullName = safe(fullName);
        this.dateOfBirth = safe(dateOfBirth);
        this.university = safe(university);
        this.program = safe(program);
        this.studentId = safe(studentId);
    }

    public boolean isEmpty() {
        return username.isEmpty()
                && fullName.isEmpty()
                && dateOfBirth.isEmpty()
                && university.isEmpty()
                && program.isEmpty()
                && studentId.isEmpty();
    }

    /** Preferred display name: full name, else username. */
    @NonNull
    public String displayName() {
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return username;
    }

    /**
     * Multi-line plain attribution for exports (empty when no profile filled).
     *
     * @param courseProfessor optional professor for the lecture's course
     */
    @NonNull
    public String attributionPlain(@Nullable String courseProfessor) {
        StringBuilder sb = new StringBuilder();
        String name = displayName();
        if (!name.isEmpty()) {
            sb.append("Student: ").append(name);
            if (!username.isEmpty() && !username.equals(fullName)) {
                sb.append(" (@").append(username).append(')');
            }
            sb.append('\n');
        } else if (!username.isEmpty()) {
            sb.append("Student: @").append(username).append('\n');
        }
        if (!university.isEmpty()) {
            sb.append("University: ").append(university).append('\n');
        }
        if (!program.isEmpty()) {
            sb.append("Program: ").append(program).append('\n');
        }
        if (!studentId.isEmpty()) {
            sb.append("Student ID: ").append(studentId).append('\n');
        }
        if (!dateOfBirth.isEmpty()) {
            sb.append("DOB: ").append(dateOfBirth).append('\n');
        }
        String professor = courseProfessor != null ? courseProfessor.trim() : "";
        if (!professor.isEmpty()) {
            sb.append("Professor: ").append(professor).append('\n');
        }
        return sb.toString().trim();
    }

    @NonNull
    public String attributionMarkdown(@Nullable String courseProfessor) {
        String plain = attributionPlain(courseProfessor);
        if (plain.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("### Student / course\n\n");
        for (String line : plain.split("\n")) {
            sb.append("- ").append(line).append('\n');
        }
        return sb.toString().trim();
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
