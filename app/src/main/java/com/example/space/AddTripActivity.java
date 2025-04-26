package com.example.space;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTripActivity extends AppCompatActivity {

    private EditText startDateInput;
    private EditText endDateInput;
    private EditText tripNameInput;
    private EditText countryInput;
    private EditText journalInput;
    private Button addPhotosButton;
    private Button addTripButton;
    private ImageButton backButton;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        initViews();
        setupListeners();
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
            // Implement photo selection logic
            Toast.makeText(this, "Photo selection will be implemented", Toast.LENGTH_SHORT).show();
        });

        addTripButton.setOnClickListener(v -> {
            // Implement save trip logic
            if (validateInputs()) {
                saveTrip();
            }
        });
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
        // Here you would save the trip data to your database or shared preferences
        Toast.makeText(this, "Trip saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}