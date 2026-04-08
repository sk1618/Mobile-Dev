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
                               CharSequence text, int start, int end, int lineNum) {

        int oldColor = paint.getColor();

        paint.setColor(color);
        paint.setAntiAlias(true);

        float cx = (left + right) / 2f;
        float cy = (top + bottom) / 2f;
        float radius = Math.min(right - left, bottom - top) / 2.8f;

        canvas.drawCircle(cx, cy, radius, paint);

        paint.setColor(oldColor);
    }
}

