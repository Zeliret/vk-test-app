package shalaev.vk_test_app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;


public class ChatListActivity extends AbstractActivity implements Observer {
    private ListView list;
    private ChatListAdapter adapter;
    private View progress;
    private ChatListManager chatListManager;
    private Bundle savedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedState = savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        setupToolbar(R.id.toolbar);
        setupViews();
    }

    private void setupViews() {
        list = (ListView) findViewById(R.id.chat_list);
        list.setAdapter(adapter = new ChatListAdapter(this));

        progress = findViewById(R.id.chat_list_progress);
    }

    @Override
    protected void onAuthSuccess() {
        initManagerFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatListManager.deleteObserver(this);
    }

    private void initManagerFragment() {
        FragmentManager fm = getSupportFragmentManager();
        ManagerFragment fragment = (ManagerFragment) fm.findFragmentByTag(ManagerFragment.TAG);
        if (null == fragment) {
            fragment = new ManagerFragment();
            fm.beginTransaction()
              .add(fragment, ManagerFragment.TAG)
              .commit();
        }

        chatListManager = fragment.manager;
        chatListManager.addObserver(ChatListActivity.this);
        chatListManager.request(null == savedState);
    }

    @Override
    public void update(final Observable observable, final Object data) {
        if (observable instanceof ChatListManager) {
            if (data instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<JSONObject> items = (ArrayList<JSONObject>) data;
                updateAdapter(items);
            } else if (data instanceof String) {
                // TODO: error
                Toast.makeText(this, (String) data, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateAdapter(final ArrayList<JSONObject> items) {
        adapter.clear();
        adapter.addAll(items);
        if (list.getVisibility() != View.VISIBLE) {
            list.setVisibility(View.VISIBLE);
            progress.setVisibility(View.INVISIBLE);
        }
    }

    public static final class ManagerFragment extends Fragment {
        public static final String TAG = ManagerFragment.class.getName();
        public final ChatListManager manager = new ChatListManager();

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
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

            JSONObject item = getItem(position);
            vh.title.setText(item.optString("title"));
            vh.message.setText(item.optString("body"));
            vh.time.setText(dateFormat.format(new Date(item.optLong("date") * 1000)));

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private class ViewHolder {
            public final TextView title;
            public final TextView message;
            public final TextView time;

            public ViewHolder(final View view) {
                title = (TextView) view.findViewById(R.id.chat_title);
                message = (TextView) view.findViewById(R.id.chat_message);
                time = (TextView) view.findViewById(R.id.chat_time);
            }
        }
    }
}
