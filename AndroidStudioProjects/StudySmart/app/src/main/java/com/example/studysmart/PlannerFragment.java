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

public class PlannerFragment extends Fragment {

    private LinearLayout tasksContainer;
    private DatabaseHelper databaseHelper;

    private TextView tabPending;
    private TextView tabInProgress;
    private TextView tabCompleted;

    private String currentStatus = "Pending";

    public PlannerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_planner, container, false);

        tasksContainer = view.findViewById(R.id.tasksContainer);
        tabPending = view.findViewById(R.id.tabPending);
        tabInProgress = view.findViewById(R.id.tabInProgress);
        tabCompleted = view.findViewById(R.id.tabCompleted);

        databaseHelper = new DatabaseHelper(getContext());

        loadTasksByStatus(currentStatus);
        updateTabStyles();

        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());

        tabPending.setOnClickListener(v -> {
            currentStatus = "Pending";
            updateTabStyles();
            loadTasksByStatus(currentStatus);
        });

        tabInProgress.setOnClickListener(v -> {
            currentStatus = "In Progress";
            updateTabStyles();
            loadTasksByStatus(currentStatus);
        });

        tabCompleted.setOnClickListener(v -> {
            currentStatus = "Completed";
            updateTabStyles();
            loadTasksByStatus(currentStatus);
        });

        return view;
    }

    private void showAddTaskDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);

        EditText editTaskTitle = dialogView.findViewById(R.id.editTaskTitle);
        EditText editTaskCategory = dialogView.findViewById(R.id.editTaskCategory);
        EditText editTaskPriority = dialogView.findViewById(R.id.editTaskPriority);

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = editTaskTitle.getText().toString().trim();
                    String category = editTaskCategory.getText().toString().trim();
                    String priority = editTaskPriority.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(category) || TextUtils.isEmpty(priority)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean inserted = databaseHelper.insertTask(title, category, priority);

                    if (inserted) {
                        currentStatus = "Pending";
                        updateTabStyles();
                        loadTasksByStatus(currentStatus);
                        Toast.makeText(getContext(), "Task saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to save task", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTasksByStatus(String status) {
        tasksContainer.removeAllViews();

        ArrayList<Task> taskList = databaseHelper.getTasksByStatus(status);

        if (taskList.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No " + status.toLowerCase() + " tasks yet");
            emptyText.setTextSize(16);
            emptyText.setTextColor(0xFF8B8E99);
            emptyText.setPadding(0, dpToPx(20), 0, 0);
            tasksContainer.addView(emptyText);
            return;
        }

        for (Task task : taskList) {
            addTaskCard(task);
        }
    }

    private void addTaskCard(Task task) {
        LinearLayout card = new LinearLayout(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(14);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        card.setBackgroundResource(R.drawable.planner_task_card);
        card.setElevation(dpToPx(4));

        TextView titleView = new TextView(getContext());
        titleView.setText(task.getTitle());
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));

        TextView categoryView = new TextView(getContext());
        categoryView.setText(task.getCategory() + " • " + task.getStatus());
        categoryView.setTextSize(14);
        categoryView.setTextColor(0xFF8B8E99);
        categoryView.setPadding(0, dpToPx(8), 0, 0);

        TextView priorityView = new TextView(getContext());
        priorityView.setText(task.getPriority() + " Priority");
        priorityView.setTextSize(13);
        priorityView.setTypeface(null, Typeface.BOLD);
        priorityView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        priorityView.setBackgroundResource(R.drawable.pill_light);
        priorityView.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        LinearLayout.LayoutParams priorityParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        priorityParams.topMargin = dpToPx(10);
        priorityView.setLayoutParams(priorityParams);

        LinearLayout actionsRow = new LinearLayout(getContext());
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dpToPx(14);
        actionsRow.setLayoutParams(actionsParams);

        ButtonLikeTextView deleteBtn = new ButtonLikeTextView(getContext());
        deleteBtn.setText("Delete");
        deleteBtn.setBackgroundResource(R.drawable.pill_light);
        deleteBtn.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
        deleteBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        deleteBtn.setTypeface(null, Typeface.BOLD);

        deleteBtn.setOnClickListener(v -> {
            boolean deleted = databaseHelper.deleteTask(task.getId());
            if (deleted) {
                loadTasksByStatus(currentStatus);
                Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
            }
        });

        actionsRow.addView(deleteBtn);

        if (!task.getStatus().equals("Completed")) {
            ButtonLikeTextView completeBtn = new ButtonLikeTextView(getContext());
            LinearLayout.LayoutParams completeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            completeParams.leftMargin = dpToPx(10);
            completeBtn.setLayoutParams(completeParams);

            completeBtn.setText(task.getStatus().equals("Pending") ? "Mark Complete" : "Set Pending");
            completeBtn.setBackgroundResource(R.drawable.button_dark_rounded);
            completeBtn.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
            completeBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            completeBtn.setTypeface(null, Typeface.BOLD);

            completeBtn.setOnClickListener(v -> {
                boolean updated = databaseHelper.updateTaskStatus(task.getId(), "Completed");
                if (updated) {
                    loadTasksByStatus(currentStatus);
                    Toast.makeText(getContext(), "Task completed", Toast.LENGTH_SHORT).show();
                }
            });

            actionsRow.addView(completeBtn);
        }

        if (task.getStatus().equals("Completed")) {
            ButtonLikeTextView pendingBtn = new ButtonLikeTextView(getContext());
            LinearLayout.LayoutParams pendingParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            pendingParams.leftMargin = dpToPx(10);
            pendingBtn.setLayoutParams(pendingParams);

            pendingBtn.setText("Move to Pending");
            pendingBtn.setBackgroundResource(R.drawable.button_dark_rounded);
            pendingBtn.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
            pendingBtn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            pendingBtn.setTypeface(null, Typeface.BOLD);

            pendingBtn.setOnClickListener(v -> {
                boolean updated = databaseHelper.updateTaskStatus(task.getId(), "Pending");
                if (updated) {
                    loadTasksByStatus(currentStatus);
                    Toast.makeText(getContext(), "Task moved to pending", Toast.LENGTH_SHORT).show();
                }
            });

            actionsRow.addView(pendingBtn);
        }

        card.addView(titleView);
        card.addView(categoryView);
        card.addView(priorityView);
        card.addView(actionsRow);

        tasksContainer.addView(card);
    }

    private void updateTabStyles() {
        resetTabStyle(tabPending);
        resetTabStyle(tabInProgress);
        resetTabStyle(tabCompleted);

        if (currentStatus.equals("Pending")) {
            setSelectedTabStyle(tabPending);
        } else if (currentStatus.equals("In Progress")) {
            setSelectedTabStyle(tabInProgress);
        } else if (currentStatus.equals("Completed")) {
            setSelectedTabStyle(tabCompleted);
        }
    }

    private void resetTabStyle(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(0xFF7C7F8A);
    }

    private void setSelectedTabStyle(TextView tab) {
        tab.setBackgroundResource(R.drawable.planner_tab_selected);
        tab.setTextColor(0xFFFFFFFF);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class ButtonLikeTextView extends androidx.appcompat.widget.AppCompatTextView {
        public ButtonLikeTextView(android.content.Context context) {
            super(context);
            setClickable(true);
            setFocusable(true);
        }
    }
}