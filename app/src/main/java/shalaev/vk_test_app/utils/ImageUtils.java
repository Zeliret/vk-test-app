package shalaev.vk_test_app.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import shalaev.vk_test_app.R;

public final class ImageUtils {
    public static void loadAvatar(final Context context, final String url,
                                  final ImageView imageView) {
        Glide.with(context)
             .load(url)
             .transform(new CircleTransform(context))
             .into(imageView);
    }

    public static void loadAttachment(final Context context, final String url,
                                      final ImageView imageView) {
        Glide.with(context)
             .load(url)
             .placeholder(R.drawable.image_placeholder)
             .into(imageView);
    }
}
