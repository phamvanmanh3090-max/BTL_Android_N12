package com.example.medicationreminderapp;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.medicationreminderapp.database.ChatDao;
import com.example.medicationreminderapp.database.ChatDatabase;
import com.example.medicationreminderapp.model.ChatConversation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ChatHistoryTest {
    private static final String TEST_DATABASE = "chat-history-instrumentation.db";
    private Context context;
    private ChatDatabase database;
    private ChatDao dao;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DATABASE);
        openDatabase();
    }

    private void openDatabase() {
        database = Room.databaseBuilder(context, ChatDatabase.class, TEST_DATABASE).build();
        dao = database.chatDao();
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(TEST_DATABASE);
    }

    private long newConversation() {
        ChatConversation conversation = new ChatConversation();
        conversation.updatedAt = System.currentTimeMillis();
        return dao.insertConversation(conversation);
    }

    @Test
    public void messagesSurviveDatabaseReopenInTheirOriginalOrder() {
        long id = newConversation();
        dao.appendMessage(id, "Nên uống thuốc khi nào?", true);
        dao.appendMessage(id, "Hãy làm theo hướng dẫn của bác sĩ.", false);
        database.close();
        openDatabase();

        assertEquals(2, dao.getMessages(id).size());
        assertEquals("Nên uống thuốc khi nào?", dao.getMessages(id).get(0).text);
        assertTrue(dao.getMessages(id).get(0).user);
        assertFalse(dao.getMessages(id).get(1).user);
        assertEquals("Nên uống thuốc khi nào?", dao.getConversation(id).title);
    }

    @Test
    public void conversationsKeepSeparateMessagesAndFirstQuestionTitle() {
        long first = newConversation();
        long second = newConversation();
        dao.appendMessage(first, "Câu hỏi đầu tiên", true);
        dao.appendMessage(second, "Câu hỏi cuộc trò chuyện khác", true);
        dao.appendMessage(first, "Câu hỏi tiếp theo", true);

        assertEquals(2, dao.getMessages(first).size());
        assertEquals(1, dao.getMessages(second).size());
        assertEquals("Câu hỏi đầu tiên", dao.getConversation(first).title);
        assertEquals(second, dao.getMessages(second).get(0).conversationId);
    }

    @Test
    public void deletingConversationRemovesMessagesAndRejectsLateReply() {
        long deleted = newConversation();
        long kept = newConversation();
        dao.appendMessage(deleted, "Câu hỏi đang chờ", true);
        dao.appendMessage(kept, "Giữ tin nhắn này", true);
        dao.deleteConversation(deleted);

        assertFalse(dao.appendMessage(deleted, "Câu trả lời đến muộn", false));
        assertNull(dao.getConversation(deleted));
        assertTrue(dao.getMessages(deleted).isEmpty());
        assertEquals(1, dao.getMessages(kept).size());
    }

    @Test
    public void deletingAllHistoryDoesNotReuseIdsForLateReplies() {
        long first = newConversation();
        long second = newConversation();
        dao.appendMessage(first, "Câu hỏi một", true);
        dao.appendMessage(second, "Câu hỏi hai", true);
        dao.deleteAll();
        assertNull(dao.getLatestConversation());
        assertTrue(dao.getMessages(first).isEmpty());
        assertTrue(dao.getMessages(second).isEmpty());

        long fresh = newConversation();
        assertTrue(fresh > second);
        assertFalse(dao.appendMessage(first, "Trả lời cuộc trò chuyện đã xóa", false));
        assertTrue(dao.getMessages(fresh).isEmpty());
    }
}
