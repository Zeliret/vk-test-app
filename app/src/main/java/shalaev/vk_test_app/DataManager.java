package shalaev.vk_test_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import de.greenrobot.event.EventBus;

public class DataManager {
    public static final int COUNT = 50;
    private static final EventBus EVENT_BUS = EventBus.getDefault();
    private static final EventBus LOCAL_EVENT_BUS = new EventBus();
    public static int CACHE = 1;
    public static int SERVER = 2;
    public static int CACHE_SERVER = CACHE | SERVER;
    private static DataManager instance;
    private DBManager dbManager;

    private DataManager(final Context context) {
        dbManager = new DBManager(context);
        LOCAL_EVENT_BUS.register(this);
    }

    public static DataManager getInstance(final Context context) {
        if (null == instance) {
            instance = new DataManager(context);
        }
        return instance;
    }

    @SuppressWarnings("unused")
    public void onEventAsync(final TaskEvent event) {
        event.run();
    }

    public void requestChats() {requestChats(CACHE_SERVER);}

    public void requestChats(final int flags) {
        LOCAL_EVENT_BUS.post(new ChatsTask(flags));
    }

    private SQLiteDatabase read() {
        return dbManager.getReadableDatabase();
    }

    private SQLiteDatabase write() {
        return dbManager.getWritableDatabase();
    }

    private boolean getMark(final String key) {
        String timestamp = null;

        Cursor cursor = read().rawQuery("select timestamp from _marks where key = ?",
                                        new String[]{key});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
            cursor.close();
        }

