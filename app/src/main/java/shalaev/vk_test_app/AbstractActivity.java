package shalaev.vk_test_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;

public abstract class AbstractActivity extends AppCompatActivity {
    private static boolean authProgress = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VKUIHelper.onCreate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initAuthentication();
    }

    protected abstract void onAuthSuccess();

    private void initAuthentication() {
        Log.d("VK", "initAuthentication");
        VKSdk.initialize(new AuthListener(), getString(R.string._vk_app_id));
        if (!authProgress) {
            Log.d("VK", "Begin auth");
            authProgress = true;
            if (!VKSdk.isLoggedIn() && !VKSdk.wakeUpSession()) {
                Log.d("VK", "VKSdk.authorize");
                VKSdk.authorize(VKScope.MESSAGES);
            } else {
                Log.d("VK", "Logged in already");
                completeAuth();
            }
        }
    }

    protected void setupToolbar(final int toolbarId) {
        Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        if (null != toolbar) {
            setSupportActionBar(toolbar);
        }
    }

    private void completeAuth() {
        authProgress = false;
        onAuthSuccess();
    }

    private void abortAuth(final String message) {
        authProgress = false;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VKUIHelper.onDestroy(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VKUIHelper.onResume(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
    }

    private class AuthListener extends VKSdkListener {
        @Override
        public void onCaptchaError(final VKError captchaError) {
            // TODO: Captcha dialog
            Log.d("VK", "onCaptchaError");
        }

        @Override
        public void onTokenExpired(final VKAccessToken expiredToken) {
            Log.d("VK", "onTokenExpired");
        }

        @Override
        public void onAccessDenied(final VKError authorizationError) {
            Log.d("VK", "onAccessDenied");
            abortAuth(authorizationError.errorMessage);
        }

        @Override
        public void onReceiveNewToken(final VKAccessToken newToken) {
            Log.d("VK", "onReceiveNewToken");
            completeAuth();
        }
    }
}
