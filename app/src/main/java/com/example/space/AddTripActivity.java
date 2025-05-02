package com.example.space;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

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
    private static final int PICK_MULTIPLE_IMAGES_REQUEST = 2;
    private static final String TAG = "AddTripActivity";

    private EditText startDateInput;
    private EditText endDateInput;
    private EditText tripNameInput;
    private EditText countryInput;
    private EditText journalInput;
    private MaterialButton addPhotosButton;
    private MaterialButton addTripButton;
    private ImageButton backButton;
    private CardView photoArea;
    private LinearLayout uploadedImagesContainer;
    private ProgressBar progressBar;
    private TextView uploadStatus;
    private Toolbar toolbar;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private List<String> uploadedImageUrls = new ArrayList<>();
    private List<View> imageViewContainers = new ArrayList<>();  // Track image containers for deletion
    private SupabaseTrip supabaseTrip;
    private int totalImagesToUpload = 0;
    private int imagesUploaded = 0;

    // For editing existing trips
    private String tripId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Initialize SupabaseTrip
        supabaseTrip = new SupabaseTrip(this);

        initViews();
        setupListeners();

        // Check if we're editing an existing trip
        handleIntent();
    }

    private void initViews() {
        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        backButton = findViewById(R.id.btn_back);

        // Get the photo area
        photoArea = findViewById(R.id.photo_area);

        // Initialize date fields
        startDateInput = findViewById(R.id.start_date);
        endDateInput = findViewById(R.id.end_date);

        // Initialize trip details fields
        tripNameInput = findViewById(R.id.trip_name_input);
        countryInput = findViewById(R.id.country_input);
        journalInput = findViewById(R.id.journal_input);

        // Initialize buttons
        addPhotosButton = findViewById(R.id.btn_add_photos);
        addTripButton = findViewById(R.id.btn_add_trip);

        // Get the uploaded images container from layout
        uploadedImagesContainer = findViewById(R.id.uploaded_images_container);

        // Get progress bar from layout
        progressBar = findViewById(R.id.progress_bar);

        // Get upload status text view
        uploadStatus = findViewById(R.id.upload_status);

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
            // Open gallery to select multiple photos
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_MULTIPLE_IMAGES_REQUEST);
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
        // OR if we're editing an existing trip
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("trip_id")) {
                // We're editing an existing trip
                tripId = intent.getStringExtra("trip_id");
                String tripName = intent.getStringExtra("trip_name");
                String country = intent.getStringExtra("country");
                String journal = intent.getStringExtra("journal");
                String startDateStr = intent.getStringExtra("start_date");
                String endDateStr = intent.getStringExtra("end_date");
                ArrayList<String> imageUrls = intent.getStringArrayListExtra("image_urls");

                // Set fields with existing data
                if (tripName != null) tripNameInput.setText(tripName);
                if (country != null) countryInput.setText(country);
                if (journal != null) journalInput.setText(journal);

                // Set dates if available
                try {
                    if (startDateStr != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        Date startDate = sdf.parse(startDateStr);
                        startDateInput.setText(dateFormat.format(startDate));
                    }

                    if (endDateStr != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        Date endDate = sdf.parse(endDateStr);
                        endDateInput.setText(dateFormat.format(endDate));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // Set image URLs
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    uploadedImageUrls.addAll(imageUrls);

                    // Display placeholders for existing images
                    for (int i = 0; i < imageUrls.size(); i++) {
                        String imageUrl = imageUrls.get(i);

                        // Create placeholder thumbnail with delete button
                        ImageView thumbnail = new ImageView(this);
                        thumbnail.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        thumbnail.setImageResource(R.drawable.ic_calendar);
                        thumbnail.setPadding(8, 8, 8, 8);

                        Bitmap bitmap = ((BitmapDrawable) thumbnail.getDrawable()).getBitmap();

                        // Create container with delete button for this image
                        View container = createImageContainerWithDeleteButton(bitmap, imageUrl, i);
                        uploadedImagesContainer.addView(container);
                        imageViewContainers.add(container);
                    }

                    // Update button text
                    addPhotosButton.setText("Add More Photos");
                }

                // Update UI to show we're in edit mode
                isEditMode = true;
                addTripButton.setText("Update Trip");

                // Show message
                Toast.makeText(this, "Editing existing trip", Toast.LENGTH_SHORT).show();
            }
            else if (intent.hasExtra("selected_country")) {
                // Handle country selection from globe (existing code)
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

        if (uploadStatus != null) {
            uploadStatus.setVisibility(View.VISIBLE);
            uploadStatus.setText(isEditMode ? "Updating trip..." : "Saving trip...");
        }

        try {
            // Create a Trip object
            Trip trip = new Trip();

            // Set trip_id only if in edit mode
            if (isEditMode && tripId != null) {
                trip.setTripId(tripId);
            }

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
                        if (uploadStatus != null) {
                            uploadStatus.setVisibility(View.GONE);
                        }
                        addTripButton.setEnabled(true);
                        Toast.makeText(AddTripActivity.this, message, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (uploadStatus != null) {
                            uploadStatus.setVisibility(View.GONE);
                        }
                        addTripButton.setEnabled(true);
                        Toast.makeText(AddTripActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (ParseException e) {
            progressBar.setVisibility(View.GONE);
            if (uploadStatus != null) {
                uploadStatus.setVisibility(View.GONE);
            }
            addTripButton.setEnabled(true);
            Toast.makeText(this, "Error parsing dates", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            progressBar.setVisibility(View.VISIBLE);

            // For single image selection (keeping backward compatibility)
            if (requestCode == PICK_IMAGE_REQUEST && data.getData() != null) {
                Uri imageUri = data.getData();
                processImage(imageUri);
            }
            // For multiple image selection
            else if (requestCode == PICK_MULTIPLE_IMAGES_REQUEST) {
                // Case 1: Multiple images selected
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    totalImagesToUpload = count;
                    imagesUploaded = 0;

                    if (uploadStatus != null) {
                        uploadStatus.setVisibility(View.VISIBLE);
                        uploadStatus.setText("Preparing to upload " + count + " images");
                    }

                    progressBar.setMax(count);
                    progressBar.setProgress(0);

                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        processImageInBatch(imageUri, count, i+1);
                    }
                }
                // Case 2: Only one image selected through multiple selection intent
                else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    processImage(imageUri);
                }
            }
        }
    }

    private void processImage(Uri imageUri) {
        try {
            // Convert Uri to byte array
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            // Show temporary placeholder while uploading
            final View tempContainer = addImageThumbnail(bitmap, null);

            // Upload the image to Supabase storage
            supabaseTrip.uploadTripImage(imageData, new SupabaseTrip.TripCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    runOnUiThread(() -> {
                        // Add URL to our list
                        uploadedImageUrls.add(imageUrl);

                        // Remove temporary container
                        uploadedImagesContainer.removeView(tempContainer);

                        // Add the final version with delete button
                        addImageThumbnailWithDeleteButton(bitmap, imageUrl);

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddTripActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // Remove temporary placeholder on error
                        uploadedImagesContainer.removeView(tempContainer);

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddTripActivity.this, "Error uploading image: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImageInBatch(Uri imageUri, int totalImages, int currentPosition) {
        try {
            // Convert Uri to byte array
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Add a temporary thumbnail
            final View tempContainer = addImageThumbnail(bitmap, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            if (uploadStatus != null) {
                uploadStatus.setText("Uploading " + currentPosition + " of " + totalImages);
            }

            // Upload the image to Supabase storage
            supabaseTrip.uploadTripImage(imageData, new SupabaseTrip.TripCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    runOnUiThread(() -> {
                        // Add URL to our list
                        uploadedImageUrls.add(imageUrl);

                        // Remove temporary placeholder
                        uploadedImagesContainer.removeView(tempContainer);

                        // Add final thumbnail with delete button
                        addImageThumbnailWithDeleteButton(bitmap, imageUrl);

                        // Update progress
                        imagesUploaded++;
                        progressBar.setProgress(imagesUploaded);

                        if (uploadStatus != null) {
                            uploadStatus.setText("Uploaded " + imagesUploaded + " of " + totalImages);
                        }

                        // If all images uploaded
                        if (imagesUploaded >= totalImages) {
                            progressBar.setVisibility(View.GONE);

                            if (uploadStatus != null) {
                                uploadStatus.setText("All images uploaded successfully!");
                                // Hide status after a delay
                                new Handler().postDelayed(() -> uploadStatus.setVisibility(View.GONE), 2000);
                            }

                            Toast.makeText(AddTripActivity.this, totalImages + " images uploaded successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // Remove temporary placeholder on error
                        uploadedImagesContainer.removeView(tempContainer);

                        // Update progress even on error
                        imagesUploaded++;
                        progressBar.setProgress(imagesUploaded);

                        if (uploadStatus != null) {
                            uploadStatus.setText("Error uploading image " + currentPosition);
                        }

                        if (imagesUploaded >= totalImages) {
                            progressBar.setVisibility(View.GONE);

                            if (uploadStatus != null) {
                                // Hide status after a delay
                                new Handler().postDelayed(() -> uploadStatus.setVisibility(View.GONE), 2000);
                            }
                        }

                        Toast.makeText(AddTripActivity.this, "Error uploading image: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (IOException e) {
            e.printStackTrace();

            // Update progress even on error
            imagesUploaded++;
            progressBar.setProgress(imagesUploaded);

            if (imagesUploaded >= totalImages) {
                progressBar.setVisibility(View.GONE);

                if (uploadStatus != null) {
                    uploadStatus.setVisibility(View.GONE);
                }
            }

            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a temporary thumbnail without delete button
     * Used for showing placeholders while uploading
     */
    private View addImageThumbnail(Bitmap bitmap, String imageUrl) {
        // Inflate the image thumbnail layout but hide the delete button
        View container = getLayoutInflater().inflate(R.layout.image_thumbnail_with_delete, null);

        // Get and configure the ImageView
        ImageView thumbnail = container.findViewById(R.id.image_thumbnail);
        thumbnail.setImageBitmap(bitmap);

        // Hide the delete button for temporary thumbnails
        ImageButton deleteButton = container.findViewById(R.id.btn_delete_image);
        deleteButton.setVisibility(View.GONE);

        // Add it to the container
        uploadedImagesContainer.addView(container);

        // Update UI to show that we have images
        addPhotosButton.setText("Add More Photos");

        return container;
    }
    /**
     * Adds a thumbnail with delete button after successful upload
     */
    private void addImageThumbnailWithDeleteButton(Bitmap bitmap, String imageUrl) {
        // Create and add container with delete button
        View container = createImageContainerWithDeleteButton(bitmap, imageUrl, uploadedImageUrls.indexOf(imageUrl));
        uploadedImagesContainer.addView(container);
        imageViewContainers.add(container);

        // Update UI to show that we have images
        addPhotosButton.setText("Add More Photos");
    }


    /**
     * Creates a container with image and delete button using the new layout
     */
    private View createImageContainerWithDeleteButton(Bitmap bitmap, final String imageUrl, final int position) {
        // Inflate the image thumbnail layout
        View container = getLayoutInflater().inflate(R.layout.image_thumbnail_with_delete, null);

        // Get the ImageView from the inflated layout
        ImageView thumbnail = container.findViewById(R.id.image_thumbnail);

        // Set the bitmap to the ImageView
        if (bitmap != null) {
            thumbnail.setImageBitmap(bitmap);
        } else {
            // Use a placeholder if no bitmap is provided (for existing images when editing)
            thumbnail.setImageResource(R.drawable.ic_calendar);
        }

        // Get the delete button from the inflated layout
        ImageButton deleteButton = container.findViewById(R.id.btn_delete_image);

        // Set click listener for the delete button
        deleteButton.setOnClickListener(v -> {
            // Remove image URL from list
            if (imageUrl != null && uploadedImageUrls.contains(imageUrl)) {
                uploadedImageUrls.remove(imageUrl);
            }

            // Remove the image container from the UI
            uploadedImagesContainer.removeView(container);
            imageViewContainers.remove(container);


            // Update button text if no images left
            if (uploadedImageUrls.isEmpty()) {
                addPhotosButton.setText("Add Photos");
            }
        });


        return container;
    }
}