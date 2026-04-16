package com.example.studysmart;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class AiCoachFragment extends Fragment {

    private EditText etAiPrompt, etPlanSubject, etPlanExamDate, etPlanDifficulty, etPlanTopics, etPlanConfidence;
    private LinearLayout chatContainer;
    private NestedScrollView chatScrollView;
    private TextView tvSelectedPdf;
    private LinearLayout plusMenuLayout;
    private View studyPlanOverlay;

    private Uri selectedPdfUri = null;
    private String selectedPdfName = "No PDF selected";

    private ActivityResultLauncher<String> pdfPickerLauncher;

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
                        tvSelectedPdf.setText("PDF attached: " + selectedPdfName);
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
        etPlanExamDate = view.findViewById(R.id.etPlanExamDate);
        etPlanDifficulty = view.findViewById(R.id.etPlanDifficulty);
        etPlanTopics = view.findViewById(R.id.etPlanTopics);
        etPlanConfidence = view.findViewById(R.id.etPlanConfidence);

        chatContainer = view.findViewById(R.id.chatContainer);
        chatScrollView = view.findViewById(R.id.chatScrollView);
        tvSelectedPdf = view.findViewById(R.id.tvSelectedPdf);
        plusMenuLayout = view.findViewById(R.id.plusMenuLayout);
        studyPlanOverlay = view.findViewById(R.id.studyPlanOverlay);

        addAiMessage("Hi! I’m StudySmart AI. Ask me any study question, attach a lecture PDF, or use the + button for more actions.");

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

    private void generateStudyPlan() {
        String subject = etPlanSubject.getText().toString().trim();
        String examDate = etPlanExamDate.getText().toString().trim();
        String difficulty = etPlanDifficulty.getText().toString().trim();
        String topics = etPlanTopics.getText().toString().trim();
        String confidence = etPlanConfidence.getText().toString().trim();

        if (TextUtils.isEmpty(subject) || TextUtils.isEmpty(examDate)) {
            Toast.makeText(getContext(), "Subject and exam date are required", Toast.LENGTH_SHORT).show();
            return;
        }

        addUserMessage("Generate a smart study plan for " + subject + " (exam: " + examDate + ").");
        studyPlanOverlay.setVisibility(View.GONE);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://10.0.2.2:5000/generate-study-plan");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("subject", subject);
                jsonObject.put("exam_date", examDate);
                jsonObject.put("difficulty", difficulty);
                jsonObject.put("topics_left", topics);
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
                    String plan = result.getString("plan");

                    requireActivity().runOnUiThread(() -> {
                        addAiMessage(plan);
                        etPlanSubject.setText("");
                        etPlanExamDate.setText("");
                        etPlanDifficulty.setText("");
                        etPlanTopics.setText("");
                        etPlanConfidence.setText("");
                    });
                } else {
                    String errorMessage = "I couldn’t generate the study plan right now.";
                    try {
                        JSONObject result = new JSONObject(response.toString());
                        if (result.has("error")) {
                            errorMessage = result.getString("error");
                        }
                    } catch (Exception ignored) {
                    }

                    String finalError = errorMessage;
                    requireActivity().runOnUiThread(() ->
                            addAiMessage("Backend error: " + finalError)
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        addAiMessage("Couldn’t reach the backend right now. Please make sure the backend is running.")
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void askStudyBackend(String prompt) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://10.0.2.2:5000/ask-study");

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
                    String answer = result.getString("answer");

                    requireActivity().runOnUiThread(() -> addAiMessage(answer));
                } else {
                    String errorMessage = "I couldn’t get a study answer right now.";
                    try {
                        JSONObject result = new JSONObject(response.toString());
                        if (result.has("error")) {
                            errorMessage = result.getString("error");
                        }
                    } catch (Exception ignored) {
                    }

                    String finalError = errorMessage;
                    requireActivity().runOnUiThread(() ->
                            addAiMessage("Backend error: " + finalError)
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        addAiMessage("Couldn’t reach the backend right now. Please make sure the backend is running.")
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
                URL url = new URL("http://10.0.2.2:5000/summarize-pdf");
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
                    String summary = result.getString("summary");

                    requireActivity().runOnUiThread(() -> {
                        addAiMessage(summary);
                        selectedPdfUri = null;
                        selectedPdfName = "No PDF selected";
                        tvSelectedPdf.setText("No PDF selected");
                    });
                } else {
                    String errorMessage = "I couldn’t summarize that PDF right now.";
                    try {
                        JSONObject result = new JSONObject(response.toString());
                        if (result.has("error")) {
                            errorMessage = result.getString("error");
                        }
                    } catch (Exception ignored) {
                    }

                    String finalError = errorMessage;
                    requireActivity().runOnUiThread(() ->
                            addAiMessage("Backend error: " + finalError)
                    );
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        addAiMessage("Couldn’t reach the backend right now. Please make sure the backend is running.")
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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
        bubble.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        bubble.setMaxWidth(dpToPx(280));
        bubble.setLineSpacing(0f, 1.15f);

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
