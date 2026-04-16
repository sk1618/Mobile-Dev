package com.example.studysmart;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private TextView statsTotalTasks, statsReminders, statsCompletionRate, statsAverageSession, statsLastSession;
    private TextView statsSummaryText, insightMostProductiveDay, insightTaskBalance, statsTrendPill;

    private PieChart taskDistributionChart;
    private LineChart weeklyStudyLineChart;
    private DatabaseHelper databaseHelper;

    public StatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        statsTotalTasks = view.findViewById(R.id.statsTotalTasks);
        statsReminders = view.findViewById(R.id.statsReminders);
        statsCompletionRate = view.findViewById(R.id.statsCompletionRate);
        statsAverageSession = view.findViewById(R.id.statsAverageSession);
        statsLastSession = view.findViewById(R.id.statsLastSession);
        statsSummaryText = view.findViewById(R.id.statsSummaryText);
        insightMostProductiveDay = view.findViewById(R.id.insightMostProductiveDay);
        insightTaskBalance = view.findViewById(R.id.insightTaskBalance);
        statsTrendPill = view.findViewById(R.id.statsTrendPill);

        taskDistributionChart = view.findViewById(R.id.taskDistributionChart);
        weeklyStudyLineChart = view.findViewById(R.id.weeklyStudyLineChart);

        databaseHelper = new DatabaseHelper(getContext());

        refreshStats();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        int totalTasks = databaseHelper.getTotalTaskCount();
        int pendingTasks = databaseHelper.getTaskCountByStatus("Pending");
        int inProgressTasks = databaseHelper.getTaskCountByStatus("In Progress");
        int completedTasks = databaseHelper.getTaskCountByStatus("Completed");
        int reminderCount = databaseHelper.getReminderCount();

        int totalSessions = databaseHelper.getTotalStudySessions();
        int totalStudyMinutes = databaseHelper.getTotalStudyMinutes();
        String lastSessionDate = databaseHelper.getLastStudySessionDate();

        int completionRate = 0;
        if (totalTasks > 0) {
            completionRate = (completedTasks * 100) / totalTasks;
        }

        int averageSession = 0;
        if (totalSessions > 0) {
            averageSession = totalStudyMinutes / totalSessions;
        }

        statsTotalTasks.setText(String.valueOf(totalTasks));
        statsReminders.setText(String.valueOf(reminderCount));
        statsCompletionRate.setText(completionRate + "%");
        statsAverageSession.setText(averageSession + " min");
        statsLastSession.setText(lastSessionDate != null ? lastSessionDate : "none");
        statsSummaryText.setText("You have completed " + completedTasks + " out of " + totalTasks + " tasks.");

        updateTaskDistributionPieChart(pendingTasks, inProgressTasks, completedTasks);
        updateWeeklyTrendChart();
        updateInsights(pendingTasks, completedTasks);
    }

    private void updateTaskDistributionPieChart(int pending, int inProgress, int completed) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (pending > 0) entries.add(new PieEntry(pending, "Pending"));
        if (inProgress > 0) entries.add(new PieEntry(inProgress, "In Progress"));
        if (completed > 0) entries.add(new PieEntry(completed, "Completed"));

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Tasks");
        dataSet.setColors(
                Color.parseColor("#6C63FF"),
                Color.parseColor("#FFA726"),
                Color.parseColor("#26A69A"),
                Color.parseColor("#D0D0D0")
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(13f);

        PieData pieData = new PieData(dataSet);
        taskDistributionChart.setData(pieData);

        taskDistributionChart.setUsePercentValues(false);
        taskDistributionChart.setDrawHoleEnabled(true);
        taskDistributionChart.setHoleColor(Color.WHITE);
        taskDistributionChart.setTransparentCircleRadius(0f);
        taskDistributionChart.setCenterText("Tasks");
        taskDistributionChart.setCenterTextSize(16f);
        taskDistributionChart.setCenterTextColor(Color.BLACK);
        taskDistributionChart.animateY(800);

        Description description = new Description();
        description.setText("");
        taskDistributionChart.setDescription(description);

        Legend legend = taskDistributionChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.BLACK);

        taskDistributionChart.invalidate();
    }

    private void updateWeeklyTrendChart() {
        int[] currentWeek = new int[7];
        int[] previousWeek = new int[7];

        ArrayList<StudySession> sessions = databaseHelper.getAllStudySessions();

        Calendar now = Calendar.getInstance();
        Calendar startOfThisWeek = Calendar.getInstance();
        startOfThisWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfThisWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfThisWeek.set(Calendar.MINUTE, 0);
        startOfThisWeek.set(Calendar.SECOND, 0);
        startOfThisWeek.set(Calendar.MILLISECOND, 0);

        Calendar startOfPrevWeek = (Calendar) startOfThisWeek.clone();
        startOfPrevWeek.add(Calendar.DAY_OF_YEAR, -7);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (StudySession session : sessions) {
            try {
                Date sessionDate = sdf.parse(session.getSessionDate());
                if (sessionDate == null) continue;

                Calendar sessionCal = Calendar.getInstance();
                sessionCal.setTime(sessionDate);
                long sessionMillis = sessionCal.getTimeInMillis();

                if (sessionMillis >= startOfThisWeek.getTimeInMillis() && sessionMillis <= now.getTimeInMillis()) {
                    int index = mapDay(sessionCal.get(Calendar.DAY_OF_WEEK));
                    currentWeek[index] += session.getDurationMinutes();
                } else if (sessionMillis >= startOfPrevWeek.getTimeInMillis() && sessionMillis < startOfThisWeek.getTimeInMillis()) {
                    int index = mapDay(sessionCal.get(Calendar.DAY_OF_WEEK));
                    previousWeek[index] += session.getDurationMinutes();
                }

            } catch (Exception ignored) {
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, currentWeek[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Weekly Trend");
        dataSet.setColor(Color.parseColor("#0B1026"));
        dataSet.setLineWidth(3.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#EEF0F4"));

        LineData lineData = new LineData(dataSet);
        weeklyStudyLineChart.setData(lineData);

        Description description = new Description();
        description.setText("");
        weeklyStudyLineChart.setDescription(description);
        weeklyStudyLineChart.setDrawGridBackground(false);
        weeklyStudyLineChart.setTouchEnabled(false);

        Legend legend = weeklyStudyLineChart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = weeklyStudyLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"M", "T", "W", "T", "F", "S", "S"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setAxisLineColor(Color.TRANSPARENT);

        YAxis leftAxis = weeklyStudyLineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setGridColor(Color.parseColor("#E9E9EE"));
        leftAxis.setAxisLineColor(Color.TRANSPARENT);

        YAxis rightAxis = weeklyStudyLineChart.getAxisRight();
        rightAxis.setEnabled(false);

        weeklyStudyLineChart.invalidate();

        int currentTotal = 0;
        int previousTotal = 0;

        for (int value : currentWeek) currentTotal += value;
        for (int value : previousWeek) previousTotal += value;

        String trendText;
        if (previousTotal == 0 && currentTotal > 0) {
            trendText = "+100%";
        } else if (previousTotal == 0) {
            trendText = "+0%";
        } else {
            int percent = Math.round(((currentTotal - previousTotal) * 100f) / previousTotal);
            trendText = (percent >= 0 ? "+" : "") + percent + "%";
        }

        statsTrendPill.setText(trendText);
        updateMostProductiveDayInsight(currentWeek);
    }

    private void updateMostProductiveDayInsight(int[] dailyMinutes) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        int maxIndex = 0;
        int maxValue = dailyMinutes[0];

        for (int i = 1; i < dailyMinutes.length; i++) {
            if (dailyMinutes[i] > maxValue) {
                maxValue = dailyMinutes[i];
                maxIndex = i;
            }
        }

        if (maxValue == 0) {
            insightMostProductiveDay.setText("No study sessions yet");
        } else {
            insightMostProductiveDay.setText(days[maxIndex] + " was your strongest study day with " + maxValue + " minutes.");
        }
    }

    private void updateInsights(int pending, int completed) {
        if (completed > pending) {
            insightTaskBalance.setText("You completed more tasks than you still have pending. Keep the momentum going.");
        } else if (pending > completed) {
            insightTaskBalance.setText("You currently have more pending tasks than completed ones. Try closing a few small tasks first.");
        } else {
            insightTaskBalance.setText("Your completed and pending tasks are balanced right now.");
        }
    }

    private int mapDay(int dayOfWeek) {
        if (dayOfWeek == Calendar.MONDAY) return 0;
        if (dayOfWeek == Calendar.TUESDAY) return 1;
        if (dayOfWeek == Calendar.WEDNESDAY) return 2;
        if (dayOfWeek == Calendar.THURSDAY) return 3;
        if (dayOfWeek == Calendar.FRIDAY) return 4;
        if (dayOfWeek == Calendar.SATURDAY) return 5;
        return 6;
    }
}