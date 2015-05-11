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
import shalaev.vk_test_app.utils.ImageUtils;
import shalaev.vk_test_app.utils.ImageUtils.Collage;

public class DataManager {
    private static final EventBus EVENT_BUS = EventBus.getDefault();
    private static DataManager instance = new DataManager();

    private ArrayList<JSONObject> chats = null;
    private SparseArray<JSONObject> users = new SparseArray<>();
    private SparseArray<ArrayList<JSONObject>> messages = new SparseArray<>();
    private SparseArray<Integer> lastChatMessagesIds = new SparseArray<>();
    private SparseArray<Collage> chatCollages = new SparseArray<>();

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

    public void requestMessages(final int chatId) {requestMessages(chatId, 0);}

    public void requestMessages(final int chatId, final int offset) {
        VKRequest vkRequest = new VKRequest(
                "messages.getHistory",
                VKParameters.from("chat_id", chatId, "offset", offset));
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonData = response.json.optJSONObject("response");
                JSONArray jsonItems = jsonData.optJSONArray("items");

                ArrayList<JSONObject> items = processMessages(chatId, jsonItems);
                EVENT_BUS.post(new MessagesEvent(items, offset));
            }

            @Override
            public void onError(final VKError error) {
                Log.d("VK ERROR", error.toString());
                EVENT_BUS.post(new ErrorEvent(error));
            }
        });
    }

    private ArrayList<JSONObject> processMessages(final int chatId, final JSONArray jsonItems) {
        ArrayList<JSONObject> items = new ArrayList<>();
        int length = jsonItems.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonItem = jsonItems.optJSONObject(i);
            if (!jsonItem.has("action")) {
                items.add(jsonItem);
            }
            lastChatMessagesIds.put(chatId, jsonItem.optInt("id"));
        }
        ArrayList<JSONObject> chatMessages = messages.get(chatId, new ArrayList<JSONObject>());
        chatMessages.addAll(items);
        messages.put(chatId, chatMessages);
        return items;
    }

    public void requestUsers(final ArrayList<Integer> chatIds) {
        VKRequest vkRequest = new VKRequest(
                "messages.getChatUsers",
                VKParameters.from("chat_ids", TextUtils.join(",", chatIds), "fields", "photo_200"));
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonChats = response.json.optJSONObject("response");
                for (Integer id : chatIds) {
                    Collage collage = new Collage();
                    JSONArray jsonItems = jsonChats.optJSONArray(String.valueOf(id));
                    ArrayList<JSONObject> items = new ArrayList<>();
                    int length = jsonItems.length();
                    for (int i = 0; i < length; i++) {
                        JSONObject jsonItem = jsonItems.optJSONObject(i);
                        items.add(jsonItem);
                        users.append(jsonItem.optInt("id"), jsonItem);
                        collage.addUserPhoto(jsonItem.optString("photo_200"));
                    }
                    chatCollages.put(id, collage);
                    EVENT_BUS.post(new UsersEvent(id, items));
                }
                EVENT_BUS.post(new UsersBulkEvent());
            }

            @Override
            public void onError(final VKError error) {
                Log.d("VK ERROR", error.toString());
                EVENT_BUS.post(new ErrorEvent(error));
            }
        });
    }

    public void requestLongPoll(final int chatId) {
        VKRequest vkRequest = new VKRequest("messages.getLongPollServer");
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonInfo = response.json.optJSONObject("response");
                connectLongPollServer(chatId, jsonInfo.optInt("ts"));
            }

            @Override
            public void onError(final VKError error) {
                Log.d("VK ERROR", error.toString());
                EVENT_BUS.post(new ErrorEvent(error));
            }
        });
    }

    private void connectLongPollServer(final int chatId, final int ts) {
        int lastMessageId = lastChatMessagesIds.get(chatId);

        VKRequest vkRequest = new VKRequest(
                "messages.getLongPollHistory",
                VKParameters.from("ts", ts, "max_msg_id", lastMessageId));
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);

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

    public Collage getCollage(final int chatId) {
        return chatCollages.get(chatId);
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
        public final int offset;

        public MessagesEvent(final ArrayList<JSONObject> items, final int offset) {
            this.items = items;
            this.offset = offset;
        }
    }

    public static class UsersEvent {
        public final int chatId;
        public final ArrayList<JSONObject> items;

        public UsersEvent(final int chatId, final ArrayList<JSONObject> items) {
            this.chatId = chatId;
            this.items = items;
        }
    }

    public static class UsersBulkEvent {
    }
}
