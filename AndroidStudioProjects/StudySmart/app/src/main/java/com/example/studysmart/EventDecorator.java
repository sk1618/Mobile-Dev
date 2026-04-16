package com.example.studysmart;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;

public class EventDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> dates;
    private final int circleColor;

    public EventDecorator(HashSet<CalendarDay> dates, int circleColor) {
        this.dates = dates;
        this.circleColor = circleColor;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new CircleSpan(circleColor));
        view.addSpan(new ForegroundColorSpan(Color.WHITE));
    }
}
