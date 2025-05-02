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
        setupFaqItem(view, R.id.faq_item_1, R.id.faq_answer_1);

        // FAQ Item 2
        setupFaqItem(view, R.id.faq_item_2, R.id.faq_answer_2);

        // FAQ Item 3
        setupFaqItem(view, R.id.faq_item_3, R.id.faq_answer_3);

        // FAQ Item 4
        setupFaqItem(view, R.id.faq_item_4, R.id.faq_answer_4);

        // FAQ Item 5
        setupFaqItem(view, R.id.faq_item_5, R.id.faq_answer_5);

        // FAQ Item 6
        setupFaqItem(view, R.id.faq_item_6, R.id.faq_answer_6);

        // FAQ Item 7
        setupFaqItem(view, R.id.faq_item_7, R.id.faq_answer_7);

        // FAQ Item 8
        setupFaqItem(view, R.id.faq_item_8, R.id.faq_answer_8);

        // FAQ Item 9
        setupFaqItem(view, R.id.faq_item_9, R.id.faq_answer_9);

        // FAQ Item 10
        setupFaqItem(view, R.id.faq_item_10, R.id.faq_answer_10);

        // FAQ Item 11
        setupFaqItem(view, R.id.faq_item_11, R.id.faq_answer_11);

        // FAQ Item 12
        setupFaqItem(view, R.id.faq_item_12, R.id.faq_answer_12);

        // FAQ Item 13
        setupFaqItem(view, R.id.faq_item_13, R.id.faq_answer_13);
    }

    private void setupFaqItem(View view, int itemId, int answerId) {
        LinearLayout faqItem = view.findViewById(itemId);
        TextView faqAnswer = view.findViewById(answerId);

        faqItem.setOnClickListener(v -> {
            if (faqAnswer.getVisibility() == View.VISIBLE) {
                faqAnswer.setVisibility(View.GONE);
            } else {
                faqAnswer.setVisibility(View.VISIBLE);
            }
        });
    }
}