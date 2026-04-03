package com.example.studysmart;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private TextView profileNameText;
    private TextView profileEmailText;
    private ImageView profileImageView;
    private Switch switchDarkMode;

    private DatabaseHelper databaseHelper;
    private SharedPreferences preferences;
    private String loggedInEmail;

    private ActivityResultLauncher<String> imagePickerLauncher;

    public ProfileFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        profileImageView.setImageURI(uri);
                        preferences.edit().putString("profileImageUri", uri.toString()).apply();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileNameText = view.findViewById(R.id.profileNameText);
        profileEmailText = view.findViewById(R.id.profileEmailText);
        profileImageView = view.findViewById(R.id.profileImageView);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        databaseHelper = new DatabaseHelper(getContext());
        preferences = requireActivity().getSharedPreferences("StudySmartPrefs", requireActivity().MODE_PRIVATE);
        loggedInEmail = preferences.getString("loggedInEmail", null);

        loadProfileData();
        loadProfileImage();
        loadDarkModeState();

        view.findViewById(R.id.btnChoosePhoto).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("darkModeEnabled", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            requireActivity().recreate();
        });

        view.findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("loggedInEmail");
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void loadProfileData() {
        if (loggedInEmail != null) {
            String userName = databaseHelper.getUserNameByEmail(loggedInEmail);

            if (userName != null && !userName.isEmpty()) {
                profileNameText.setText(userName);
            } else {
                profileNameText.setText("User");
            }

            profileEmailText.setText(loggedInEmail);
        } else {
            profileNameText.setText("User");
            profileEmailText.setText("No email found");
        }
    }

    private void loadProfileImage() {
        String imageUriString = preferences.getString("profileImageUri", null);
        if (imageUriString != null) {
            profileImageView.setImageURI(Uri.parse(imageUriString));
        }
    }

    private void loadDarkModeState() {
        boolean darkModeEnabled = preferences.getBoolean("darkModeEnabled", false);
        switchDarkMode.setChecked(darkModeEnabled);
    }

    private void showEditProfileDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);

        EditText editProfileName = dialogView.findViewById(R.id.editProfileName);
        EditText editProfileEmail = dialogView.findViewById(R.id.editProfileEmail);
        EditText editProfilePassword = dialogView.findViewById(R.id.editProfilePassword);

        editProfileName.setText(profileNameText.getText().toString());
        editProfileEmail.setText(profileEmailText.getText().toString());

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editProfileName.getText().toString().trim();
                    String newEmail = editProfileEmail.getText().toString().trim();
                    String newPassword = editProfilePassword.getText().toString().trim();

                    if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newEmail) || TextUtils.isEmpty(newPassword)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean updated = databaseHelper.updateUserProfile(loggedInEmail, newName, newEmail, newPassword);

                    if (updated) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("loggedInEmail", newEmail);
                        editor.apply();

                        loggedInEmail = newEmail;
                        loadProfileData();

                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}