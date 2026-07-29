package com.lecturelens.data.export;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Renders plain-text lecture notes into a multi-page PDF via {@link PdfDocument}. */
public final class NotesPdfWriter {

    private static final int PAGE_WIDTH = 595;   // A4-ish points
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final float TITLE_SIZE = 18f;
    private static final float BODY_SIZE = 11f;
    private static final float LINE_SPACING = 1.25f;

    private NotesPdfWriter() {
    }

    public static void write(@NonNull File out, @NonNull String title, @NonNull String body)
            throws IOException {
        PdfDocument doc = new PdfDocument();
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(TITLE_SIZE);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setColor(0xFF1A1C18);

        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setTextSize(BODY_SIZE);
        bodyPaint.setColor(0xFF1A1C18);

        int contentWidth = PAGE_WIDTH - (MARGIN * 2);
        StaticLayout titleLayout = StaticLayout.Builder
                .obtain(title, 0, title.length(), titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, LINE_SPACING)
                .setIncludePad(false)
                .build();

        StaticLayout bodyLayout = StaticLayout.Builder
                .obtain(body, 0, body.length(), bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, LINE_SPACING)
                .setIncludePad(false)
                .build();

        int pageNumber = 1;
        int y = MARGIN;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas canvas = page.getCanvas();

        canvas.save();
        canvas.translate(MARGIN, y);
        titleLayout.draw(canvas);
        canvas.restore();
        y += titleLayout.getHeight() + 16;

        int bodyLine = 0;
        while (bodyLine < bodyLayout.getLineCount()) {
            int lineTop = bodyLayout.getLineTop(bodyLine);
            int lineBottom = bodyLayout.getLineBottom(bodyLine);
            int lineHeight = lineBottom - lineTop;
            if (y + lineHeight > PAGE_HEIGHT - MARGIN) {
                doc.finishPage(page);
                pageNumber++;
                info = new PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                page = doc.startPage(info);
                canvas = page.getCanvas();
                y = MARGIN;
            }
            canvas.save();
            canvas.translate(MARGIN, y - lineTop);
            canvas.clipRect(0, lineTop, contentWidth, lineBottom);
            bodyLayout.draw(canvas);
            canvas.restore();
            y += lineHeight;
            bodyLine++;
        }

        doc.finishPage(page);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            doc.writeTo(fos);
        } finally {
            doc.close();
        }
    }
}
