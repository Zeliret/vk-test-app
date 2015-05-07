package shalaev.vk_test_app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class ChatActivity extends AbstractActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
    }

    @Override
    protected void onAuthSuccess() {

    }
}
