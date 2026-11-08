package com.example.medicationreminderapp.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";

    // Thử lần lượt; nếu model không tồn tại thì tự chuyển sang model kế tiếp
    private static final String[] MODEL_NAMES = {
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash"
    };

    private final GenerativeModelFutures[] models = new GenerativeModelFutures[MODEL_NAMES.length];
    private final MutableLiveData<String> chatResponse = new MutableLiveData<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public ChatViewModel(String apiKey) {
        for (int i = 0; i < MODEL_NAMES.length; i++) {
            models[i] = GenerativeModelFutures.from(new GenerativeModel(MODEL_NAMES[i], apiKey));
        }
    }

    public LiveData<String> getChatResponse() {
        return chatResponse;
    }

    public void sendMessage(String messageText) {
        send(messageText, 0, 2);
    }

    private void send(String messageText, int modelIndex, int retriesLeft) {
        String prompt = "Bạn là trợ lý sức khỏe trong ứng dụng nhắc uống thuốc. "
                + "Trả lời bằng tiếng Việt, ngắn gọn (tối đa khoảng 150 từ), dùng gạch đầu dòng đơn giản, "
                + "không dùng bảng. Nhắc người dùng hỏi bác sĩ hoặc dược sĩ khi cần.\n\nCâu hỏi: " + messageText;
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = models[modelIndex].generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                chatResponse.postValue(text != null ? text : "AI không trả về nội dung nào.");
            }

            @Override
            public void onFailure(Throwable t) {
                String raw = t.getMessage() != null ? t.getMessage() : t.toString();
                String low = raw.toLowerCase(Locale.ROOT);
                Log.e(TAG, "Model " + MODEL_NAMES[modelIndex] + " lỗi: " + raw, t);

                boolean busy = low.contains("overloaded") || low.contains("high demand") || low.contains("503");
                boolean modelMissing = low.contains("not found") || low.contains("404")
                        || low.contains("not supported") || low.contains("deprecated");

                if (busy && retriesLeft > 0) {
                    executor.schedule(() -> send(messageText, modelIndex, retriesLeft - 1), 2, TimeUnit.SECONDS);
                } else if (modelMissing && modelIndex + 1 < models.length) {
                    send(messageText, modelIndex + 1, 2);
                } else {
                    chatResponse.postValue("⚠️ " + friendlyError(low, raw));
                }
            }
        }, executor);
    }

    private String friendlyError(String low, String raw) {
        if (low.contains("api key") || low.contains("api_key") || low.contains("403")
                || low.contains("permission") || low.contains("400")) {
            return "API key không hợp lệ hoặc đã bị vô hiệu hóa. Hãy tạo key mới và đặt vào local.properties.\n\n(" + raw + ")";
        }
        if (low.contains("quota") || low.contains("429") || low.contains("rate")) {
            return "Đã hết hạn mức gọi AI, vui lòng thử lại sau ít phút.\n\n(" + raw + ")";
        }
        if (low.contains("unable to resolve") || low.contains("timeout") || low.contains("network")) {
            return "Không kết nối được mạng. Kiểm tra Internet của máy/giả lập.\n\n(" + raw + ")";
        }
        if (low.contains("not found") || low.contains("404")) {
            return "Không có model nào trong danh sách còn hoạt động. Cần cập nhật tên model.\n\n(" + raw + ")";
        }
        return raw;
    }
}