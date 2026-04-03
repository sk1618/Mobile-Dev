package com.example.studysmart;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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

    private TextView statsTotalTasks, statsReminders, statsTotalSessions, statsStudyMinutes;
    private TextView statsCompletionRate, statsAverageSession, statsLastSession, statsSummaryText;
    private TextView insightMostProductiveDay, insightTaskBalance;

    private PieChart taskDistributionChart;
    private BarChart weeklyStudyChart;
    private DatabaseHelper databaseHelper;

    public StatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        statsTotalTasks = view.findViewById(R.id.statsTotalTasks);
        statsReminders = view.findViewById(R.id.statsReminders);
        statsTotalSessions = view.findViewById(R.id.statsTotalSessions);
        statsStudyMinutes = view.findViewById(R.id.statsStudyMinutes);

        statsCompletionRate = view.findViewById(R.id.statsCompletionRate);
        statsAverageSession = view.findViewById(R.id.statsAverageSession);
        statsLastSession = view.findViewById(R.id.statsLastSession);
        statsSummaryText = view.findViewById(R.id.statsSummaryText);

        insightMostProductiveDay = view.findViewById(R.id.insightMostProductiveDay);
        insightTaskBalance = view.findViewById(R.id.insightTaskBalance);

        taskDistributionChart = view.findViewById(R.id.taskDistributionChart);
        weeklyStudyChart = view.findViewById(R.id.weeklyStudyChart);

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
        statsTotalSessions.setText(String.valueOf(totalSessions));
        statsStudyMinutes.setText(String.valueOf(totalStudyMinutes));

        statsCompletionRate.setText("Completion Rate: " + completionRate + "%");
        statsAverageSession.setText("Average Session Length: " + averageSession + " min");
        statsLastSession.setText("Last Session: " + (lastSessionDate != null ? lastSessionDate : "none"));
        statsSummaryText.setText("You have completed " + completedTasks + " out of " + totalTasks + " tasks.");

        updateTaskDistributionPieChart(pendingTasks, inProgressTasks, completedTasks);
        updateWeeklyStudyChart();
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

    private void updateWeeklyStudyChart() {
        int[] dailyMinutes = new int[7];

        ArrayList<StudySession> sessions = databaseHelper.getAllStudySessions();

        Calendar now = Calendar.getInstance();
        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (StudySession session : sessions) {
            try {
                Date sessionDate = sdf.parse(session.getSessionDate());
                if (sessionDate == null) continue;

                Calendar sessionCal = Calendar.getInstance();
                sessionCal.setTime(sessionDate);

                if (sessionCal.before(startOfWeek) || sessionCal.after(now)) {
                    continue;
                }

                int dayOfWeek = sessionCal.get(Calendar.DAY_OF_WEEK);
                int index;

                if (dayOfWeek == Calendar.MONDAY) index = 0;
                else if (dayOfWeek == Calendar.TUESDAY) index = 1;
                else if (dayOfWeek == Calendar.WEDNESDAY) index = 2;
                else if (dayOfWeek == Calendar.THURSDAY) index = 3;
                else if (dayOfWeek == Calendar.FRIDAY) index = 4;
                else if (dayOfWeek == Calendar.SATURDAY) index = 5;
                else index = 6;

                dailyMinutes[index] += session.getDurationMinutes();

            } catch (Exception ignored) {
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, dailyMinutes[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Study Minutes");
        dataSet.setColor(Color.parseColor("#111323"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);

        weeklyStudyChart.setData(barData);
        weeklyStudyChart.setFitBars(true);
        weeklyStudyChart.animateY(800);

        Description description = new Description();
        description.setText("");
        weeklyStudyChart.setDescription(description);

        weeklyStudyChart.setDrawGridBackground(false);
        weeklyStudyChart.setDrawBarShadow(false);
        weeklyStudyChart.setPinchZoom(false);
        weeklyStudyChart.setDoubleTapToZoomEnabled(false);

        Legend legend = weeklyStudyChart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = weeklyStudyChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
        ));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);

        YAxis leftAxis = weeklyStudyChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.BLACK);

        YAxis rightAxis = weeklyStudyChart.getAxisRight();
        rightAxis.setEnabled(false);

        weeklyStudyChart.invalidate();

        updateMostProductiveDayInsight(dailyMinutes);
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
            insightMostProductiveDay.setText("Most productive day: no study sessions yet");
        } else {
            insightMostProductiveDay.setText("Most productive day: " + days[maxIndex] + " (" + maxValue + " min)");
        }
    }

    private void updateInsights(int pending, int completed) {
        if (completed > pending) {
            insightTaskBalance.setText("Great job — you completed more tasks than you still have pending.");
        } else if (pending > completed) {
            insightTaskBalance.setText("You currently have more pending tasks than completed ones. Try finishing a few today.");
        } else {
            insightTaskBalance.setText("Your completed and pending tasks are balanced right now.");
        }
    }
}