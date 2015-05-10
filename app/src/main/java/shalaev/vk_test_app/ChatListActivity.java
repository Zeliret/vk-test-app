package shalaev.vk_test_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import shalaev.vk_test_app.utils.AvatarUtils;


public class ChatListActivity extends AbstractActivity {
    private ListView listView;
    private View progressView;
    private Bundle savedState;
    private ChatListAdapter adapter;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedState = savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        setupToolbar(R.id.toolbar);
        setupViews();

        dataManager = DataManager.getInstance(this);
    }

    @Override
    protected void setupViews() {
        listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter = new ChatListAdapter(this, null));
        listView.setOnItemClickListener(new ListItemClickListener());

        progressView = findViewById(R.id.chat_list_progress);
    }

    private void openChat(final int chatId) {
        Intent intent = new Intent(this, ChatActivity.class)
                .putExtra(ChatActivity.KEY_CHAT_ID, chatId);
        startActivity(intent);
    }

    @Override
    protected void onAuthSuccess() {
        dataManager.requestChats(DataManager.CACHE);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.ChatsReadyEvent event) {
        requestUsers(event.cursor);
        render(event.cursor);
    }

    private void requestUsers(final Cursor c) {
        Integer[] ids = new Integer[c.getCount()];
        while (c.moveToNext()) {
            ids[c.getPosition()] = c.getInt(c.getColumnIndex("chat_id"));
        }
        dataManager.requestUsersBatch(ids);
    }

    private void render(final Cursor cursor) {
        adapter.changeCursor(cursor);
        if (listView.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.INVISIBLE);
        }
    }

    public static final class ChatListAdapter extends CursorAdapter {
        private static final int RESOURCE_ID = R.layout.list_item_chat;
        private final DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        private final LayoutInflater inflater;
        private final Activity activity;

        public ChatListAdapter(final Activity activity, final Cursor c) {
            super(activity, c, 0);
            this.activity = activity;
            inflater = LayoutInflater.from(activity);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            View view = inflater.inflate(RESOURCE_ID, parent, false);
            view.setTag(new ViewHolder(view));

            return view;
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor c) {
            ViewHolder vh = (ViewHolder) view.getTag();
            vh.title.setText(c.getString(c.getColumnIndex("title")));
            vh.message.setText(c.getString(c.getColumnIndex("body")));

            long date = c.getInt(c.getColumnIndex("date"));
            vh.time.setText(dateFormat.format(new Date(date * 1000)));

            String avatarUrl = c.getString(c.getColumnIndex("avatar"));
            AvatarUtils.loadAvatar(activity, avatarUrl, vh.avatar);
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
            Cursor c = (Cursor) adapter.getItem(position);
            openChat(c.getInt(c.getColumnIndex("chat_id")));
        }
    }
}
