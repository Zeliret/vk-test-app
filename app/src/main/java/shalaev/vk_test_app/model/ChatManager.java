package shalaev.vk_test_app.model;

import android.content.Context;
import android.util.Log;

import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatManager extends Manager {
    private int chatId;

    public ChatManager(final Context context, final int chatId) {
        super(context);
        this.chatId = chatId;
    }

    @Override
    protected void loadData() {
        VKRequest vkRequest = new VKRequest(
                "messages.getHistory",
                VKParameters.from("chat_id", chatId));
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonData = response.json.optJSONObject("response");
                JSONArray jsonItems = jsonData.optJSONArray("items");
                ArrayList<JSONObject> data = parseItems(jsonItems);
                deliverData(data);
            }

            @Override
            public void onError(final VKError error) {
                deliverError(error.errorMessage);
            }
        });
    }

    private ArrayList<JSONObject> parseItems(final JSONArray jsonItems) {
        ArrayList<JSONObject> data = new ArrayList<>();

        int length = jsonItems.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonItem = jsonItems.optJSONObject(i);
            data.add(jsonItem);
        }
        return data;
    }
}
