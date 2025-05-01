package com.example.space;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTripActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "AddTripActivity";

    private EditText startDateInput;
    private EditText endDateInput;
    private EditText tripNameInput;
    private EditText countryInput;
    private EditText journalInput;
    private Button addPhotosButton;
    private Button addTripButton;
    private ImageButton backButton;
    private CardView photoArea;
    private LinearLayout uploadedImagesContainer;
    private ProgressBar progressBar;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private List<String> uploadedImageUrls = new ArrayList<>();
    private SupabaseTrip supabaseTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        // Initialize SupabaseTrip
        supabaseTrip = new SupabaseTrip(this);

        initViews();
        setupListeners();
        handleIntent();
    }

    private void initViews() {
        startDateInput = findViewById(R.id.start_date);
        endDateInput = findViewById(R.id.end_date);
        tripNameInput = findViewById(R.id.trip_name_input);
        countryInput = findViewById(R.id.country_input);
        journalInput = findViewById(R.id.journal_input);
        addPhotosButton = findViewById(R.id.btn_add_photos);
        addTripButton = findViewById(R.id.btn_add_trip);
        backButton = findViewById(R.id.btn_back);

        // Get the uploaded images container from layout
        uploadedImagesContainer = findViewById(R.id.uploaded_images_container);

        // Get progress bar from layout
        progressBar = findViewById(R.id.progress_bar);

        // Set current date as default
        String today = dateFormat.format(calendar.getTime());
        startDateInput.setText(today);
        endDateInput.setText(today);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
        endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

        addPhotosButton.setOnClickListener(v -> {
            // Open gallery to select photos
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        addTripButton.setOnClickListener(v -> {
            // Implement save trip logic
            if (validateInputs()) {
                saveTrip();
            }
        });
    }

    private void handleIntent() {
        // Check if we have a country name from the globe interaction
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("selected_country")) {
            String selectedCountry = intent.getStringExtra("selected_country");
            if (selectedCountry != null && !selectedCountry.isEmpty()) {
                countryInput.setText(selectedCountry);

                // Optionally, we can also suggest a trip name based on the country
                String suggestedTripName = "Trip to " + selectedCountry;
                tripNameInput.setText(suggestedTripName);

                // Show feedback to the user
                Toast.makeText(this, "Planning a trip to " + selectedCountry, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePicker(final EditText dateField) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    dateField.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private boolean validateInputs() {
        if (tripNameInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a trip name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (countryInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a country", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveTrip() {
        progressBar.setVisibility(View.VISIBLE);
        addTripButton.setEnabled(false);

        try {
            // Create a new Trip object
            Trip trip = new Trip();
            trip.setTripName(tripNameInput.getText().toString().trim());
            trip.setCountry(countryInput.getText().toString().trim());
            trip.setJournal(journalInput.getText().toString().trim());

            // Parse the start and end dates
            SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date startDate = format.parse(startDateInput.getText().toString());
            Date endDate = format.parse(endDateInput.getText().toString());

            trip.setStartDate(startDate);
            trip.setEndDate(endDate);

            // Add all uploaded image URLs
            for (String url : uploadedImageUrls) {
                trip.addImageUrl(url);
            }

            // Save the trip to Supabase
            supabaseTrip.saveTrip(trip, new SupabaseTrip.TripCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addTripButton.setEnabled(true);
                        Toast.makeText(AddTripActivity.this, "Trip saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addTripButton.setEnabled(true);
                        Toast.makeText(AddTripActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (ParseException e) {
            progressBar.setVisibility(View.GONE);
            addTripButton.setEnabled(true);
            Toast.makeText(this, "Error parsing dates", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            try {
                // Convert Uri to byte array
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageData = baos.toByteArray();

                // Show upload progress
                progressBar.setVisibility(View.VISIBLE);

                // Upload the image to Supabase storage
                supabaseTrip.uploadTripImage(imageData, new SupabaseTrip.TripCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        runOnUiThread(() -> {
                            // Add URL to our list
                            uploadedImageUrls.add(imageUrl);

                            // Display the uploaded image as a thumbnail
                            addImageThumbnail(bitmap);

                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AddTripActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AddTripActivity.this, "Error uploading image: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addImageThumbnail(Bitmap bitmap) {
        // Create an ImageView for the thumbnail
        ImageView thumbnail = new ImageView(this);
        thumbnail.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setImageBitmap(bitmap);
        thumbnail.setPadding(8, 8, 8, 8);

        // Add it to the container
        uploadedImagesContainer.addView(thumbnail);

        // Update UI to show that we have images
        addPhotosButton.setText("Add More Photos");
    }
}