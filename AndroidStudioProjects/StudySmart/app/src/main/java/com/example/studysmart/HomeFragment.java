package com.example.studysmart;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 25 * 60 * 1000;
    private boolean timerRunning = false;
    private long sessionStartMillis = 0;

    private TextView focusTimerText;
    private DatabaseHelper databaseHelper;
    private BarChart weeklyBarChart;

    private TextView totalSessionsText, totalStudyMinutesText, lastSessionText;
    private TextView cardStudyTimeText, cardSubjectsText, cardProductivityText;
    private TextView cardNextExamText, cardNextExamTitleText;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView greetingText = view.findViewById(R.id.greetingText);
        TextView nameText = view.findViewById(R.id.nameText);

        totalSessionsText = view.findViewById(R.id.totalSessionsText);
        totalStudyMinutesText = view.findViewById(R.id.totalStudyMinutesText);
        lastSessionText = view.findViewById(R.id.lastSessionText);

        cardStudyTimeText = view.findViewById(R.id.cardStudyTimeText);
        cardSubjectsText = view.findViewById(R.id.cardSubjectsText);
        cardProductivityText = view.findViewById(R.id.cardProductivityText);
        cardNextExamText = view.findViewById(R.id.cardNextExamText);
        cardNextExamTitleText = view.findViewById(R.id.cardNextExamTitleText);

        focusTimerText = view.findViewById(R.id.focusTimerText);
        weeklyBarChart = view.findViewById(R.id.weeklyBarChart);

        databaseHelper = new DatabaseHelper(getContext());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 18) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        greetingText.setText(greeting);

        SharedPreferences preferences = requireActivity().getSharedPreferences("StudySmartPrefs", android.content.Context.MODE_PRIVATE);
        String loggedInEmail = preferences.getString("loggedInEmail", null);

        if (loggedInEmail != null) {
            String userName = databaseHelper.getUserNameByEmail(loggedInEmail);
            if (userName != null && !userName.isEmpty()) {
                nameText.setText(userName + " 👋");
            } else {
                nameText.setText("User 👋");
            }
        } else {
            nameText.setText("User 👋");
        }

        refreshHomeData();
        updateTimerText();
        updateWeeklyPerformanceChart();

        view.findViewById(R.id.btnStartFocus).setOnClickListener(v -> {
            if (!timerRunning) {
                startTimer();
                Toast.makeText(getContext(), "Focus session started 🚀", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnStopFocus).setOnClickListener(v -> {
            if (timerRunning) {
                stopAndSaveSession();
            } else {
                Toast.makeText(getContext(), "No active session to save", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnAddExam).setOnClickListener(v -> showAddExamDialog());

        return view;
    }

    private void refreshHomeData() {
        int totalSessions = databaseHelper.getTotalStudySessions();
        int totalStudyMinutes = databaseHelper.getTotalStudyMinutes();
        String lastSessionDate = databaseHelper.getLastStudySessionDate();

        totalSessionsText.setText("• Total sessions: " + totalSessions);
        totalStudyMinutesText.setText("• Total study minutes: " + totalStudyMinutes);
        lastSessionText.setText("• Last session: " + (lastSessionDate != null ? lastSessionDate : "none"));

        int subjectCount = databaseHelper.getDistinctSubjectCount();

        int pendingCount = databaseHelper.getTaskCountByStatus("Pending");
        int inProgressCount = databaseHelper.getTaskCountByStatus("In Progress");
        int completedCount = databaseHelper.getTaskCountByStatus("Completed");
        int totalTasks = pendingCount + inProgressCount + completedCount;

        int productivity = 0;
        if (totalTasks > 0) {
            productivity = (completedCount * 100) / totalTasks;
        }

        cardStudyTimeText.setText(totalStudyMinutes + " min");
        cardSubjectsText.setText(String.valueOf(subjectCount));
        cardProductivityText.setText(productivity + "%");

        Exam nearestExam = databaseHelper.getNearestUpcomingExam();
        if (nearestExam != null) {
            cardNextExamText.setText(getDaysUntilExam(nearestExam.getExamDate()));
            cardNextExamTitleText.setText(nearestExam.getTitle());
        } else {
            cardNextExamText.setText("No exam");
            cardNextExamTitleText.setText("Add one");
        }
    }

    private String getDaysUntilExam(String examDateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Date examDate = sdf.parse(examDateString);
            if (examDate == null) return "No exam";

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar examCal = Calendar.getInstance();
            examCal.setTime(examDate);
            examCal.set(Calendar.HOUR_OF_DAY, 0);
            examCal.set(Calendar.MINUTE, 0);
            examCal.set(Calendar.SECOND, 0);
            examCal.set(Calendar.MILLISECOND, 0);

            long diffMillis = examCal.getTimeInMillis() - today.getTimeInMillis();
            long days = diffMillis / (24 * 60 * 60 * 1000);

            if (days <= 0) return "Today";
            return days + "d";
        } catch (Exception e) {
            return "No exam";
        }
    }

    private void showAddExamDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_exam, null);

        EditText editExamTitle = dialogView.findViewById(R.id.editExamTitle);
        EditText editExamDate = dialogView.findViewById(R.id.editExamDate);

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editExamTitle.getText().toString().trim();
                    String date = editExamDate.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean inserted = databaseHelper.insertExam(title, date);

                    if (inserted) {
                        Toast.makeText(getContext(), "Next exam saved", Toast.LENGTH_SHORT).show();
                        refreshHomeData();
                    } else {
                        Toast.makeText(getContext(), "Failed to save exam", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startTimer() {
        sessionStartMillis = System.currentTimeMillis();
        timerRunning = true;

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                int durationMinutes = 25;
                saveStudySession(durationMinutes);
                timeLeftInMillis = 25 * 60 * 1000;
                updateTimerText();
                Toast.makeText(getContext(), "Focus session completed and saved ✅", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void stopAndSaveSession() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timerRunning = false;

        long elapsedMillis = System.currentTimeMillis() - sessionStartMillis;
        int durationMinutes = (int) (elapsedMillis / 60000);

        if (durationMinutes <= 0) {
            durationMinutes = 1;
        }

        saveStudySession(durationMinutes);

        timeLeftInMillis = 25 * 60 * 1000;
        updateTimerText();

        Toast.makeText(getContext(), "Study session saved: " + durationMinutes + " min", Toast.LENGTH_SHORT).show();
    }

    private void saveStudySession(int durationMinutes) {
        String sessionDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        databaseHelper.insertStudySession(durationMinutes, sessionDate);
        refreshHomeData();
        updateWeeklyPerformanceChart();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        focusTimerText.setText(timeFormatted);
    }

    private void updateWeeklyPerformanceChart() {
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

        weeklyBarChart.setData(barData);
        weeklyBarChart.setFitBars(true);
        weeklyBarChart.animateY(800);

        Description description = new Description();
        description.setText("");
        weeklyBarChart.setDescription(description);

        weeklyBarChart.setDrawGridBackground(false);
        weeklyBarChart.setDrawBarShadow(false);
        weeklyBarChart.setPinchZoom(false);
        weeklyBarChart.setDoubleTapToZoomEnabled(false);

        Legend legend = weeklyBarChart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = weeklyBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
        ));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);

        YAxis leftAxis = weeklyBarChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.BLACK);

        YAxis rightAxis = weeklyBarChart.getAxisRight();
        rightAxis.setEnabled(false);

        weeklyBarChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}