package com.lecturelens.ui.util;

import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight markdown → Spanned for notes / AI answers.
 * Supports **bold**, *italic*, and leading -/* bullets.
 */
public final class MarkdownSpans {

    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");

    private MarkdownSpans() {
    }

    @NonNull
    public static Spanned fromLiteMarkdown(@NonNull String raw) {
        StringBuilder html = new StringBuilder();
        String[] lines = raw.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("- ") || line.startsWith("* ")) {
                html.append("• ").append(inlineToHtml(line.substring(2).trim()));
            } else if (line.matches("^#+\\s+.*")) {
                String heading = line.replaceFirst("^#+\\s+", "");
                html.append("<b>").append(inlineToHtml(heading)).append("</b>");
            } else {
                html.append(inlineToHtml(line));
            }
            if (i < lines.length - 1) {
                html.append("<br/>");
            }
        }
        return HtmlCompat.fromHtml(html.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    @NonNull
    private static String inlineToHtml(@NonNull String text) {
        String escaped = Html.escapeHtml(text);
        // After escape, restore markdown markers that were escaped as plain text.
        // Work on the original: apply markdown then escape carefully.
        String withBold = replaceAll(BOLD, text, "<b>", "</b>");
        String withItalic = replaceAll(ITALIC, withBold, "<i>", "</i>");
        // Escape everything except our inserted tags.
        return escapeKeepingTags(withItalic);
    }

    @NonNull
    private static String replaceAll(@NonNull Pattern pattern,
                                     @NonNull String input,
                                     @NonNull String open,
                                     @NonNull String close) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    Matcher.quoteReplacement(open + matcher.group(1) + close));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @NonNull
    private static String escapeKeepingTags(@NonNull String input) {
        String[] parts = input.split("(?=<b>|</b>|<i>|</i>)|(?<=<b>|</b>|<i>|</i>)");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if ("<b>".equals(part) || "</b>".equals(part)
                    || "<i>".equals(part) || "</i>".equals(part)) {
                out.append(part);
            } else {
                out.append(Html.escapeHtml(part));
            }
        }
        return out.toString();
    }
}
