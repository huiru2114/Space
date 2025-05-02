package com.example.space;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HelpCenterFragment extends Fragment {

    private ImageView backButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help_center, container, false);

        // Initialize back button
        backButton = view.findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Setup expandable FAQ items
        setupFaqItems(view);

        return view;
    }

    private void setupFaqItems(View view) {
        // FAQ Item 1
        LinearLayout faqItem1 = view.findViewById(R.id.faq_item_1);
        TextView faqAnswer1 = view.findViewById(R.id.faq_answer_1);
        faqItem1.setOnClickListener(v -> {
            if (faqAnswer1.getVisibility() == View.VISIBLE) {
                faqAnswer1.setVisibility(View.GONE);
            } else {
                faqAnswer1.setVisibility(View.VISIBLE);
            }
        });

        // FAQ Item 2
        LinearLayout faqItem2 = view.findViewById(R.id.faq_item_2);
        TextView faqAnswer2 = view.findViewById(R.id.faq_answer_2);
        faqItem2.setOnClickListener(v -> {
            if (faqAnswer2.getVisibility() == View.VISIBLE) {
                faqAnswer2.setVisibility(View.GONE);
            } else {
                faqAnswer2.setVisibility(View.VISIBLE);
            }
        });

        // FAQ Item 3
        LinearLayout faqItem3 = view.findViewById(R.id.faq_item_3);
        TextView faqAnswer3 = view.findViewById(R.id.faq_answer_3);
        faqItem3.setOnClickListener(v -> {
            if (faqAnswer3.getVisibility() == View.VISIBLE) {
                faqAnswer3.setVisibility(View.GONE);
            } else {
                faqAnswer3.setVisibility(View.VISIBLE);
            }
        });

        // FAQ Item 4
        LinearLayout faqItem4 = view.findViewById(R.id.faq_item_4);
        TextView faqAnswer4 = view.findViewById(R.id.faq_answer_4);
        faqItem4.setOnClickListener(v -> {
            if (faqAnswer4.getVisibility() == View.VISIBLE) {
                faqAnswer4.setVisibility(View.GONE);
            } else {
                faqAnswer4.setVisibility(View.VISIBLE);
            }
        });

        // FAQ Item 5
        LinearLayout faqItem5 = view.findViewById(R.id.faq_item_5);
        TextView faqAnswer5 = view.findViewById(R.id.faq_answer_5);
        faqItem5.setOnClickListener(v -> {
            if (faqAnswer5.getVisibility() == View.VISIBLE) {
                faqAnswer5.setVisibility(View.GONE);
            } else {
                faqAnswer5.setVisibility(View.VISIBLE);
            }
        });
    }
}