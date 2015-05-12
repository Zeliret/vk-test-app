package shalaev.vk_test_app;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import shalaev.vk_test_app.utils.DataUtils;
import shalaev.vk_test_app.utils.ImageUtils;


public class ChatActivity extends AbstractActivity {
    public static final String KEY_CHAT = "chat";
    private static final int THRESHOLD = 8;
    private ListView listView;
    private View progressView;
    private View headerProgressView;
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
        View headerView = getLayoutInflater().inflate(R.layout.list_item_msg_progress, null);
        headerProgressView = headerView.findViewById(R.id.header_progress);
        listView = (ListView) findViewById(R.id.messages_list);
        listView.addHeaderView(headerView, null, false);
        listView.setAdapter(adapter = new MessagesAdapter(this));
        listView.setOnScrollListener(new MessagesScrollListener());

        progressView = findViewById(R.id.messages_list_progress);

        try {
            JSONObject chat = new JSONObject(getIntent().getStringExtra(KEY_CHAT));
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
        if (event.offset > 0) {
            renderAppend(event.items);
        } else {
            render(event.items);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final DataManager.ErrorEvent event) {
        displayError(event.error.errorMessage);
    }

    private void renderAppend(final ArrayList<JSONObject> items) {
        if (items.size() > 0) {
            int firstVisibleItem = listView.getFirstVisiblePosition();
            int oldCount = adapter.getCount();
            View view = listView.getChildAt(0);
            int pos = (view == null ? 0 : view.getBottom());

            adapter.addAll(items);
            listView.setSelectionFromTop(firstVisibleItem + adapter.getCount() - oldCount + 1, pos);
            scrollReady = true;
        }
        headerProgressView.setVisibility(View.GONE);
    }

    private void render(final ArrayList<JSONObject> items) {
        adapter.clear();
        adapter.addAll(items);
        if (adapter.getCount() > 0) {
            listView.setSelection(adapter.getCount() - 1);
        }
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
        private final SparseArray<ArrayList<ImageView>> attachments = new SparseArray<>();

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

            loadBody(message, vh.body);
            loadTime(message, vh.time);
            loadAvatar(message, vh.avatar);
            loadAttachments(message, vh.attachments);

            return view;
        }

        private void loadTime(final JSONObject message, final TextView textView) {
            textView.setText(dateFormat.format(new Date(message.optLong("date") * 1000)));
        }

        private void loadBody(final JSONObject message, final TextView textView) {
            String body = message.optString("body");
            textView.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(body)) {
                textView.setText(body);
            } else {
                textView.setVisibility(View.GONE);
            }
        }

        private void loadAvatar(final JSONObject message, final ImageView imageView) {
            if (null != imageView) {
                DataManager dataManager = DataManager.getInstance();
                JSONObject user = dataManager.getUser(message.optInt("user_id"));
                if (null != user) {
                    ImageUtils.loadSimpleAvatar(getContext(), user.optString("photo_200"), imageView);
                }
            }
        }

        private void loadAttachments(final JSONObject message, final ViewGroup viewGroup) {
            int viewCount = viewGroup.getChildCount();
            for (int i = 0; i < viewCount; i++) {
                viewGroup.removeViewAt(i);
            }

            if (message.has("attachments")) {
                int id = message.optInt("id");
                ArrayList<ImageView> imageViews = attachments.get(id);
                if (null != imageViews) {
                    for (ImageView imageView : imageViews) {
                        viewGroup.addView(imageView);
                    }
                } else {
                    imageViews = new ArrayList<>();

                    JSONArray jsonAttachments = message.optJSONArray("attachments");
                    int length = jsonAttachments.length();
                    for (int i = 0; i < length; i++) {
                        JSONObject jsonAttachment = jsonAttachments.optJSONObject(i);
                        String type = jsonAttachment.optString("type");
                        JSONObject jsonAttachmentData = jsonAttachment.optJSONObject(type);
                        String url = DataUtils.getPhotoUrl(getContext(),
                                                           jsonAttachmentData,
                                                           type);
                        if (null != url) {
                            ImageView imageView = new ImageView(getContext());
                            imageView.setAdjustViewBounds(true);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                            viewGroup.addView(imageView);
                            imageViews.add(imageView);
                            ImageUtils.loadAttachment(getContext(), url, imageView);
                        }
                    }
                    attachments.put(id, imageViews);
                }
            }
        }

        private int pxToDp(final int px) {
            Resources resources = getContext().getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            return (int) (px / (metrics.densityDpi / 160f));
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
            public final LinearLayout attachments;

            public ViewHolder(final View view) {
                avatar = (ImageView) view.findViewById(R.id.message_avatar);
                body = (TextView) view.findViewById(R.id.message_body);
                time = (TextView) view.findViewById(R.id.message_time);
                attachments = (LinearLayout) view.findViewById(R.id.message_attachments);
            }
        }
    }

    private class MessagesScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(final AbsListView view, final int scrollState) {}

        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem,
                             final int visibleItemCount,
                             final int totalItemCount) {
            if (scrollReady && firstVisibleItem > 0 && firstVisibleItem < THRESHOLD) {
                scrollReady = false;

                dataManager.requestMessages(chatId, totalItemCount);
                headerProgressView.setVisibility(View.VISIBLE);
            }
        }
    }
}
