package com.example.space;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AccountSettingsFragment extends Fragment {

    private static final String TAG = "AccountSettingsFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextInputLayout usernameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private Button updateProfileButton;
    private Button changePasswordButton;
    private ImageView profileImageView;
    private String base64Image = null;

    private SupabaseAuth auth;
    private SharedPreferences authPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_settings, container, false);

        // Initialize SupabaseAuth
        auth = new SupabaseAuth(requireContext());
        authPrefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);

        // Initialize UI components
        usernameLayout = view.findViewById(R.id.username_layout);
        emailLayout = view.findViewById(R.id.email_layout);
        phoneLayout = view.findViewById(R.id.phone_layout);
        usernameEditText = view.findViewById(R.id.username_edit_text);
        emailEditText = view.findViewById(R.id.email_edit_text);
        phoneEditText = view.findViewById(R.id.phone_edit_text);
        updateProfileButton = view.findViewById(R.id.update_profile_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        profileImageView = view.findViewById(R.id.profile_image);

        // Set up profile image click listener
        profileImageView.setOnClickListener(v -> selectImage());

        // Set up update profile button click listener
        updateProfileButton.setOnClickListener(v -> updateProfile());

        // Set up change password button click listener
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        String userId = auth.getUserId();
        if (userId == null) {
            // User is not logged in
            Toast.makeText(requireContext(), "You must be logged in to access account settings", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        // Get user email
        String userEmail = auth.getCurrentUserEmail();
        if (userEmail != null) {
            emailEditText.setText(userEmail);
            // Make email field read-only (changing email requires re-verification)
            emailEditText.setEnabled(false);
        }

        // Fetch profile data
        auth.fetchProfile(userId, new SupabaseAuth.ProfileCallback() {
            @Override
            public void onSuccess(String username, String phone, String profilePic) {
                if (getActivity() == null) return; // Fragment might be detached

                getActivity().runOnUiThread(() -> {
                    usernameEditText.setText(username);
                    phoneEditText.setText(phone);

                    // Load profile picture if available
                    if (profilePic != null && !profilePic.isEmpty()) {
                        // You would typically use an image loading library like Glide or Picasso here
                        // For simplicity, we'll use a placeholder
                        profileImageView.setImageResource(R.drawable.ic_account);

                        // In a real implementation, you would do something like:
                        // Glide.with(requireContext()).load(profilePic).into(profileImageView);
                    }
                });
            }

            @Override
            public void onProfileNotFound() {
                if (getActivity() == null) return; // Fragment might be detached

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Profile not found", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return; // Fragment might be detached

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Resize the bitmap to reduce size
                Bitmap resizedBitmap = resizeBitmap(bitmap, 500);

                // Display the selected image
                profileImageView.setImageBitmap(resizedBitmap);

                // Convert to base64 for upload
                base64Image = bitmapToBase64(resizedBitmap);

            } catch (FileNotFoundException e) {
                Toast.makeText(requireContext(), "Error selecting image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error selecting image", e);
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = (float) width / (float) height;

        if (ratio > 1) { // Width is greater than height
            width = maxSize;
            height = (int) (width / ratio);
        } else { // Height is greater than width
            height = maxSize;
            width = (int) (height * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void updateProfile() {
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        // Validate input
        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            return;
        } else {
            usernameLayout.setError(null);
        }

        // Show loading progress
        updateProfileButton.setEnabled(false);
        updateProfileButton.setText("Updating...");

        // Update profile
        String userId = auth.getUserId();
        if (userId != null) {
            auth.updateProfile(userId, username, phone, base64Image, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() == null) return; // Fragment might be detached

                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        updateProfileButton.setEnabled(true);
                        updateProfileButton.setText("Update Profile");
                        base64Image = null; // Clear after upload
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return; // Fragment might be detached

                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error updating profile: " + error, Toast.LENGTH_SHORT).show();
                        updateProfileButton.setEnabled(true);
                        updateProfileButton.setText("Update Profile");
                    });
                }
            });
        }
    }

    private void showChangePasswordDialog() {
        // Create dialog view
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", null) // Set null to override default behavior
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        dialog.show();

        // Override the positive button to prevent dialog dismissal when input is invalid
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            EditText currentPasswordEdit = dialogView.findViewById(R.id.current_password_edit);
            EditText newPasswordEdit = dialogView.findViewById(R.id.new_password_edit);
            EditText confirmPasswordEdit = dialogView.findViewById(R.id.confirm_password_edit);

            String currentPassword = currentPasswordEdit.getText().toString();
            String newPassword = newPasswordEdit.getText().toString();
            String confirmPassword = confirmPasswordEdit.getText().toString();

            // Validate input
            if (currentPassword.isEmpty()) {
                currentPasswordEdit.setError("Current password is required");
                return;
            }

            if (newPassword.isEmpty()) {
                newPasswordEdit.setError("New password is required");
                return;
            }

            if (newPassword.length() < 6) {
                newPasswordEdit.setError("Password must be at least 6 characters");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                confirmPasswordEdit.setError("Passwords do not match");
                return;
            }

            // Show progress
            TextView messageView = dialogView.findViewById(R.id.password_change_message);
            if (messageView != null) {
                messageView.setText("Changing password...");
                messageView.setVisibility(View.VISIBLE);
            }

            // Disable the buttons to prevent multiple clicks
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

            // Call password change method
            auth.changePassword(currentPassword, newPassword, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() == null) return; // Fragment might be detached

                    getActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return; // Fragment might be detached

                    getActivity().runOnUiThread(() -> {
                        // Re-enable the buttons
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);

                        // Show error message
                        if (messageView != null) {
                            messageView.setText(error);
                            messageView.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
    }
}