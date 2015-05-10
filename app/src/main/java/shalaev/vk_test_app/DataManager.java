package shalaev.vk_test_app;

import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class DataManager {
    private static final EventBus EVENT_BUS = EventBus.getDefault();
    private static DataManager instance = new DataManager();

    private ArrayList<JSONObject> chats = null;
    private SparseArray<JSONObject> users = new SparseArray<>();
    private SparseArray<ArrayList<JSONObject>> messages = new SparseArray<>();

    public static DataManager getInstance() {
        return instance;
    }

    public void requestChatList() {
        VKRequest vkRequest = new VKRequest("messages.getDialogs");
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonData = response.json.optJSONObject("response");
                JSONArray jsonItems = jsonData.optJSONArray("items");
                ArrayList<JSONObject> items = new ArrayList<>();
                int length = jsonItems.length();
                for (int i = 0; i < length; i++) {
                    JSONObject jsonItem = jsonItems.optJSONObject(i);
                    JSONObject jsonMessage = jsonItem.optJSONObject("message");
                    if (jsonMessage.has("chat_id")) {
                        items.add(jsonMessage);
                    }
                }
                EVENT_BUS.post(new ChatListEvent(chats = items));
            }

            @Override
            public void onError(final VKError error) {
                Log.d("VK ERROR", error.toString());
                EVENT_BUS.post(new ErrorEvent(error));
            }
        });
    }

    public void requestMessages(final int chatId) {
        VKRequest vkRequest = new VKRequest(
                "messages.getHistory",
                VKParameters.from("chat_id", chatId));
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonData = response.json.optJSONObject("response");
                JSONArray jsonItems = jsonData.optJSONArray("items");
                ArrayList<JSONObject> items = new ArrayList<>();

                int length = jsonItems.length();
                for (int i = 0; i < length; i++) {
                    JSONObject jsonItem = jsonItems.optJSONObject(i);
                    items.add(jsonItem);
                    messages.get(chatId).add(jsonItem);
                }
                EVENT_BUS.post(new MessagesEvent(items));
            }

            @Override
            public void onError(final VKError error) {
                Log.d("VK ERROR", error.toString());
                EVENT_BUS.post(new ErrorEvent(error));
            }
        });
    }

    public void requestUsers(final ArrayList<Integer> chatIds) {
        VKRequest vkRequest = new VKRequest(
                "messages.getChatUsers",
                VKParameters.from("chat_ids", TextUtils.join(",", chatIds), "fields", "photo"));
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonChats = response.json.optJSONObject("response");

                for (Integer id : chatIds) {
                    JSONArray jsonItems = jsonChats.optJSONArray(String.valueOf(id));
                    ArrayList<JSONObject> items = new ArrayList<>();
                    int length = jsonItems.length();
                    for (int i = 0; i < length; i++) {
                        JSONObject jsonItem = jsonItems.optJSONObject(i);
                        items.add(jsonItem);
                        users.append(jsonItem.optInt("id"), jsonItem);
                    }
                    EVENT_BUS.post(new UsersEvent(id, items));
                }
            }

            @Override
            public void onError(final VKError error) {
                Log.d("VK ERROR", error.toString());
                EVENT_BUS.post(new ErrorEvent(error));
            }
        });
    }

    public JSONObject getUser(final int user_id) {
        return users.get(user_id);
    }

    public ArrayList<JSONObject> getChats() {
        return chats;
    }

    public ArrayList<JSONObject> getChatMessages(final int chatId) {
        return messages.get(chatId);
    }

    public static class ErrorEvent {
        public final VKError error;

        public ErrorEvent(final VKError error) {this.error = error;}
    }

    public static class ChatListEvent {
        public final ArrayList<JSONObject> items;

        public ChatListEvent(final ArrayList<JSONObject> items) {this.items = items;}
    }

    public static class MessagesEvent {
        public final ArrayList<JSONObject> items;

        public MessagesEvent(final ArrayList<JSONObject> items) {this.items = items;}
    }

    public static class UsersEvent {
        public final int chatId;
        public final ArrayList<JSONObject> items;

        public UsersEvent(final int chatId, final ArrayList<JSONObject> items) {
            this.chatId = chatId;
            this.items = items;
        }
    }
}
