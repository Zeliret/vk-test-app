package shalaev.vk_test_app.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

public final class AvatarUtils {
    public static void loadChatAvatar(final Context context, final JSONObject chat,
                                      final ImageView imageView) {
        if (chat.has("photo_200")) {
            Glide.with(context)
                 .load(chat.optString("photo_200"))
                 .transform(new CircleTransform(context))
                 .into(imageView);
        }
    }
}
