package shalaev.vk_test_app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.vk.sdk.util.VKUtil;


public class ChatListActivity extends AbstractActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        setupToolbar(R.id.toolbar);
    }

    @Override
    protected void onAuthSuccess() {
        Log.d("VK", "onAuthSuccess");
    }
}
