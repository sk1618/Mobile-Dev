package com.example.studysmart;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onComplete(Task task);
        void onDelete(Task task);
    }

    private ArrayList<Task> taskList;
    private final TaskActionListener listener;

    public TaskAdapter(ArrayList<Task> taskList, TaskActionListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    public void updateTasks(ArrayList<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        String title = task.getTitle();
        String shortTitle = title;

        if (title != null && title.length() > 65) {
            shortTitle = title.substring(0, 65) + "...";
        }

        holder.taskTitle.setText(shortTitle);
        holder.taskCategory.setText(task.getCategory() == null ? "No subject" : task.getCategory());
        holder.taskStatus.setText("Status: " + task.getStatus());
        holder.taskDueDate.setText(
                task.getDueDate() == null || task.getDueDate().isEmpty()
                        ? "No due date"
                        : "Due: " + task.getDueDate()
        );

        holder.btnDescriptionTask.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Task Description")
                    .setMessage(task.getTitle())
                    .setPositiveButton("Close", null)
                    .show();
        });

        holder.btnComplete.setOnClickListener(v -> listener.onComplete(task));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(task));
    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskCategory, taskStatus, taskDueDate;
        Button btnDescriptionTask, btnComplete, btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskCategory = itemView.findViewById(R.id.taskCategory);
            taskStatus = itemView.findViewById(R.id.taskStatus);
            taskDueDate = itemView.findViewById(R.id.taskDueDate);
            btnDescriptionTask = itemView.findViewById(R.id.btnDescriptionTask);
            btnComplete = itemView.findViewById(R.id.btnCompleteTask);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}
