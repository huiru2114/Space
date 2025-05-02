package com.example.space;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class FilterDialogFragment extends DialogFragment {

    // Interface for filter selection callback
    public interface FilterSelectionListener {
        void onFilterSelected(String filterOption);
    }

    private final FilterSelectionListener listener;

    public FilterDialogFragment(FilterSelectionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Inflate custom layout for dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filter, null);

        RadioGroup filterOptions = view.findViewById(R.id.filter_options);
        RadioButton recentButton = view.findViewById(R.id.filter_recent);
        RadioButton oldestButton = view.findViewById(R.id.filter_oldest);
        RadioButton clearButton = view.findViewById(R.id.filter_clear);

        builder.setView(view)
                .setTitle("Filter Trips")
                .setPositiveButton("Apply", (dialog, id) -> {
                    int selectedId = filterOptions.getCheckedRadioButtonId();
                    String filterOption = null;

                    if (selectedId == R.id.filter_recent) {
                        filterOption = "recent";
                    } else if (selectedId == R.id.filter_oldest) {
                        filterOption = "oldest";
                    } // Null for clear filter

                    if (listener != null) {
                        listener.onFilterSelected(filterOption);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    // User cancelled the dialog
                    dialog.dismiss();
                });

        return builder.create();
    }
}