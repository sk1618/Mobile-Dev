package com.example.studysmart;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 25 * 60 * 1000L;
    private boolean timerRunning = false;
    private long sessionStartMillis = 0L;

    private DatabaseHelper databaseHelper;

    private TextView greetingText, nameText, subGreetingText;
    private TextView focusTimerText, streakChipText, trendPillText;
    private TextView cardStudyTimeText, cardSubjectsText, cardProductivityText;
    private TextView cardNextExamText, cardNextExamTitleText;
    private TextView sessionTotalValue, sessionMinutesValue, sessionLastValue;
    private TextView calendarSummaryText, selectedDateTitleText, selectedDateEventsText;

    private ImageButton btnToggleCalendar;
    private LinearLayout calendarBodyContainer;

    private MaterialAutoCompleteTextView subjectDropdown;
    private LineChart weeklyLineChart;
    private MaterialCalendarView academicCalendarView;

    private boolean isCalendarExpanded = false;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        databaseHelper = new DatabaseHelper(requireContext());

        greetingText = view.findViewById(R.id.greetingText);
        nameText = view.findViewById(R.id.nameText);
        subGreetingText = view.findViewById(R.id.subGreetingText);
        focusTimerText = view.findViewById(R.id.focusTimerText);
        streakChipText = view.findViewById(R.id.streakChipText);
        trendPillText = view.findViewById(R.id.trendPillText);

        cardStudyTimeText = view.findViewById(R.id.cardStudyTimeText);
        cardSubjectsText = view.findViewById(R.id.cardSubjectsText);
        cardProductivityText = view.findViewById(R.id.cardProductivityText);
        cardNextExamText = view.findViewById(R.id.cardNextExamText);
        cardNextExamTitleText = view.findViewById(R.id.cardNextExamTitleText);

        sessionTotalValue = view.findViewById(R.id.sessionTotalValue);
        sessionMinutesValue = view.findViewById(R.id.sessionMinutesValue);
        sessionLastValue = view.findViewById(R.id.sessionLastValue);

        subjectDropdown = view.findViewById(R.id.subjectDropdown);
        weeklyLineChart = view.findViewById(R.id.weeklyLineChart);
        academicCalendarView = view.findViewById(R.id.academicCalendarView);

        calendarSummaryText = view.findViewById(R.id.calendarSummaryText);
        selectedDateTitleText = view.findViewById(R.id.selectedDateTitleText);
        selectedDateEventsText = view.findViewById(R.id.selectedDateEventsText);
        btnToggleCalendar = view.findViewById(R.id.btnToggleCalendar);
        calendarBodyContainer = view.findViewById(R.id.calendarBodyContainer);

        setupGreeting();
        setupSubjectDropdown();
        refreshHomeData();
        updateTimerText();
        updateWeeklyTrendChart();
        loadCalendarDecorators();
        updateCalendarSummary();
        updateCalendarToggleIcon();

        btnToggleCalendar.setOnClickListener(v -> toggleCalendar());

        academicCalendarView.setOnDateChangedListener((widget, date, selected) -> {
            String selectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    date.getYear(),
                    date.getMonth(),
                    date.getDay()
            );
            showEventsForDate(selectedDate);
        });

        view.findViewById(R.id.btnStartFocus).setOnClickListener(v -> {
            if (!timerRunning) {
                startTimer();
            }
        });

        view.findViewById(R.id.btnStopFocus).setOnClickListener(v -> {
            if (timerRunning) {
                stopAndSaveSession();
            }
        });

        view.findViewById(R.id.btnAddExam).setOnClickListener(v -> showAddExamDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHomeData();
        updateWeeklyTrendChart();
        loadCalendarDecorators();
        updateCalendarSummary();
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning" : hour < 18 ? "Good Afternoon" : "Good Evening";
        greetingText.setText(greeting);

        SharedPreferences preferences = requireActivity()
                .getSharedPreferences("StudySmartPrefs", Context.MODE_PRIVATE);

        String loggedInEmail = preferences.getString("loggedInEmail", null);

        if (loggedInEmail != null) {
            String userName = databaseHelper.getUserNameByEmail(loggedInEmail);
            nameText.setText((userName != null && !userName.isEmpty()) ? "Hi " + userName + "! 👋" : "Hi User! 👋");
        } else {
            nameText.setText("Hi User! 👋");
        }
    }

    private void setupSubjectDropdown() {
        ArrayList<String> subjects = databaseHelper.getAllTaskCategories();

        if (subjects.isEmpty()) {
            subjects.add("Study");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                subjects
        );

        subjectDropdown.setAdapter(adapter);
        subjectDropdown.setText(subjects.get(0), false);
    }

    private void refreshHomeData() {
        int totalSessions = databaseHelper.getTotalStudySessions();
        int totalStudyMinutes = databaseHelper.getTotalStudyMinutes();
        String lastSessionDate = databaseHelper.getLastStudySessionDate();

        int subjectCount = databaseHelper.getDistinctSubjectCount();

        int pendingCount = databaseHelper.getTaskCountByStatus("Pending");
        int inProgressCount = databaseHelper.getTaskCountByStatus("In Progress");
        int completedCount = databaseHelper.getTaskCountByStatus("Completed");
        int totalTasks = pendingCount + inProgressCount + completedCount;

        int productivity = 0;
        if (totalTasks > 0) {
            productivity = Math.round((completedCount * 100f) / totalTasks);
        }

        cardStudyTimeText.setText(totalStudyMinutes + " min");
        cardSubjectsText.setText(String.valueOf(subjectCount));
        cardProductivityText.setText(productivity + "%");

        sessionTotalValue.setText(String.valueOf(totalSessions));
        sessionMinutesValue.setText(String.valueOf(totalStudyMinutes));
        sessionLastValue.setText(lastSessionDate != null ? lastSessionDate : "none");

        subGreetingText.setText("You've studied " + totalStudyMinutes + " min in total.");

        streakChipText.setText(databaseHelper.getNextExamDaysText());

        Exam nearestExam = databaseHelper.getNearestUpcomingExam();
        if (nearestExam != null) {
            cardNextExamText.setText(getDaysUntilExam(nearestExam.getExamDate()));
            cardNextExamTitleText.setText(nearestExam.getTitle());
        } else {
            cardNextExamText.setText("No exam");
            cardNextExamTitleText.setText("Add one");
        }
    }

    private void toggleCalendar() {
        isCalendarExpanded = !isCalendarExpanded;
        calendarBodyContainer.setVisibility(isCalendarExpanded ? View.VISIBLE : View.GONE);
        updateCalendarToggleIcon();

        if (isCalendarExpanded) {
            Calendar calendar = Calendar.getInstance();
            String today = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            showEventsForDate(today);
        }
    }

    private void updateCalendarToggleIcon() {
        if (btnToggleCalendar == null) return;
        btnToggleCalendar.setImageResource(
                isCalendarExpanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float
        );
    }

    private void updateCalendarSummary() {
        ArrayList<CalendarEvent> events = databaseHelper.getAllCalendarEvents();

        int exams = 0;
        int pending = 0;
        int completed = 0;

        for (CalendarEvent event : events) {
            if ("exam".equalsIgnoreCase(event.getType())) {
                exams++;
            } else if ("Completed".equalsIgnoreCase(event.getStatus())) {
                completed++;
            } else {
                pending++;
            }
        }

        calendarSummaryText.setText(exams + " exams • " + pending + " pending tasks • " + completed + " completed");
    }

    private void showEventsForDate(String dateString) {
        ArrayList<CalendarEvent> events = databaseHelper.getAllCalendarEvents();
        StringBuilder builder = new StringBuilder();
        int count = 0;

        for (CalendarEvent event : events) {
            if (dateString.equals(event.getDate())) {
                count++;

                if ("exam".equalsIgnoreCase(event.getType())) {
                    builder.append("Exam: ").append(event.getTitle()).append("\n");
                } else if ("Completed".equalsIgnoreCase(event.getStatus())) {
                    builder.append("Completed Task: ").append(event.getTitle()).append("\n");
                } else {
                    builder.append("Pending Task: ").append(event.getTitle()).append("\n");
                }
            }
        }

        selectedDateTitleText.setText("Agenda for " + dateString);

        if (count == 0) {
            selectedDateEventsText.setText("No events for this date");
        } else {
            selectedDateEventsText.setText(builder.toString().trim());
        }
    }

    private void loadCalendarDecorators() {
        academicCalendarView.removeDecorators();

        ArrayList<CalendarEvent> events = databaseHelper.getAllCalendarEvents();

        HashSet<CalendarDay> pendingDays = new HashSet<>();
        HashSet<CalendarDay> completedDays = new HashSet<>();
        HashSet<CalendarDay> examDays = new HashSet<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (CalendarEvent event : events) {
            try {
                Date parsedDate = sdf.parse(event.getDate());
                if (parsedDate == null) continue;

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsedDate);

                CalendarDay day = CalendarDay.from(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                if ("exam".equalsIgnoreCase(event.getType())) {
                    examDays.add(day);
                } else if ("Completed".equalsIgnoreCase(event.getStatus())) {
                    completedDays.add(day);
                } else {
                    pendingDays.add(day);
                }
            } catch (Exception ignored) {
            }
        }

        if (!pendingDays.isEmpty()) {
            academicCalendarView.addDecorator(new EventDecorator(pendingDays, Color.parseColor("#2D6AE3")));
        }

        if (!completedDays.isEmpty()) {
            academicCalendarView.addDecorator(new EventDecorator(completedDays, Color.parseColor("#2FA56C")));
        }

        if (!examDays.isEmpty()) {
            academicCalendarView.addDecorator(new EventDecorator(examDays, Color.parseColor("#F26A21")));
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
            long days = diffMillis / (24L * 60L * 60L * 1000L);

            if (days <= 0) return "Today";
            return days + "d";
        } catch (Exception e) {
            return "No exam";
        }
    }

    private void showAddExamDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_exam, null);

        EditText editExamTitle = dialogView.findViewById(R.id.editExamTitle);
        EditText editExamDate = dialogView.findViewById(R.id.editExamDate);

        editExamDate.setFocusable(false);
        editExamDate.setClickable(true);
        editExamDate.setOnClickListener(v -> showExamDatePicker(editExamDate));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editExamTitle.getText().toString().trim();
                    String date = editExamDate.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date)) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean saved = databaseHelper.insertOrUpdateExam(title, date);
                    if (saved) {
                        refreshHomeData();
                        loadCalendarDecorators();
                        updateCalendarSummary();
                        Toast.makeText(requireContext(), "Exam saved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Could not save exam", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showExamDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );
                    target.setText(formatted);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
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
                saveStudySession(25);
                timeLeftInMillis = 25 * 60 * 1000L;
                updateTimerText();
            }
        }.start();
    }

    private void stopAndSaveSession() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timerRunning = false;

        long elapsedMillis = System.currentTimeMillis() - sessionStartMillis;
        int durationMinutes = (int) (elapsedMillis / 60000L);
        if (durationMinutes <= 0) {
            durationMinutes = 1;
        }

        saveStudySession(durationMinutes);
        timeLeftInMillis = 25 * 60 * 1000L;
        updateTimerText();
    }

    private void saveStudySession(int durationMinutes) {
        String sessionDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        databaseHelper.insertStudySession(durationMinutes, sessionDate);
        refreshHomeData();
        updateWeeklyTrendChart();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000L) / 60;
        int seconds = (int) (timeLeftInMillis / 1000L) % 60;
        focusTimerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateWeeklyTrendChart() {
        int[] currentWeek = new int[7];
        int[] previousWeek = new int[7];

        ArrayList<StudySession> sessions = databaseHelper.getAllStudySessions();

        Calendar startOfThisWeek = getStartOfWeek(0);
        Calendar startOfNextWeek = (Calendar) startOfThisWeek.clone();
        startOfNextWeek.add(Calendar.DAY_OF_YEAR, 7);

        Calendar startOfPrevWeek = (Calendar) startOfThisWeek.clone();
        startOfPrevWeek.add(Calendar.DAY_OF_YEAR, -7);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (StudySession session : sessions) {
            try {
                Date sessionDate = sdf.parse(session.getSessionDate());
                if (sessionDate == null) continue;

                Calendar sessionCal = Calendar.getInstance();
                sessionCal.setTime(sessionDate);
                long millis = sessionCal.getTimeInMillis();

                if (millis >= startOfThisWeek.getTimeInMillis() && millis < startOfNextWeek.getTimeInMillis()) {
                    int index = mapDay(sessionCal.get(Calendar.DAY_OF_WEEK));
                    currentWeek[index] += session.getDurationMinutes();
                } else if (millis >= startOfPrevWeek.getTimeInMillis() && millis < startOfThisWeek.getTimeInMillis()) {
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

        weeklyLineChart.setData(new LineData(dataSet));

        Description description = new Description();
        description.setText("");
        weeklyLineChart.setDescription(description);
        weeklyLineChart.setDrawGridBackground(false);
        weeklyLineChart.setTouchEnabled(false);

        Legend legend = weeklyLineChart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = weeklyLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"M", "T", "W", "T", "F", "S", "S"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setAxisLineColor(Color.TRANSPARENT);

        YAxis leftAxis = weeklyLineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setGridColor(Color.parseColor("#E9E9EE"));
        leftAxis.setAxisLineColor(Color.TRANSPARENT);

        YAxis rightAxis = weeklyLineChart.getAxisRight();
        rightAxis.setEnabled(false);

        weeklyLineChart.invalidate();

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

        trendPillText.setText(trendText);
    }

    private Calendar getStartOfWeek(int weekOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}