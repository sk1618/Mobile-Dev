package com.example.studysmart;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

public class CircleSpan implements LineBackgroundSpan {

    private final int color;

    public CircleSpan(int color) {
        this.color = color;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint paint,
                               int left, int right, int top, int baseline, int bottom,
                               CharSequence text, int start, int end, int lineNumber) {

        int oldColor = paint.getColor();
        Paint.Style oldStyle = paint.getStyle();

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float cx = (left + right) / 2f;
        float cy = (top + bottom) / 2f;

        // MUCH bigger circle
        float radius = 30f;

        canvas.drawCircle(cx, cy, radius, paint);

        paint.setColor(oldColor);
        paint.setStyle(oldStyle);
    }
}

