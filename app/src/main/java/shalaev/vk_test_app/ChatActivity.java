package shalaev.vk_test_app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import shalaev.vk_test_app.utils.AvatarUtils;


public class ChatActivity extends AbstractActivity {
    private static final int LOAD_MODE_OFFSET = 10;
    public static final String KEY_CHAT_ID = "chat_id";
    private ListView listView;
    private View progressView;
    private Bundle savedState;
    private MessagesAdapter adapter;
    private int chatId;
    private DataManager dataManager;
    private boolean scrollReady = false;
    private int total = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedState = savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupToolbar(R.id.toolbar);
        setupViews();

        dataManager = DataManager.getInstance(this);
    }

    @Override
    protected void setupViews() {
        listView = (ListView) findViewById(R.id.messages_list);
        listView.setAdapter(adapter = new MessagesAdapter(this, null));
        listView.setOnScrollListener(new MessagesScrollListener());

        progressView = findViewById(R.id.messages_list_progress);

        chatId = getIntent().getIntExtra(KEY_CHAT_ID, 0);
        Cursor c = DataManager.getInstance(this).getChat(chatId);
        if (null != c) {
            TextView titleView = (TextView) findViewById(R.id.toolbar_title);
            titleView.setText(c.getString(c.getColumnIndex("title")));

            int usersCount = c.getInt(c.getColumnIndex("users_count"));
            String subtitle = getResources().getQuantityString(R.plurals.subtitle_chat,
                                                               usersCount,
                                                               usersCount);
            TextView subtitleView = (TextView) findViewById(R.id.toolbar_subtitle);
            subtitleView.setText(subtitle);
        } else {
            throw new RuntimeException("Invalid chat id, check extra params!");
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.MessagesReadyEvent event) {
        render(event.cursor);
        total = event.total;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.UsersReadyEvent event) {
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void setupToolbar(final int toolbarId) {
        super.setupToolbar(toolbarId);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onAuthSuccess() {
        dataManager.requestMessages(chatId);
    }

    private void render(final Cursor cursor) {
        adapter.changeCursor(cursor);
        if (listView.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.INVISIBLE);
        }

        scrollReady = true;
    }

    public static final class MessagesAdapter extends CursorAdapter {
        private static final int[] RES = {
                R.layout.list_item_msg_in_first,
                R.layout.list_item_msg_in,
                R.layout.list_item_msg_out
        };
        private static final int TYPE_IN_FIRST = 0;
        private static final int TYPE_IN = 1;
        private static final int TYPE_OUT = 2;
        private final DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        private final LayoutInflater inflater;
        private final Activity activity;

        public MessagesAdapter(final Activity activity, final Cursor c) {
            super(activity, c, 0);
            this.activity = activity;
            inflater = LayoutInflater.from(activity);
        }

        @Override
        public View newView(final Context context, final Cursor c, final ViewGroup parent) {
            View view = inflater.inflate(RES[getItemViewType(c.getPosition())], parent, false);
            view.setTag(new ViewHolder(view));

            return view;
        }

        @Override
        public int getItemViewType(final int position) {
            int type;
            Cursor c = (Cursor) getItem(position);
            if (c.getInt(c.getColumnIndex("out")) > 0) {
                type = TYPE_OUT;
            } else {
                int userId = c.getInt(c.getColumnIndex("user_id"));
                if (c.moveToPrevious()) {
                    if (c.getInt(c.getColumnIndex("user_id")) == userId) {
                        type = TYPE_IN;
                    } else {
                        type = TYPE_IN_FIRST;
                    }
                } else {
                    type = TYPE_IN_FIRST;
                }
            }
            c.moveToPosition(position);

            return type;
        }

        @Override
        public int getViewTypeCount() {
            return RES.length;
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor c) {
            ViewHolder vh = (ViewHolder) view.getTag();
            vh.body.setText(c.getString(c.getColumnIndex("body")));

            long date = c.getInt(c.getColumnIndex("date"));
            vh.time.setText(dateFormat.format(new Date(date * 1000)));

            if (null != vh.avatar) {
                String avatarUrl = c.getString(c.getColumnIndex("avatar"));
                AvatarUtils.loadAvatar(activity, avatarUrl, vh.avatar);
            }
        }

        private class ViewHolder {
            public final ImageView avatar;
            public final TextView body;
            public final TextView time;

            public ViewHolder(final View view) {
                avatar = (ImageView) view.findViewById(R.id.message_avatar);
                body = (TextView) view.findViewById(R.id.message_body);
                time = (TextView) view.findViewById(R.id.message_time);
            }
        }
    }

    private class MessagesScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(final AbsListView view, final int scrollState) {

        }

        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem,
                             final int visibleItemCount,
                             final int totalItemCount) {
            if (scrollReady && null != adapter) {
                Cursor c = adapter.getCursor();
                if (null != c) {
                    int pos = c.getPosition();
                    int count = c.getCount();
                    if (pos <= LOAD_MODE_OFFSET && total >= 0 && total > count) {
                        scrollReady = false;
                        dataManager.requestMessages(chatId, c.getCount(), DataManager.SERVER);
                    }
                }
            }
        }
    }
}
