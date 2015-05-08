package shalaev.vk_test_app;

import android.util.Log;

import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Observable;

public class ChatListManager extends Observable {
    private ArrayList<JSONObject> data;

    public void request(final boolean force) {
        if (force) {
            loadData();
        } else {
            if (null != data) {
                deliverData(data);
            } else {
                loadData();
            }
        }
    }

    private void loadData() {
        VKRequest vkRequest = new VKRequest("messages.getDialogs");
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                Log.d("VK", response.responseString);
                JSONObject jsonData = response.json.optJSONObject("response");
                JSONArray jsonItems = jsonData.optJSONArray("items");
                parseItems(jsonItems);
            }

            @Override
            public void onError(final VKError error) {
                deliverError(error.errorMessage);
            }
        });
    }

    private void parseItems(final JSONArray jsonItems) {
        ArrayList<JSONObject> data = new ArrayList<>();

        int length = jsonItems.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonItem = jsonItems.optJSONObject(i);
            JSONObject jsonMessage = jsonItem.optJSONObject("message");
            if (jsonMessage.has("chat_id")) {
                data.add(jsonMessage);
            }
        }
        deliverData(data);
    }

    private void deliverData(final ArrayList<JSONObject> data) {
        this.data = data;
        setChanged();
        notifyObservers(data);
    }

    private void deliverError(final String error) {
        setChanged();
        notifyObservers(error);
    }
}
