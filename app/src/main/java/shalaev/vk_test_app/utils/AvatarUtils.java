package shalaev.vk_test_app.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import shalaev.vk_test_app.R;

public final class AvatarUtils {
    public static void loadAvatar(final Activity activity, final String url,
                                  final ImageView imageView) {
        Glide.with(activity)
             .load(url)
             .transform(new CircleTransform(activity))
             .error(R.drawable.avatar_dummy)
             .crossFade()
             .into(imageView);
    }
}
