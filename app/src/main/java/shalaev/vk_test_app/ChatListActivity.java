package shalaev.vk_test_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import shalaev.vk_test_app.utils.ImageUtils;


public class ChatListActivity extends AbstractActivity {
    private ListView listView;
    private View progressView;
    private ChatListAdapter adapter;
    private Bundle savedState;
    private DataManager dataManager = DataManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedState = savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        setupToolbar(R.id.toolbar);
        setupViews();
    }

    @Override
    protected void setupViews() {
        listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter = new ChatListAdapter(this));
        listView.setOnItemClickListener(new ListItemClickListener());

        progressView = findViewById(R.id.chat_list_progress);
    }

    private void openChat(final JSONObject chatItem) {
        Intent intent = new Intent(this, ChatActivity.class)
                .putExtra(ChatActivity.KEY_CHAT, chatItem.toString());
        startActivity(intent);
    }

    @Override
    protected void onAuthSuccess() {
        ArrayList<JSONObject> chats = dataManager.getChats();
        if (null == chats) {
            dataManager.requestChatList();
        } else {
            requestUsers(chats);
            render(chats);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.ChatListEvent event) {
        requestUsers(event.items);
        render(event.items);
    }

    private void requestUsers(final ArrayList<JSONObject> items) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (JSONObject item : items) {
            ids.add(item.optInt("chat_id"));
        }
        dataManager.requestUsers(ids);
    }

    private void render(final ArrayList<JSONObject> items) {
        adapter.clear();
        adapter.addAll(items);
        if (listView.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.INVISIBLE);
        }
    }

    public static final class ChatListAdapter extends ArrayAdapter<JSONObject> {
        private static final int RESOURCE_ID = R.layout.list_item_chat;
        private final LayoutInflater inflater;
        private final DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        public ChatListAdapter(final Context context) {
            super(context, RESOURCE_ID, new ArrayList<JSONObject>());
            inflater = LayoutInflater.from(context);
        }

        @Override
        public long getItemId(final int position) {
            return getItem(position).optInt("chat_id");
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if (null == view) {
                view = inflater.inflate(RESOURCE_ID, parent, false);
                view.setTag(vh = new ViewHolder(view));
            } else {
                vh = (ViewHolder) view.getTag();
            }

            JSONObject chat = getItem(position);
            vh.title.setText(chat.optString("title"));
            vh.message.setText(chat.optString("body"));
            vh.time.setText(dateFormat.format(new Date(chat.optLong("date") * 1000)));

            ImageUtils.loadAvatar(getContext(), chat.optString("photo_200"), vh.avatar);

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private class ViewHolder {
            public final ImageView avatar;
            public final TextView title;
            public final TextView message;
            public final TextView time;

            public ViewHolder(final View view) {
                avatar = (ImageView) view.findViewById(R.id.chat_avatar);
                title = (TextView) view.findViewById(R.id.chat_title);
                message = (TextView) view.findViewById(R.id.chat_message);
                time = (TextView) view.findViewById(R.id.chat_time);
            }
        }
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                                final long id) {
            JSONObject item = adapter.getItem(position);
            openChat(item);
        }
    }
}
