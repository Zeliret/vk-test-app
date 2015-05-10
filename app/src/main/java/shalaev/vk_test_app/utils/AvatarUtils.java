package shalaev.vk_test_app.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public final class AvatarUtils {
    public static void loadAvatar(final Context context, final String url,
                                  final ImageView imageView) {
        Glide.with(context)
             .load(url)
             .transform(new CircleTransform(context))
             .into(imageView);
    }
}
