package com.example.studysmart;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AiCoachFragment extends Fragment {

    private EditText etAiTopic;
    private TextView tvAiRecommendation;
    private LinearLayout aiHistoryContainer;
    private DatabaseHelper databaseHelper;

    private TextView tvSelectedPdf;
    private TextView tvPdfSummary;
    private Uri selectedPdfUri;

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
                        tvSelectedPdf.setText("Selected PDF ready");
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ai_coach, container, false);

        etAiTopic = view.findViewById(R.id.etAiTopic);
        tvAiRecommendation = view.findViewById(R.id.tvAiRecommendation);
        aiHistoryContainer = view.findViewById(R.id.aiHistoryContainer);
        tvSelectedPdf = view.findViewById(R.id.tvSelectedPdf);
        tvPdfSummary = view.findViewById(R.id.tvPdfSummary);

        databaseHelper = new DatabaseHelper(getContext());

        loadHistory();

        view.findViewById(R.id.chipOrganic).setOnClickListener(v ->
                etAiTopic.setText("Organic Chemistry"));

        view.findViewById(R.id.chipHistory).setOnClickListener(v ->
                etAiTopic.setText("World History"));

        view.findViewById(R.id.chipBusiness).setOnClickListener(v ->
                etAiTopic.setText("Business Law"));

        view.findViewById(R.id.btnGenerateRoadmap).setOnClickListener(v -> {
            String topic = etAiTopic.getText().toString().trim();

            if (TextUtils.isEmpty(topic)) {
                Toast.makeText(getContext(), "Please enter a subject first", Toast.LENGTH_SHORT).show();
                return;
            }

            fetchRoadmapFromBackend(topic);
        });

        view.findViewById(R.id.btnChoosePdf).setOnClickListener(v ->
                pdfPickerLauncher.launch("application/pdf"));

        view.findViewById(R.id.btnSummarizePdf).setOnClickListener(v -> {
            if (selectedPdfUri == null) {
                Toast.makeText(getContext(), "Please choose a PDF first", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadPdfAndSummarize();
        });

        return view;
    }

    private void fetchRoadmapFromBackend(String topic) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:5000/generate-roadmap");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("topic", topic);

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject result = new JSONObject(response.toString());
                String roadmap = result.getString("roadmap");

                saveRoadmap(topic, roadmap);

                requireActivity().runOnUiThread(() -> {
                    tvAiRecommendation.setText(roadmap);
                    loadHistory();
                });

            } catch (Exception e) {
                String fallbackRoadmap = buildLocalRoadmap(topic);
                String finalRoadmap = "Backend unavailable — using offline AI Coach mode.\n\n" + fallbackRoadmap;

                saveRoadmap(topic, finalRoadmap);

                requireActivity().runOnUiThread(() -> {
                    tvAiRecommendation.setText(finalRoadmap);
                    loadHistory();
                    Toast.makeText(getContext(), "Backend is off, fallback mode used", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void uploadPdfAndSummarize() {
        new Thread(() -> {
            try {
                String boundary = "----StudySmartBoundary" + UUID.randomUUID().toString().replace("-", "");
                URL url = new URL("http://10.0.2.2:5000/summarize-pdf");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                String fileName = "lecture.pdf";

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n");
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

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject result = new JSONObject(response.toString());
                String summary = result.getString("summary");

                requireActivity().runOnUiThread(() ->
                        tvPdfSummary.setText(summary)
                );

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "PDF summary failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void saveRoadmap(String topic, String roadmap) {
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        databaseHelper.insertAiRoadmap(topic, roadmap, createdAt);
    }

    private String buildLocalRoadmap(String topic) {
        String lowerTopic = topic.toLowerCase();

        if (lowerTopic.contains("chemistry")) {
            return "1) Review the key concepts of " + topic + " for 25 minutes.\n\n"
                    + "2) Focus on the most important reactions and definitions for 20 minutes.\n\n"
                    + "3) Solve 4 practice questions.\n\n"
                    + "4) Take a 10-minute break.\n\n"
                    + "5) Summarize the hardest ideas in your own words.";
        } else if (lowerTopic.contains("history")) {
            return "1) Review the main timeline of " + topic + " for 20 minutes.\n\n"
                    + "2) Highlight the major causes and consequences.\n\n"
                    + "3) Write a short summary paragraph.\n\n"
                    + "4) Take a 10-minute break.\n\n"
                    + "5) Practice one likely exam question.";
        } else if (lowerTopic.contains("law")) {
            return "1) Review the main principles of " + topic + ".\n\n"
                    + "2) Identify the most important rules and examples.\n\n"
                    + "3) Practice applying one concept to a short case.\n\n"
                    + "4) Take a 10-minute break.\n\n"
                    + "5) Recap the key legal terms.";
        } else if (lowerTopic.contains("math") || lowerTopic.contains("calculus")) {
            return "1) Review the main formulas of " + topic + " for 20 minutes.\n\n"
                    + "2) Solve 3 guided examples step by step.\n\n"
                    + "3) Solve 3 more questions on your own.\n\n"
                    + "4) Take a 10-minute break.\n\n"
                    + "5) Revisit mistakes and note the weak areas.";
        } else {
            return "1) Study " + topic + " for 25 minutes.\n\n"
                    + "2) Split it into 2 or 3 subtopics.\n\n"
                    + "3) Practice with examples or short questions.\n\n"
                    + "4) Take a 10-minute break.\n\n"
                    + "5) Summarize the most important ideas.";
        }
    }

    private void loadHistory() {
        aiHistoryContainer.removeAllViews();

        ArrayList<AiRoadmap> roadmapList = databaseHelper.getAllAiRoadmaps();

        if (roadmapList.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No saved AI roadmaps yet");
            emptyText.setTextSize(15);
            emptyText.setTextColor(0xFF8B8E99);
            emptyText.setPadding(0, dpToPx(12), 0, 0);
            aiHistoryContainer.addView(emptyText);
            return;
        }

        for (AiRoadmap roadmap : roadmapList) {
            addHistoryCard(roadmap);
        }
    }

    private void addHistoryCard(AiRoadmap aiRoadmap) {
        LinearLayout card = new LinearLayout(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(14);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        card.setBackgroundResource(R.drawable.ai_soft_purple_bg);

        TextView topicText = new TextView(getContext());
        topicText.setText(aiRoadmap.getTopic());
        topicText.setTextSize(16);
        topicText.setTextColor(0xFF7A35E8);
        topicText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView dateText = new TextView(getContext());
        dateText.setText(aiRoadmap.getCreatedAt());
        dateText.setTextSize(12);
        dateText.setTextColor(0xFF8B8E99);
        dateText.setPadding(0, dpToPx(6), 0, 0);

        TextView roadmapText = new TextView(getContext());
        roadmapText.setText(aiRoadmap.getRoadmap());
        roadmapText.setTextSize(14);
        roadmapText.setTextColor(0xFF111111);
        roadmapText.setPadding(0, dpToPx(10), 0, 0);

        TextView deleteText = new TextView(getContext());
        deleteText.setText("Delete");
        deleteText.setTextSize(13);
        deleteText.setTextColor(0xFFFFFFFF);
        deleteText.setTypeface(null, android.graphics.Typeface.BOLD);
        deleteText.setBackgroundResource(R.drawable.button_dark_rounded);
        deleteText.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.topMargin = dpToPx(12);
        deleteText.setLayoutParams(deleteParams);

        deleteText.setOnClickListener(v -> {
            boolean deleted = databaseHelper.deleteAiRoadmap(aiRoadmap.getId());
            if (deleted) {
                loadHistory();
                Toast.makeText(getContext(), "Roadmap deleted", Toast.LENGTH_SHORT).show();
            }
        });

        card.addView(topicText);
        card.addView(dateText);
        card.addView(roadmapText);
        card.addView(deleteText);

        aiHistoryContainer.addView(card);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}