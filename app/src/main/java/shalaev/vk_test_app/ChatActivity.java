package shalaev.vk_test_app;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import shalaev.vk_test_app.utils.AvatarUtils;


public class ChatActivity extends AbstractActivity {
    public static final String KEY_CHAT = "chat";
    private static final int THRESHOLD = 5;
    private ListView listView;
    private View progressView;
    private JSONObject chat;
    private Bundle savedState;
    private MessagesAdapter adapter;
    private DataManager dataManager = DataManager.getInstance();
    private int chatId;
    private boolean scrollReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedState = savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupToolbar(R.id.toolbar);
        setupViews();
    }

    @Override
    protected void setupViews() {
        listView = (ListView) findViewById(R.id.messages_list);
        listView.setAdapter(adapter = new MessagesAdapter(this));
        listView.setOnScrollListener(new MessagesScrollListener());

        progressView = findViewById(R.id.messages_list_progress);

        try {
            chat = new JSONObject(getIntent().getStringExtra(KEY_CHAT));
            chatId = chat.optInt("chat_id");

            TextView titleView = (TextView) findViewById(R.id.toolbar_title);
            titleView.setText(chat.optString("title"));

            int usersCount = chat.optInt("users_count");
            String subtitle = getResources().getQuantityString(R.plurals.subtitle_chat,
                                                               usersCount,
                                                               usersCount);
            TextView subtitleView = (TextView) findViewById(R.id.toolbar_subtitle);
            subtitleView.setText(subtitle);
        } catch (JSONException ignored) {
        }
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
        ArrayList<JSONObject> messages = dataManager.getChatMessages(chatId);
        if (null == messages) {
            dataManager.requestMessages(chatId);
        } else {
            render(messages);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.MessagesEvent event) {
        render(event.items);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.MessagesExtraEvent event) {
        renderAppend(event.items);
    }

    private void renderAppend(final ArrayList<JSONObject> items) {
        int firstVisibleItem = listView.getFirstVisiblePosition();
        int oldCount = adapter.getCount();
        View view = listView.getChildAt(0);
        int pos = (view == null ? 0 :  view.getBottom());

        adapter.addAll(items);
        listView.setSelectionFromTop(firstVisibleItem + adapter.getCount() - oldCount + 1, pos);
        scrollReady = true;
    }

    private void render(final ArrayList<JSONObject> items) {
        adapter.clear();
        adapter.addAll(items);
        if (listView.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.INVISIBLE);
        }
        scrollReady = true;
    }

    public static final class MessagesAdapter extends ArrayAdapter<JSONObject> {
        private static final int[] RES = {
                R.layout.list_item_msg_in_first,
                R.layout.list_item_msg_in,
                R.layout.list_item_msg_out
        };
        private static final int TYPE_IN_FIRST = 0;
        private static final int TYPE_IN = 1;
        private static final int TYPE_OUT = 2;
        private final LayoutInflater inflater;
        private final DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        public MessagesAdapter(final Context context) {
            super(context, 0, new ArrayList<JSONObject>());
            inflater = LayoutInflater.from(context);
        }

        @Override
        public long getItemId(final int position) {
            return getItem(position).optInt("id");
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if (null == view) {
                view = inflater.inflate(RES[getItemViewType(position)], parent, false);
                view.setTag(vh = new ViewHolder(view));
            } else {
                vh = (ViewHolder) view.getTag();
            }

            JSONObject message = getItem(position);
            vh.body.setText(message.optString("body"));
            vh.time.setText(dateFormat.format(new Date(message.optLong("date") * 1000)));

            if (null != vh.avatar) {
                DataManager dataManager = DataManager.getInstance();
                JSONObject user = dataManager.getUser(message.optInt("user_id"));
                if (null != user) {
                    AvatarUtils.loadAvatar(getContext(), user.optString("photo"), vh.avatar);
                }
            }

            return view;
        }

        @Override
        public JSONObject getItem(final int position) {
            int count = getCount();
            int reversePos = (count > 0 ? count - 1 : 0) - position;

            return super.getItem(reversePos);
        }

        @Override
        public int getViewTypeCount() {
            return RES.length;
        }

        @Override
        public int getItemViewType(final int position) {
            JSONObject message = getItem(position);
            if (message.optInt("out") > 0) {
                return TYPE_OUT;
            } else {
                try {
                    JSONObject prevMessage = getItem(position - 1);
                    if (prevMessage.optInt("user_id") == message.optInt("user_id")) {
                        return TYPE_IN;
                    } else {
                        return TYPE_IN_FIRST;
                    }
                } catch (IndexOutOfBoundsException e) {
                    return TYPE_IN_FIRST;
                }
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
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
            if (scrollReady && firstVisibleItem < THRESHOLD) {
                scrollReady = false;

                dataManager.requestMessagesExtra(chatId, totalItemCount);
            }
        }
    }
}
