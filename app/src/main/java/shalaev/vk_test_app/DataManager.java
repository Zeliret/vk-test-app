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
    private static final EventBus EVENT_BUS = EventBus.getDefault();
    private static final EventBus LOCAL_EVENT_BUS = new EventBus();
    public static int CACHE = 1;
    public static int SERVER = 2;
    public static int CACHE_SERVER = CACHE | SERVER;
    private static DataManager instance;
    private DBManager dbManager;

    private DataManager(final Context context) {
        if (null == dbManager) {
            dbManager = new DBManager(context);
        }
    }

    public static DataManager getInstance(final Context context) {
        if (null == instance) {
            instance = new DataManager(context);
        }
        return instance;
    }

    private Cursor getChats() {
        return read().rawQuery("select * from chats", new String[]{});
    }

    public Cursor getChat(final int id) {
        Cursor cursor = read().rawQuery("select * from chats where chat_id = ?",
                                        new String[]{String.valueOf(id)});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor;
        }
        return null;
    }

    public void requestChats() {requestChats(CACHE_SERVER);}

    public void requestChats(final int flags) {
        if ((flags & CACHE) == 1) {
            if (getMark("chats")) {
                EVENT_BUS.post(new ChatsReadyEvent(getChats()));
            }
        }

        if ((flags & SERVER) == 1 || !getMark("chats")) {
            VKRequest vkRequest = new VKRequest("messages.getDialogs");
            vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(final VKResponse response) {
                    Log.d("VK", response.responseString);
                    JSONObject jsonData = response.json.optJSONObject("response");
                    JSONArray jsonItems = jsonData.optJSONArray("items");
                    storeChats(jsonItems);
                    EVENT_BUS.post(new ChatsReadyEvent(getChats()));
                }

                @Override
                public void onError(final VKError error) {
                    // TODO: error
                    Log.d("VK", error.toString());
                }
            });
        }
    }

    private void storeChats(final JSONArray chats) {
        int length = chats.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonItem = chats.optJSONObject(i);
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

    private Cursor getMessages(final int chatId) {
        return read().rawQuery(
                "select m.*, u.* from messages as m " +
                        "left join users as u on m.user_id = u.user_id " +
                        "where m.chat_id = ? order by m.message_id asc",
                new String[]{String.valueOf(chatId)});
    }

    public void requestMessages(final int chatId) {requestMessages(chatId, CACHE_SERVER);}

    public void requestMessages(final int chatId, final int flags) {
        boolean mark = getMark(String.format("messages:%d", chatId));
        if ((flags & CACHE) == 1) {
            if (mark) {
                EVENT_BUS.post(new MessagesReadyEvent(getMessages(chatId)));
            }
        }

        if ((flags & SERVER) == 1 || !mark) {
            VKRequest vkRequest = new VKRequest(
                    "messages.getHistory",
                    VKParameters.from("chat_id", chatId));
            vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(final VKResponse response) {
                    Log.d("VK", response.responseString);
                    JSONObject jsonData = response.json.optJSONObject("response");
                    JSONArray jsonItems = jsonData.optJSONArray("items");
                    storeMessages(chatId, jsonItems);
                    EVENT_BUS.post(new MessagesReadyEvent(getMessages(chatId)));
                }

                @Override
                public void onError(final VKError error) {
                    // TODO: error
                    Log.d("VK", error.toString());
                }
            });
        }
    }

    public void requestUsersBatch(final Integer[] chatIds) {
        requestUsersBatch(chatIds, CACHE_SERVER);
    }

    public void requestUsersBatch(final Integer[] chatIds, final int flags) {
        ArrayList<Integer> ids = new ArrayList<>(Arrays.asList(chatIds));
        if ((flags & CACHE) == 1) {
            for (Integer id : chatIds) {
                if (getMark(String.format("users:%s", id))) {
                    ids.remove(id);
                }
            }
        }

        if ((flags & SERVER) == 1 || ids.size() == chatIds.length) {
            VKRequest vkRequest = new VKRequest(
                    "messages.getChatUsers",
                    VKParameters.from("chat_ids", TextUtils.join(",", ids),
                                      "fields", "photo"));
            vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(final VKResponse response) {
                    Log.d("VK", response.responseString);
                    JSONObject jsonChats = response.json.optJSONObject("response");
                    storeUsers(chatIds, jsonChats);
                    EVENT_BUS.post(new UsersReadyEvent());
                }

                @Override
                public void onError(final VKError error) {
                    // TODO: error
                    Log.d("VK", error.toString());
                }
            });
        }
    }

    private void storeUsers(final Integer[] chatIds, final JSONObject chats) {
        for (int id : chatIds) {
            String key = String.valueOf(id);
            if (chats.has(key)) {
                JSONArray users = chats.optJSONArray(key);
                int usersCount = users.length();
                for (int i = 0; i < usersCount; i++) {
                    JSONObject user = users.optJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("user_id", user.optInt("id"));
                    values.put("avatar", user.optString("photo"));

                    write().insert("users", null, values);
                }
                setMark(String.format("users:%d", id));
            }
        }
    }

    private void storeMessages(final int chatId, final JSONArray messages) {
        int length = messages.length();
        for (int i = 0; i < length; i++) {
            JSONObject message = messages.optJSONObject(i);
            if (!message.has("action")) {
                ContentValues values = new ContentValues();
                values.put("message_id", message.optInt("id"));
                values.put("chat_id", message.optInt("chat_id"));
                values.put("user_id", message.optInt("user_id"));
                values.put("date", message.optInt("date"));
                values.put("body", message.optString("body"));
                values.put("out", message.optInt("out"));

                write().insert("messages", null, values);
            }
        }
        setMark(String.format("messages:%d", chatId));
    }

    public static class ChatsReadyEvent {
        public final Cursor cursor;

        public ChatsReadyEvent(final Cursor cursor) {this.cursor = cursor;}
    }

    public static class MessagesReadyEvent {
        public final Cursor cursor;

        public MessagesReadyEvent(final Cursor cursor) {
            this.cursor = cursor;
        }
    }

    public static class UsersReadyEvent {
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
