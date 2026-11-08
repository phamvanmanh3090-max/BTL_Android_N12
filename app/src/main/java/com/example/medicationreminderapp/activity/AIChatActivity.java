package com.example.medicationreminderapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.BuildConfig;
import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.adapter.ChatAdapter;
import com.example.medicationreminderapp.model.ChatMessage;
import com.example.medicationreminderapp.viewmodel.ChatViewModel;
import com.example.medicationreminderapp.viewmodel.ChatViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {

    private TextView navHome, navAlarm, navStore, navConsultation;

    private RecyclerView recyclerViewChat;
    private TextInputEditText editTextMessage;
    private Button buttonSend;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;

    private View suggestionContainer;
    private TextView suggestion1, suggestion2, suggestion3;

    private ChatViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);

        initView();

        try {
            initViewModel();
        } catch (Exception e) {
            Toast.makeText(this, "Chưa cấu hình API Key!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        initChat();
        setupSuggestions();
        setupBottomNavigation();

        if (viewModel != null) {
            observeViewModel();
        }
    }

    private void initView() {
        navHome = findViewById(R.id.navHome);
        navAlarm = findViewById(R.id.navAlarm);
        navStore = findViewById(R.id.navStore);
        navConsultation = findViewById(R.id.navConsultation);

        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);

        suggestionContainer = findViewById(R.id.suggestion_container);
        suggestion1 = findViewById(R.id.suggestion1);
        suggestion2 = findViewById(R.id.suggestion2);
        suggestion3 = findViewById(R.id.suggestion3);

        if (navConsultation != null) navConsultation.setSelected(true);
        if (navHome != null) navHome.setSelected(false);
        if (navAlarm != null) navAlarm.setSelected(false);
        if (navStore != null) navStore.setSelected(false);
    }

    private void initViewModel() {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        ChatViewModelFactory factory = new ChatViewModelFactory(apiKey);
        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);
    }

    private void initChat() {
        messages = new ArrayList<>();
        messages.add(new ChatMessage("Xin chào! Tôi là trợ lý sức khỏe AI. Tôi có thể giúp gì cho bạn về thông tin thuốc?", false));

        chatAdapter = new ChatAdapter(messages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void observeViewModel() {
        viewModel.getChatResponse().observe(this, response -> {
            if (response == null || response.contains("high demand")) {
                messages.add(new ChatMessage("Hệ thống đang bận, vui lòng thử lại sau.", false));
            } else {
                messages.add(new ChatMessage(response, false));
            }
            chatAdapter.notifyItemInserted(messages.size() - 1);
            recyclerViewChat.smoothScrollToPosition(messages.size() - 1);
        });
    }

    private void setupSuggestions() {
        View.OnClickListener listener = v -> {
            String question = ((TextView) v).getText().toString();
            sendMessage(question);
        };

        if (suggestion1 != null) suggestion1.setOnClickListener(listener);
        if (suggestion2 != null) suggestion2.setOnClickListener(listener);
        if (suggestion3 != null) suggestion3.setOnClickListener(listener);
    }

    private void sendMessage(String text) {
        messages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerViewChat.scrollToPosition(messages.size() - 1);

        editTextMessage.setText("");
        if (suggestionContainer != null) {
            suggestionContainer.setVisibility(View.GONE);
        }

        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            messages.add(new ChatMessage("⚠️ Chưa cấu hình GEMINI_API_KEY trong local.properties.", false));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            recyclerViewChat.scrollToPosition(messages.size() - 1);
        } else if (viewModel != null) {
            viewModel.sendMessage(text);
        }
    }

    private void setupBottomNavigation() {

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (navAlarm != null) {
            navAlarm.setOnClickListener(v -> {
                Intent intent = new Intent(this, ScheduleActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (navStore != null) {
            navStore.setOnClickListener(v -> {
                Intent intent = new Intent(this, StoreActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (navConsultation != null) {
            navConsultation.setOnClickListener(v -> {});
        }

        if (buttonSend != null) {
            buttonSend.setOnClickListener(v -> {
                String text = editTextMessage.getText().toString().trim();
                if (!text.isEmpty()) sendMessage(text);
            });
        }
    }
}