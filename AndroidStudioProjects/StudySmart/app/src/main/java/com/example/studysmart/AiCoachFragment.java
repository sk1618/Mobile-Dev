package com.example.studysmart;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AiCoachFragment extends Fragment {

    private static final String BASE_URL = "http://10.0.2.2:5000";

    private EditText etAiPrompt;
    private EditText etPlanPrepStartDate;
    private EditText etPlanExamDate;
    private EditText etPlanCoverage;
    private EditText etPlanWeakTopics;
    private EditText etPlanConfidence;

    private MaterialAutoCompleteTextView etPlanSubject;

    private LinearLayout chatContainer;
    private NestedScrollView chatScrollView;
    private TextView tvSelectedPdf;
    private LinearLayout plusMenuLayout;
    private View studyPlanOverlay;

    private Uri selectedPdfUri = null;
    private String selectedPdfName = "No PDF selected";

    private ActivityResultLauncher<String> pdfPickerLauncher;
    private DatabaseHelper databaseHelper;

    public AiCoachFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPdfUri = uri;
                        selectedPdfName = getFileName(uri);
                        if (tvSelectedPdf != null) {
                            tvSelectedPdf.setText("PDF attached: " + selectedPdfName);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ai_coach, container, false);

        etAiPrompt = view.findViewById(R.id.etAiPrompt);
        etPlanSubject = view.findViewById(R.id.etPlanSubject);
        etPlanPrepStartDate = view.findViewById(R.id.etPlanPrepStartDate);
        etPlanExamDate = view.findViewById(R.id.etPlanExamDate);
        etPlanCoverage = view.findViewById(R.id.etPlanCoverage);
        etPlanWeakTopics = view.findViewById(R.id.etPlanWeakTopics);
        etPlanConfidence = view.findViewById(R.id.etPlanConfidence);

        chatContainer = view.findViewById(R.id.chatContainer);
        chatScrollView = view.findViewById(R.id.chatScrollView);
        tvSelectedPdf = view.findViewById(R.id.tvSelectedPdf);
        plusMenuLayout = view.findViewById(R.id.plusMenuLayout);
        studyPlanOverlay = view.findViewById(R.id.studyPlanOverlay);

        databaseHelper = new DatabaseHelper(requireContext());

        setupSubjectDropdown();

        addAiMessage("Hi! I’m StudySmart AI. Ask me any study question, attach a lecture PDF, or use the + button for more actions.");

        etPlanPrepStartDate.setOnClickListener(v -> showDatePicker(etPlanPrepStartDate));
        etPlanExamDate.setOnClickListener(v -> showDatePicker(etPlanExamDate));

        view.findViewById(R.id.btnPlusMenu).setOnClickListener(v -> {
            if (plusMenuLayout.getVisibility() == View.VISIBLE) {
                plusMenuLayout.setVisibility(View.GONE);
            } else {
                plusMenuLayout.setVisibility(View.VISIBLE);
            }
        });

        view.findViewById(R.id.menuChoosePdf).setOnClickListener(v -> {
            plusMenuLayout.setVisibility(View.GONE);
            pdfPickerLauncher.launch("application/pdf");
        });

        view.findViewById(R.id.menuGeneratePlan).setOnClickListener(v -> {
            plusMenuLayout.setVisibility(View.GONE);
            studyPlanOverlay.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.btnClosePlanOverlay).setOnClickListener(v ->
                studyPlanOverlay.setVisibility(View.GONE));

        view.findViewById(R.id.btnGenerateStudyPlan).setOnClickListener(v -> generateStudyPlan());

        view.findViewById(R.id.btnSendAi).setOnClickListener(v -> {
            String prompt = etAiPrompt.getText().toString().trim();

            if (selectedPdfUri != null) {
                addUserMessage("Please summarize this lecture PDF.");
                uploadPdfAndSummarize();
                etAiPrompt.setText("");
                return;
            }

            if (TextUtils.isEmpty(prompt)) {
                Toast.makeText(getContext(), "Please type a message first", Toast.LENGTH_SHORT).show();
                return;
            }

            addUserMessage(prompt);
            etAiPrompt.setText("");
            askStudyBackend(prompt);
        });

        return view;
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

        etPlanSubject.setAdapter(adapter);
        etPlanSubject.setText(subjects.get(0), false);
    }

    private void showDatePicker(EditText target) {
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

    private void generateStudyPlan() {
        String subject = etPlanSubject.getText().toString().trim();
        String prepStartDate = etPlanPrepStartDate.getText().toString().trim();
        String examDate = etPlanExamDate.getText().toString().trim();
        String studyScope = etPlanCoverage.getText().toString().trim();
        String weakTopics = etPlanWeakTopics.getText().toString().trim();
        String confidence = etPlanConfidence.getText().toString().trim();

        if (TextUtils.isEmpty(subject)
                || TextUtils.isEmpty(prepStartDate)
                || TextUtils.isEmpty(examDate)
                || TextUtils.isEmpty(studyScope)) {

            Toast.makeText(
                    getContext(),
                    "Subject, preparation start date, exam date, and study scope are required",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        addUserMessage("Generate a smart study plan for " + subject + " from " + prepStartDate + " until " + examDate + ".");
        studyPlanOverlay.setVisibility(View.GONE);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "/generate-study-plan");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("subject", subject);
                jsonObject.put("prep_start_date", prepStartDate);
                jsonObject.put("exam_date", examDate);
                jsonObject.put("study_scope", studyScope);
                jsonObject.put("weak_topics", weakTopics);
                jsonObject.put("confidence", confidence);

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject result = new JSONObject(response.toString());
                    String rawPlan = result.getString("plan");
                    String cleanPlan = normalizeAiText(rawPlan);

                    saveStudyPlanAsTasks(subject, prepStartDate, examDate, cleanPlan);

                    requireActivity().runOnUiThread(() -> {
                        addAiMessage(cleanPlan);
                        etPlanSubject.setText("");
                        etPlanPrepStartDate.setText("");
                        etPlanExamDate.setText("");
                        etPlanCoverage.setText("");
                        etPlanWeakTopics.setText("");
                        etPlanConfidence.setText("");
                        Toast.makeText(getContext(), "Study plan added to planner and calendar", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    final String backendText = response.length() > 0
                            ? response.toString()
                            : "Backend returned code " + responseCode;

                    requireActivity().runOnUiThread(() ->
                            addAiMessage("Backend error: " + backendText)
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        addAiMessage("Couldn’t reach the backend right now. Please make sure the backend is running on 0.0.0.0:5000.")
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void saveStudyPlanAsTasks(String subject, String prepStartDate, String examDate, String cleanPlan) {
        String[] lines = cleanPlan.split("\\n");

        Calendar calendar = parseDateToCalendar(prepStartDate);
        Calendar examCal = parseDateToCalendar(examDate);

        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        boolean validExamDate = examCal != null;
        int addedCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (!trimmed.toLowerCase().startsWith("day")) continue;

            if (validExamDate && calendar.after(examCal)) break;

            String dueDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            databaseHelper.insertTask(trimmed, subject, "Pending", dueDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            addedCount++;
        }

        if (addedCount == 0) {
            String dueDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            databaseHelper.insertTask("Study plan for " + subject, subject, "Pending", dueDate);
        }
    }

    private Calendar parseDateToCalendar(String dateText) {
        try {
            String[] parts = dateText.split("-");
            if (parts.length != 3) return null;

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        } catch (Exception e) {
            return null;
        }
    }

    private void askStudyBackend(String prompt) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "/ask-study");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("prompt", prompt);

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject result = new JSONObject(response.toString());
                    String answer = normalizeAiText(result.getString("answer"));

                    requireActivity().runOnUiThread(() -> addAiMessage(answer));
                } else {
                    final String backendText = response.length() > 0
                            ? response.toString()
                            : "Backend returned code " + responseCode;

                    requireActivity().runOnUiThread(() ->
                            addAiMessage("Backend error: " + backendText)
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        addAiMessage("Couldn’t reach the backend right now. Please make sure the backend is running on 0.0.0.0:5000.")
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void uploadPdfAndSummarize() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String boundary = "----StudySmartBoundary" + UUID.randomUUID().toString().replace("-", "");
                URL url = new URL(BASE_URL + "/summarize-pdf");
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + selectedPdfName + "\"\r\n");
                request.writeBytes("Content-Type: application/pdf\r\n\r\n");

                InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedPdfUri);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    request.write(buffer, 0, bytesRead);
                }

                inputStream.close();

                request.writeBytes("\r\n");
                request.writeBytes("--" + boundary + "--\r\n");
                request.flush();
                request.close();

                int responseCode = conn.getResponseCode();

                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject result = new JSONObject(response.toString());
                    String summary = normalizeAiText(result.getString("summary"));

                    requireActivity().runOnUiThread(() -> {
                        addAiMessage(summary);
                        selectedPdfUri = null;
                        selectedPdfName = "No PDF selected";
                        tvSelectedPdf.setText("No PDF selected");
                    });
                } else {
                    final String backendText = response.length() > 0
                            ? response.toString()
                            : "Backend returned code " + responseCode;

                    requireActivity().runOnUiThread(() ->
                            addAiMessage("Backend error: " + backendText)
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        addAiMessage("Couldn’t reach the backend right now. Please make sure the backend is running on 0.0.0.0:5000.")
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String normalizeAiText(String text) {
        if (text == null) return "";

        String cleaned = text;
        cleaned = cleaned.replace("**", "");
        cleaned = cleaned.replace("###", "");
        cleaned = cleaned.replace("##", "");
        cleaned = cleaned.replace("#", "");
        cleaned = cleaned.replace("•", "-");
        cleaned = cleaned.replace("* ", "- ");
        cleaned = cleaned.replace("  ", " ");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        String[] lines = cleaned.split("\\n");
        StringBuilder builder = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                builder.append("\n");
                continue;
            }

            if (trimmed.toLowerCase().startsWith("day ")) {
                builder.append(trimmed).append("\n\n");
            } else {
                builder.append(trimmed).append("\n");
            }
        }

        return builder.toString().trim();
    }

    private void addUserMessage(String text) {
        addMessageBubble(text, true);
    }

    private void addAiMessage(String text) {
        addMessageBubble(text, false);
    }

    private void addMessageBubble(String text, boolean isUser) {
        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setGravity(isUser ? Gravity.END : Gravity.START);

        TextView bubble = new TextView(getContext());
        bubble.setText(text);
        bubble.setTextSize(15f);
        bubble.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        bubble.setMaxWidth(dpToPx(290));
        bubble.setLineSpacing(0f, 1.2f);
        bubble.setTextIsSelectable(true);

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleParams.topMargin = dpToPx(10);
        bubble.setLayoutParams(bubbleParams);

        if (isUser) {
            bubble.setBackgroundResource(R.drawable.ai_user_bubble);
            bubble.setTextColor(Color.WHITE);
        } else {
            bubble.setBackgroundResource(R.drawable.ai_bot_bubble);
            bubble.setTextColor(Color.parseColor("#111111"));
        }

        row.addView(bubble);
        chatContainer.addView(row);

        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private String getFileName(Uri uri) {
        String result = "lecture.pdf";
        try (android.database.Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
