package shalaev.vk_test_app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;

import shalaev.vk_test_app.model.ChatManager;


public class ChatActivity extends AbstractActivity {
    public static final String KEY_CHAT = "chat";
    private ListView listView;
    private View progressView;
    private ManagerFragment fragment;
    private ChatManager chatManager;
    private JSONObject chat;
    private Bundle savedState;
    private MessagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedState = savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupToolbar(R.id.toolbar);
        setupViews();
        setupManagerFragment();
    }

    @Override
    protected void setupManagerFragment() {
        FragmentManager fm = getSupportFragmentManager();
        fragment = (ManagerFragment) fm.findFragmentByTag(ManagerFragment.TAG);
        if (null == fragment) {
            fragment = ManagerFragment.newInstance(chat.optInt("chat_id"));
            fm.beginTransaction()
              .add(fragment, ManagerFragment.TAG)
              .commit();
        }
    }

    @Override
    protected void setupViews() {
        listView = (ListView) findViewById(R.id.messages_list);
        listView.setAdapter(adapter = new MessagesAdapter(this));

        progressView = findViewById(R.id.messages_list_progress);

        try {
            chat = new JSONObject(getIntent().getStringExtra(KEY_CHAT));
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
        chatManager = fragment.getManager();
        chatManager.addObserver(ChatActivity.this);
        chatManager.request(null == savedState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatManager.deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object data) {
        if (observable instanceof ChatManager) {
            if (data instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<JSONObject> items = (ArrayList<JSONObject>) data;
                render(items);
            } else if (data instanceof String) {
                // TODO: error
                Toast.makeText(this, (String) data, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void render(final ArrayList<JSONObject> items) {
        adapter.clear();
        adapter.addAll(items);
        if (listView.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.VISIBLE);
            progressView.setVisibility(View.INVISIBLE);
        }
    }

    public static final class ManagerFragment extends Fragment {
        public static final String TAG = ManagerFragment.class.getName();
        private static final String KEY_CHAT_ID = "chat_id";
        public ChatManager manager;

        public static ManagerFragment newInstance(final int chatId) {
            Bundle args = new Bundle();
            args.putInt(KEY_CHAT_ID, chatId);

            ManagerFragment fragment = new ManagerFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onActivityCreated(final Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            int chatId = getArguments().getInt(KEY_CHAT_ID);
            if (chatId <= 0) {
                throw new IllegalStateException("Chat id must be positive above zero!");
            }
            if (null == manager) {
                manager = new ChatManager(getActivity(), chatId);
            }
        }

        public ChatManager getManager() {
            return manager;
        }
    }

    public static final class MessagesAdapter extends ArrayAdapter<JSONObject> {
        private static final int[] RES = {
                R.layout.list_item_msg_in,
                R.layout.list_item_msg_out
        };
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
            return 2;
        }

        @Override
        public int getItemViewType(final int position) {
            JSONObject message = getItem(position);

            return message.optInt("out");
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private class ViewHolder {
            public final TextView body;
            public final TextView time;

            public ViewHolder(final View view) {
                body = (TextView) view.findViewById(R.id.message_body);
                time = (TextView) view.findViewById(R.id.message_time);
            }
        }
    }
}
