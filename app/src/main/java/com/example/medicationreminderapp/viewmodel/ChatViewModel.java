package com.example.medicationreminderapp.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.medicationreminderapp.database.ChatRepository;
import com.example.medicationreminderapp.model.ChatConversation;
import com.example.medicationreminderapp.model.SavedChatMessage;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChatViewModel extends ViewModel {
    private static final String[] MODEL_NAMES = {
            "gemini-3-flash-preview", "gemini-3.1-flash-lite", "gemini-2.5-flash"
    };
    private final ChatRepository repository;
    private final String apiKey;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Long> activeId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final LiveData<List<SavedChatMessage>> messages;
    private final LiveData<List<ChatConversation>> conversations;

    public ChatViewModel(Context context, String apiKey) {
        this.apiKey = apiKey;
        repository = ChatRepository.getInstance(context);
        messages = Transformations.switchMap(activeId, repository.dao::observeMessages);
        conversations = repository.dao.observeConversations();
        execute(() -> {
            long savedId = repository.preferences.getLong("active_id", 0);
            ChatConversation conversation = repository.dao.getConversation(savedId);
            if (conversation == null) conversation = repository.dao.getLatestConversation();
            long id = conversation != null ? conversation.id : createConversation();
            main.post(() -> selectConversation(id));
        });
    }

    public LiveData<Long> getActiveId() { return activeId; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<SavedChatMessage>> getMessages() { return messages; }
    public LiveData<List<ChatConversation>> getConversations() { return conversations; }
    public LiveData<Set<Long>> getPending() { return repository.getPending(); }

    private long createConversation() {
        ChatConversation conversation = new ChatConversation();
        conversation.updatedAt = System.currentTimeMillis();
        return repository.dao.insertConversation(conversation);
    }

    public void selectConversation(long id) {
        if (activeId.getValue() != null && activeId.getValue() == id) {
            loading.setValue(false);
            return;
        }
        loading.setValue(true);
        repository.preferences.edit().putLong("active_id", id).apply();
        activeId.setValue(id);
        loading.setValue(false);
    }

    public void newConversation() {
        loading.setValue(true);
        execute(() -> {
            long id = createConversation();
            main.post(() -> selectConversation(id));
        });
    }

    public void deleteCurrentConversation() {
        Long id = activeId.getValue();
        if (id == null) return;
        loading.setValue(true);
        execute(() -> {
            repository.dao.deleteConversation(id);
            ChatConversation latest = repository.dao.getLatestConversation();
            long nextId = latest != null ? latest.id : createConversation();
            main.post(() -> selectConversation(nextId));
        });
    }

    public void deleteAllConversations() {
        loading.setValue(true);
        execute(() -> {
            repository.dao.deleteAll();
            long id = createConversation();
            main.post(() -> selectConversation(id));
        });
    }

    public boolean sendMessage(String question) {
        Long id = activeId.getValue();
        String text = question.trim();
        if (id == null || text.isEmpty() || Boolean.TRUE.equals(loading.getValue())
                || !repository.beginRequest(id)) return false;
        repository.executor.execute(() -> {
            try {
                if (!repository.dao.appendMessage(id, text, true)) {
                    repository.finishRequest(id);
                    return;
                }
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    finishReply(id, "Chưa thể kết nối trợ lý AI. Vui lòng cấu hình dịch vụ tư vấn.");
                    return;
                }
                // Include this conversation only, so a reopened chat can be continued.
                StringBuilder prompt = new StringBuilder("Bạn là trợ lý sức khỏe trong ứng dụng nhắc uống thuốc. "
                        + "Trả lời bằng tiếng Việt, ngắn gọn (tối đa khoảng 150 từ), dùng gạch đầu dòng đơn giản, "
                        + "không dùng bảng. Nhắc người dùng hỏi bác sĩ hoặc dược sĩ khi cần.\n\n");
                List<SavedChatMessage> history = repository.dao.getMessages(id);
                for (int i = Math.max(0, history.size() - 20); i < history.size(); i++) {
                    SavedChatMessage message = history.get(i);
                    prompt.append(message.user ? "Người dùng: " : "Trợ lý: ")
                            .append(message.text).append("\n\n");
                }
                send(id, prompt.toString(), 0, 2);
            } catch (RuntimeException e) {
                repository.finishRequest(id);
                reportError(e);
            }
        });
        return true;
    }

    private void send(long id, String prompt, int modelIndex, int retriesLeft) {
        if (repository.dao.getConversation(id) == null) {
            repository.finishRequest(id);
            return;
        }
        try {
            GenerativeModelFutures model = GenerativeModelFutures.from(
                    new GenerativeModel(MODEL_NAMES[modelIndex], apiKey));
            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String text = result.getText();
                    finishReply(id, text == null ? "AI không trả về nội dung nào." : text);
                }

                @Override
                public void onFailure(Throwable t) {
                    String low = String.valueOf(t.getMessage()).toLowerCase(Locale.ROOT);
                    boolean busy = low.contains("overloaded") || low.contains("high demand") || low.contains("503");
                    boolean missing = low.contains("not found") || low.contains("404")
                            || low.contains("not supported") || low.contains("deprecated");
                    if (busy && retriesLeft > 0) {
                        repository.executor.schedule(() -> send(id, prompt, modelIndex, retriesLeft - 1),
                                2, TimeUnit.SECONDS);
                    } else if (missing && modelIndex + 1 < MODEL_NAMES.length) {
                        send(id, prompt, modelIndex + 1, 2);
                    } else {
                        finishReply(id, friendlyError(low));
                    }
                }
            }, repository.executor);
        } catch (RuntimeException e) {
            finishReply(id, "Không thể kết nối trợ lý AI. Vui lòng thử lại sau.");
        }
    }

    private void finishReply(long id, String text) {
        try {
            repository.dao.appendMessage(id, text, false);
        } catch (RuntimeException e) {
            reportError(e);
        } finally {
            repository.finishRequest(id);
        }
    }

    private void execute(Runnable work) {
        repository.executor.execute(() -> {
            try { work.run(); }
            catch (RuntimeException e) { reportError(e); }
        });
    }

    private void reportError(RuntimeException e) {
        Log.e("ChatHistory", "Chat storage operation failed", e);
        main.post(() -> {
            loading.setValue(false);
            error.setValue("Không thể lưu hoặc đọc lịch sử tư vấn. Vui lòng thử lại.");
        });
    }

    private String friendlyError(String low) {
        if (low.contains("api key") || low.contains("api_key") || low.contains("403")
                || low.contains("permission") || low.contains("400")) {
            return "Dịch vụ tư vấn chưa được cấu hình hợp lệ. Vui lòng kiểm tra cấu hình kết nối.";
        }
        if (low.contains("quota") || low.contains("429") || low.contains("rate")) {
            return "Dịch vụ đang đạt giới hạn sử dụng. Vui lòng thử lại sau ít phút.";
        }
        if (low.contains("unable to resolve") || low.contains("timeout") || low.contains("network")) {
            return "Không kết nối được mạng. Vui lòng kiểm tra Internet và thử lại.";
        }
        return "Trợ lý AI đang không khả dụng. Vui lòng thử lại sau.";
    }
}