        return timestamp != null;
    }

    private void setMark(final String key) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        write().insert("_marks", null, values);
    }

    public void requestMessages(final int chatId) {requestMessages(chatId, 0);}

    public void requestMessages(final int chatId, final int offset) {
        requestMessages(chatId, offset, CACHE_SERVER);
    }

    public void requestMessages(final int chatId, final int offset, final int flags) {
        LOCAL_EVENT_BUS.post(new MessagesTask(chatId, offset, flags));
    }

    private int getTotalFromResponse(final VKResponse response) {
        return response.json.optJSONObject("response").optInt("count");
    }

    public void requestUsersBatch(final Integer[] chatIds) {
        requestUsersBatch(chatIds, CACHE_SERVER);
    }

    public void requestUsersBatch(final Integer[] chatIds, final int flags) {
        LOCAL_EVENT_BUS.post(new ChatsUsersTask(chatIds, flags));
    }

    public void requestChat(final int chatId) {
        LOCAL_EVENT_BUS.post(new ChatTask(chatId));
    }

    private interface TaskEvent extends Runnable { }

    public static class CursorEvent {
        public final boolean fromCache;
        public final Cursor cursor;

        public CursorEvent(final boolean fromCache, final Cursor cursor) {
            this.fromCache = fromCache;
            this.cursor = cursor;
        }
    }

    public static class ChatReadyEvent extends CursorEvent {
        public ChatReadyEvent(final Cursor cursor) {
            super(true, cursor);
        }
    }

    public static class ChatsReadyEvent extends CursorEvent {

        protected ChatsReadyEvent(final Cursor cursor) {this(false, cursor);}

        protected ChatsReadyEvent(final boolean fromCache, final Cursor cursor) {
            super(fromCache, cursor);
        }
    }

    public static class MessagesReadyEvent extends CursorEvent {
        public final int total;

        public MessagesReadyEvent(final Cursor cursor, final int total) {
            this(false, cursor, total);
        }

        public MessagesReadyEvent(final boolean fromCache, final Cursor cursor) {
            this(fromCache, cursor, 0);
        }

        public MessagesReadyEvent(final boolean fromCache, final Cursor cursor, final int total) {
            super(fromCache, cursor);
            this.total = total;
        }
    }

    public static class UsersReadyEvent {
    }

    private class ChatTask implements TaskEvent {
        private final int chatId;

        public ChatTask(final int chatId) {
            this.chatId = chatId;
        }

        @Override
        public void run() {
            Cursor cursor = read().rawQuery("select * from chats where chat_id = ?",
                                            new String[]{String.valueOf(chatId)});
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                EVENT_BUS.post(new ChatReadyEvent(cursor));
            }
        }
    }

    private class ChatsUsersTask implements TaskEvent {
        private final Integer[] chatIds;
        private final int flags;

        public ChatsUsersTask(final Integer[] chatIds, final int flags) {
            this.chatIds = chatIds;
            this.flags = flags;
        }

        @Override
        public void run() {
            ArrayList<Integer> ids = new ArrayList<>(Arrays.asList(chatIds));
            if ((flags & CACHE) == CACHE) {
                for (Integer id : chatIds) {
                    if (getMark(String.format("users:%s", id))) {
                        ids.remove(id);
                    }
                }
            }

            if ((flags & SERVER) == SERVER && ids.size() > 0) {
                VKRequest vkRequest = new VKRequest(
                        "messages.getChatUsers",
                        VKParameters.from("chat_ids", TextUtils.join(",", ids),
                                          "fields", "photo"));
                vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(final VKResponse response) {
                        LOCAL_EVENT_BUS.post(new TaskEvent() {
                            @Override
                            public void run() {
                                Log.d("VK", response.responseString);
                                storeUsers(chatIds, response);
                                EVENT_BUS.post(new UsersReadyEvent());
                            }
                        });
                    }

                    @Override
                    public void onError(final VKError error) {
                        // TODO: error
                        Log.d("VK", error.toString());
                    }
                });
            }
        }

        private void storeUsers(final Integer[] chatIds, final VKResponse response) {
            JSONObject jsonChats = response.json.optJSONObject("response");
            for (int id : chatIds) {
                String key = String.valueOf(id);
                if (jsonChats.has(key)) {
                    JSONArray jsonUsers = jsonChats.optJSONArray(key);
                    int usersCount = jsonUsers.length();
                    for (int i = 0; i < usersCount; i++) {
                        JSONObject jsonUser = jsonUsers.optJSONObject(i);
                        ContentValues values = new ContentValues();
                        values.put("user_id", jsonUser.optInt("id"));
                        values.put("avatar", jsonUser.optString("photo"));

                        write().insert("users", null, values);
                    }
                    setMark(String.format("users:%d", id));
                }
            }
        }
    }

    private class ChatsTask implements TaskEvent {
        private final int flags;

        private ChatsTask(final int flags) {this.flags = flags;}

        @Override
        public void run() {
            if ((flags & CACHE) == CACHE) {
                if (getMark("chats")) {
                    EVENT_BUS.post(new ChatsReadyEvent(true, getChats()));
                }
            }

            if ((flags & SERVER) == SERVER || !getMark("chats")) {
                VKRequest vkRequest = new VKRequest("messages.getDialogs");
                vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(final VKResponse response) {
                        LOCAL_EVENT_BUS.post(new TaskEvent() {
                            @Override
                            public void run() {
                                Log.d("VK", response.responseString);
                                storeChats(response);
                                EVENT_BUS.post(new ChatsReadyEvent(getChats()));
                            }
                        });
                    }

                    @Override
                    public void onError(final VKError error) {
                        // TODO: error
                        Log.d("VK", error.toString());
                    }
                });
            }
        }

        private void storeChats(final VKResponse response) {
            JSONObject jsonData = response.json.optJSONObject("response");
            JSONArray jsonItems = jsonData.optJSONArray("items");
            int length = jsonItems.length();
            for (int i = 0; i < length; i++) {
                JSONObject jsonItem = jsonItems.optJSONObject(i);
                JSONObject jsonMessage = jsonItem.optJSONObject("message");
                if (jsonMessage.has("chat_id")) {
                    ContentValues values = new ContentValues();
                    values.put("chat_id", jsonMessage.optInt("chat_id"));
                    values.put("date", jsonMessage.optInt("date"));
                    values.put("title", jsonMessage.optString("title"));
                    values.put("body", jsonMessage.optString("body"));
                    values.put("avatar", jsonMessage.optString("photo_200"));
                    values.put("users_count", jsonMessage.optInt("users_count"));

                    write().insert("chats", null, values);
                }
            }
            setMark("chats");
        }

        private Cursor getChats() {
            return read().rawQuery("select * from chats", new String[]{});
        }
    }

    private class MessagesTask implements TaskEvent {
        private final int chatId;
        private final int offset;
        private final int flags;

        public MessagesTask(final int chatId, final int offset, final int flags) {
            this.chatId = chatId;
            this.offset = offset;
            this.flags = flags;
        }

        @Override
        public void run() {
            boolean mark = getMark(String.format("messages:%d", chatId));
            if ((flags & CACHE) == CACHE) {
                if (mark) {
                    EVENT_BUS.post(new MessagesReadyEvent(true, getMessages(chatId)));
                }
            }

            if ((flags & SERVER) == SERVER || !mark) {
                VKRequest vkRequest = new VKRequest(
                        "messages.getHistory",
                        VKParameters.from("chat_id", chatId, "count", COUNT, "offset", offset));
                vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(final VKResponse response) {
                        LOCAL_EVENT_BUS.post(new TaskEvent() {
                            @Override
                            public void run() {
                                Log.d("VK", response.responseString);
                                storeMessages(chatId, response);

                                int total = getTotalFromResponse(response);
                                EVENT_BUS.post(new MessagesReadyEvent(getMessages(chatId), total));
                            }
                        });
                    }

                    @Override
                    public void onError(final VKError error) {
                        // TODO: error
                        Log.d("VK", error.toString());
                    }
                });
            }
        }

        private Cursor getMessages(final int chatId) {
            return read().rawQuery(
                    "select m.*, u.* from messages as m " +
                            "left join users as u on m.user_id = u.user_id " +
                            "where m.chat_id = ? order by m.message_id asc",
                    new String[]{String.valueOf(chatId)});
        }

        private void storeMessages(final int chatId, final VKResponse response) {
            JSONObject jsonData = response.json.optJSONObject("response");
            JSONArray jsonItems = jsonData.optJSONArray("items");

            int length = jsonItems.length();
            for (int i = 0; i < length; i++) {
                JSONObject item = jsonItems.optJSONObject(i);
                if (!item.has("action")) {
                    ContentValues values = new ContentValues();
                    values.put("message_id", item.optInt("id"));
                    values.put("chat_id", item.optInt("chat_id"));
                    values.put("user_id", item.optInt("user_id"));
                    values.put("date", item.optInt("date"));
                    values.put("body", item.optString("body"));
                    values.put("out", item.optInt("out"));

                    write().insert("messages", null, values);
                }
            }
            setMark(String.format("messages:%d", chatId));
        }
    }

    private class DBManager extends SQLiteAssetHelper {
        // Configuration
        private static final String DATABASE_NAME = "cache.db";
        private static final int DATABASE_VERSION = 1;

        public DBManager(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
    }
}
