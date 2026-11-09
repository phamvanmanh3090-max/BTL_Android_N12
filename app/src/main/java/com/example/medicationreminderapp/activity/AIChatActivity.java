package com.example.medicationreminderapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.BuildConfig;
import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.adapter.ChatAdapter;
import com.example.medicationreminderapp.model.ChatConversation;
import com.example.medicationreminderapp.model.ChatMessage;
import com.example.medicationreminderapp.model.SavedChatMessage;
import com.example.medicationreminderapp.viewmodel.ChatViewModel;
import com.example.medicationreminderapp.viewmodel.ChatViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AIChatActivity extends AppCompatActivity {
    private static final String GREETING = "Xin chào! Tôi là trợ lý sức khỏe AI. Tôi có thể giúp gì cho bạn về thông tin thuốc?";
    private RecyclerView recyclerViewChat;
    private TextInputEditText editTextMessage;
    private Button buttonSend;
    private View suggestionContainer, historyButton, newButton, deleteButton;
    private TextView conversationTitle, chatStatus;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private List<ChatConversation> conversations = Collections.emptyList();
    private Set<Long> pending = Collections.emptySet();
    private Long activeId;
    private boolean loading = true;
    private ChatViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        suggestionContainer = findViewById(R.id.suggestion_container);
        historyButton = findViewById(R.id.button_chat_history);
        newButton = findViewById(R.id.button_new_chat);
        deleteButton = findViewById(R.id.button_delete_chat);
        conversationTitle = findViewById(R.id.chat_conversation_title);
        chatStatus = findViewById(R.id.chat_status);
        chatAdapter = new ChatAdapter(messages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);

        viewModel = new ViewModelProvider(this,
                new ChatViewModelFactory(this, BuildConfig.GEMINI_API_KEY)).get(ChatViewModel.class);
        historyButton.setOnClickListener(v -> showHistory());
        newButton.setOnClickListener(v -> viewModel.newConversation());
        deleteButton.setOnClickListener(v -> confirmDelete(false));
        buttonSend.setOnClickListener(v -> {
            if (editTextMessage.getText() != null) sendMessage(editTextMessage.getText().toString());
        });
        for (int id : new int[]{R.id.suggestion1, R.id.suggestion2, R.id.suggestion3}) {
            TextView suggestion = findViewById(id);
            suggestion.setOnClickListener(v -> sendMessage(suggestion.getText().toString()));
        }
        setupBottomNavigation();
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getActiveId().observe(this, id -> {
            activeId = id;
            messages.clear();
            messages.add(new ChatMessage(GREETING, false));
            chatAdapter.notifyDataSetChanged();
            editTextMessage.setText("");
            suggestionContainer.setVisibility(View.VISIBLE);
            updateControls();
        });
        viewModel.getMessages().observe(this, saved -> {
            if (activeId == null || saved == null) return;
            if (!saved.isEmpty() && saved.get(0).conversationId != activeId) return;
            messages.clear();
            messages.add(new ChatMessage(GREETING, false));
            for (SavedChatMessage message : saved) {
                messages.add(new ChatMessage(message.text, message.user));
            }
            chatAdapter.notifyDataSetChanged();
            recyclerViewChat.scrollToPosition(messages.size() - 1);
            suggestionContainer.setVisibility(saved.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getConversations().observe(this, saved -> {
            conversations = saved == null ? Collections.emptyList() : saved;
            updateControls();
        });
        viewModel.getLoading().observe(this, value -> {
            loading = Boolean.TRUE.equals(value);
            updateControls();
        });
        viewModel.getPending().observe(this, ids -> {
            pending = ids == null ? Collections.emptySet() : ids;
            updateControls();
        });
        viewModel.getError().observe(this, message -> {
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void updateControls() {
        boolean ready = activeId != null && !loading;
        boolean sending = activeId != null && pending.contains(activeId);
        buttonSend.setEnabled(ready && !sending);
        historyButton.setEnabled(ready);
        newButton.setEnabled(ready);
        deleteButton.setEnabled(ready);
        chatStatus.setText(loading ? "Đang tải lịch sử…" : "Trợ lý đang trả lời…");
        chatStatus.setVisibility(loading || sending ? View.VISIBLE : View.GONE);
        conversationTitle.setText("Cuộc trò chuyện mới");
        for (ChatConversation conversation : conversations) {
            if (activeId != null && conversation.id == activeId) {
                conversationTitle.setText(conversation.title);
                break;
            }
        }
    }

    private void sendMessage(String question) {
        String text = question.trim();
        if (text.isEmpty() || activeId == null || loading || pending.contains(activeId)) return;
        if (viewModel.sendMessage(text)) editTextMessage.setText("");
    }

    private void showHistory() {
        List<ChatConversation> snapshot = new ArrayList<>(conversations);
        String[] items = new String[snapshot.size()];
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        for (int i = 0; i < snapshot.size(); i++) {
            ChatConversation conversation = snapshot.get(i);
            String current = activeId != null && conversation.id == activeId ? "Đang mở · " : "";
            items[i] = conversation.title + "\n" + current + dateFormat.format(new Date(conversation.updatedAt));
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Lịch sử tư vấn")
                .setItems(items, (dialog, which) -> viewModel.selectConversation(snapshot.get(which).id))
                .setPositiveButton("Trò chuyện mới", (dialog, which) -> viewModel.newConversation())
                .setNeutralButton("Xóa tất cả", (dialog, which) -> confirmDelete(true))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void confirmDelete(boolean all) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(all ? "Xóa toàn bộ lịch sử?" : "Xóa cuộc trò chuyện này?")
                .setMessage(all ? "Tất cả cuộc trò chuyện đã lưu trên máy sẽ bị xóa. Không thể khôi phục."
                        : "Các tin nhắn trong cuộc trò chuyện này sẽ bị xóa. Không thể khôi phục.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (all) viewModel.deleteAllConversations();
                    else viewModel.deleteCurrentConversation();
                })
                .show();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navConsultation).setSelected(true);
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.navAlarm).setOnClickListener(v -> {
            startActivity(new Intent(this, ScheduleActivity.class));
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.navStore).setOnClickListener(v -> {
            startActivity(new Intent(this, StoreActivity.class));
            overridePendingTransition(0, 0);
        });
    }
}
