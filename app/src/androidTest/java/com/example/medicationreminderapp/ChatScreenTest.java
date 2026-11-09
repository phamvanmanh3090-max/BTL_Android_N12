package com.example.medicationreminderapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.medicationreminderapp.activity.AIChatActivity;
import com.example.medicationreminderapp.database.ChatRepository;
import com.example.medicationreminderapp.model.ChatConversation;
import com.example.medicationreminderapp.viewmodel.ChatViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(AndroidJUnit4.class)
public class ChatScreenTest {
    @Test
    public void historyCanBeOpenedAndNewChatSurvivesScreenRecreation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ChatRepository repository = ChatRepository.getInstance(context);
        long previousSelection = repository.preferences.getLong("active_id", 0);
        List<Long> testIds = new ArrayList<>();
        try {
            long first = seed(repository, "Kiểm thử lịch sử thứ nhất");
            long second = seed(repository, "Kiểm thử lịch sử thứ hai");
            testIds.add(first);
            testIds.add(second);
            repository.preferences.edit().putLong("active_id", first).commit();
            try (ActivityScenario<AIChatActivity> scenario = ActivityScenario.launch(AIChatActivity.class)) {
                awaitMessages(scenario, "Kiểm thử lịch sử thứ nhất");
                onView(withId(R.id.button_chat_history)).perform(click());
                onView(withText(startsWith("Kiểm thử lịch sử thứ hai"))).perform(click());
                awaitMessages(scenario, "Kiểm thử lịch sử thứ hai");

                onView(withId(R.id.button_new_chat)).perform(click());
                awaitMessages(scenario, null);
                AtomicLong newId = new AtomicLong();
                scenario.onActivity(activity -> newId.set(new ViewModelProvider(activity)
                        .get(ChatViewModel.class).getActiveId().getValue()));
                testIds.add(newId.get());
                assertNotEquals(second, newId.get());

                scenario.recreate();
                awaitMessages(scenario, null);
                scenario.onActivity(activity -> assertEquals(Long.valueOf(newId.get()),
                        new ViewModelProvider(activity).get(ChatViewModel.class).getActiveId().getValue()));

                onView(withId(R.id.button_delete_chat)).perform(click());
                onView(withId(android.R.id.button1)).perform(click());
                awaitMessages(scenario, "Kiểm thử lịch sử thứ hai");
                assertNull(repository.dao.getConversation(newId.get()));
            }
        } finally {
            for (long id : testIds) repository.dao.deleteConversation(id);
            repository.preferences.edit().putLong("active_id", previousSelection).commit();
        }
    }

    private long seed(ChatRepository repository, String question) {
        ChatConversation conversation = new ChatConversation();
        conversation.updatedAt = System.currentTimeMillis();
        long id = repository.dao.insertConversation(conversation);
        repository.dao.appendMessage(id, question, true);
        return id;
    }

    private void awaitMessages(ActivityScenario<AIChatActivity> scenario, String question) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        scenario.onActivity(activity -> {
            ChatViewModel model = new ViewModelProvider(activity).get(ChatViewModel.class);
            model.getMessages().observe(activity, messages -> {
                Long id = model.getActiveId().getValue();
                if (id == null || messages == null) return;
                if (question == null && messages.isEmpty()) ready.countDown();
                if (question != null && messages.stream().anyMatch(message ->
                        message.conversationId == id && question.equals(message.text))) ready.countDown();
            });
        });
        assertTrue("Expected saved conversation to appear", ready.await(5, TimeUnit.SECONDS));
    }
}
