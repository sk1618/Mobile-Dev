package com.example.studysmart;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class AlertsFragment extends Fragment {

    private LinearLayout remindersContainer;
    private DatabaseHelper databaseHelper;

    private TextView filterAll;
    private TextView filterPending;
    private TextView filterDone;
    private TextView alertsTotalText;
    private TextView alertsCompletedText;

    private String currentFilter = "All";

    public AlertsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        remindersContainer = view.findViewById(R.id.remindersContainer);
        filterAll = view.findViewById(R.id.filterAll);
        filterPending = view.findViewById(R.id.filterPending);
        filterDone = view.findViewById(R.id.filterDone);
        alertsTotalText = view.findViewById(R.id.alertsTotalText);
        alertsCompletedText = view.findViewById(R.id.alertsCompletedText);

        databaseHelper = new DatabaseHelper(getContext());

        updateSummary();
        loadRemindersByFilter(currentFilter);
        updateFilterStyles();

        view.findViewById(R.id.btnAddReminder).setOnClickListener(v -> showAddReminderDialog());

        filterAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterStyles();
            loadRemindersByFilter(currentFilter);
        });

        filterPending.setOnClickListener(v -> {
            currentFilter = "Pending";
            updateFilterStyles();
            loadRemindersByFilter(currentFilter);
        });

        filterDone.setOnClickListener(v -> {
            currentFilter = "Done";
            updateFilterStyles();
            loadRemindersByFilter(currentFilter);
        });

        return view;
    }

    private void showAddReminderDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_reminder, null);

        EditText editReminderTitle = dialogView.findViewById(R.id.editReminderTitle);
        EditText editReminderDate = dialogView.findViewById(R.id.editReminderDate);
        EditText editReminderTime = dialogView.findViewById(R.id.editReminderTime);

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = editReminderTitle.getText().toString().trim();
                    String date = editReminderDate.getText().toString().trim();
                    String time = editReminderTime.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean inserted = databaseHelper.insertReminder(title, date, time);

                    if (inserted) {
                        updateSummary();
                        loadRemindersByFilter(currentFilter);
                        Toast.makeText(getContext(), "Reminder added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to add reminder", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSummary() {
        alertsTotalText.setText(String.valueOf(databaseHelper.getReminderCount()));
        alertsCompletedText.setText(String.valueOf(databaseHelper.getCompletedReminderCount()));
    }

    private void loadRemindersByFilter(String filter) {
        remindersContainer.removeAllViews();

        ArrayList<Reminder> reminderList = databaseHelper.getRemindersByFilter(filter);

        if (reminderList.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No reminders found for this filter");
            emptyText.setTextSize(16);
            emptyText.setTextColor(0xFF8B8E99);
            emptyText.setPadding(0, dpToPx(20), 0, 0);
            remindersContainer.addView(emptyText);
            return;
        }

        for (Reminder reminder : reminderList) {
            addReminderCard(reminder);
        }
    }

    private void addReminderCard(Reminder reminder) {
        LinearLayout card = new LinearLayout(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(14);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        card.setBackgroundResource(R.drawable.card_white);
        card.setElevation(dpToPx(4));

        TextView titleView = new TextView(getContext());
        titleView.setText(reminder.getTitle());
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));

        if (reminder.getIsDone() == 1) {
            titleView.setAlpha(0.6f);
        }

        TextView dateTimeView = new TextView(getContext());
        dateTimeView.setText(reminder.getDate() + " • " + reminder.getTime());
        dateTimeView.setTextSize(14);
        dateTimeView.setTextColor(0xFF8B8E99);
        dateTimeView.setPadding(0, dpToPx(8), 0, 0);

        TextView statusBadge = new TextView(getContext());
        statusBadge.setText(reminder.getIsDone() == 1 ? "Done" : "Pending");
        statusBadge.setTextSize(13);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        statusBadge.setBackgroundResource(R.drawable.pill_light);
        statusBadge.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.topMargin = dpToPx(10);
        statusBadge.setLayoutParams(badgeParams);

        LinearLayout actionsRow = new LinearLayout(getContext());
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dpToPx(14);
        actionsRow.setLayoutParams(actionsParams);

        TextView deleteBtn = new TextView(getContext());
        deleteBtn.setText("Delete");
        deleteBtn.setTextSize(14);
        deleteBtn.setTypeface(null, Typeface.BOLD);
        deleteBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        deleteBtn.setBackgroundResource(R.drawable.pill_light);
        deleteBtn.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        deleteBtn.setOnClickListener(v -> {
            boolean deleted = databaseHelper.deleteReminder(reminder.getId());
            if (deleted) {
                updateSummary();
                loadRemindersByFilter(currentFilter);
                Toast.makeText(getContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
            }
        });

        actionsRow.addView(deleteBtn);

        TextView toggleDoneBtn = new TextView(getContext());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        doneParams.leftMargin = dpToPx(10);
        toggleDoneBtn.setLayoutParams(doneParams);

        toggleDoneBtn.setText(reminder.getIsDone() == 1 ? "Mark Pending" : "Mark Done");
        toggleDoneBtn.setTextSize(14);
        toggleDoneBtn.setTypeface(null, Typeface.BOLD);
        toggleDoneBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        toggleDoneBtn.setBackgroundResource(R.drawable.button_dark_rounded);
        toggleDoneBtn.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        toggleDoneBtn.setOnClickListener(v -> {
            int newStatus = reminder.getIsDone() == 1 ? 0 : 1;
            boolean updated = databaseHelper.updateReminderDoneStatus(reminder.getId(), newStatus);
            if (updated) {
                updateSummary();
                loadRemindersByFilter(currentFilter);
                Toast.makeText(getContext(), "Reminder updated", Toast.LENGTH_SHORT).show();
            }
        });

        actionsRow.addView(toggleDoneBtn);

        card.addView(titleView);
        card.addView(dateTimeView);
        card.addView(statusBadge);
        card.addView(actionsRow);

        remindersContainer.addView(card);
    }

    private void updateFilterStyles() {
        resetFilterStyle(filterAll);
        resetFilterStyle(filterPending);
        resetFilterStyle(filterDone);

        if (currentFilter.equals("All")) {
            setSelectedFilterStyle(filterAll);
        } else if (currentFilter.equals("Pending")) {
            setSelectedFilterStyle(filterPending);
        } else {
            setSelectedFilterStyle(filterDone);
        }
    }

    private void resetFilterStyle(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(0xFF7C7F8A);
    }

    private void setSelectedFilterStyle(TextView tab) {
        tab.setTextColor(0xFFFFFFFF);

        if (tab == filterPending) {
            tab.setBackgroundResource(R.drawable.tab_pending_bg);
        } else if (tab == filterDone) {
            tab.setBackgroundResource(R.drawable.tab_done_bg);
        } else {
            tab.setBackgroundResource(R.drawable.planner_tab_selected);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}