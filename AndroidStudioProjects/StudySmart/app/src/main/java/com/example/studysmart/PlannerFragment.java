package com.example.studysmart;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PlannerFragment extends Fragment {

    private DatabaseHelper databaseHelper;
    private TaskAdapter taskAdapter;
    private ArrayList<Task> taskList;

    public PlannerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_planner, container, false);

        databaseHelper = new DatabaseHelper(getContext());
        RecyclerView recyclerTasks = view.findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        taskList = databaseHelper.getAllTasks();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.TaskActionListener() {
            @Override
            public void onComplete(Task task) {
                boolean updated = databaseHelper.updateTaskStatus(task.getId(), "Completed");
                if (updated) {
                    loadTasks();
                    Toast.makeText(getContext(), "Task completed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(Task task) {
                boolean deleted = databaseHelper.deleteTask(task.getId());
                if (deleted) {
                    loadTasks();
                    Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerTasks.setAdapter(taskAdapter);

        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        taskList = databaseHelper.getAllTasks();
        taskAdapter.updateTasks(taskList);
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);

        EditText editTaskTitle = dialogView.findViewById(R.id.editTaskTitle);
        EditText editTaskCategory = dialogView.findViewById(R.id.editTaskCategory);
        EditText editTaskDueDate = dialogView.findViewById(R.id.editTaskDueDate);

        editTaskDueDate.setOnClickListener(v -> showDatePicker(editTaskDueDate));

        new AlertDialog.Builder(getContext())
                .setTitle("Add Task")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editTaskTitle.getText().toString().trim();
                    String category = editTaskCategory.getText().toString().trim();
                    String dueDate = editTaskDueDate.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(category) || TextUtils.isEmpty(dueDate)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean inserted = databaseHelper.insertTask(title, category, "Pending", dueDate);
                    if (inserted) {
                        loadTasks();
                        Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    target.setText(formatted);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }
}
