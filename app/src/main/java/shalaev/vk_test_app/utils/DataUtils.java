package shalaev.vk_test_app.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import org.json.JSONObject;

import shalaev.vk_test_app.R;

public class DataUtils {
    public static String getPhotoUrl(final Context context, final JSONObject json,
                                     final String type) {
        String photoKey = getPhotoKey(context, type);
        String[] sizes = getPhotoSizes(context, type);
        if (null != photoKey && null != sizes) {
            int index = -1;
            for (int i = 0; i < sizes.length; i++) {
                if (photoKey.equals(sizes[i])) {
                    index = i;
                }
                if (index >= 0) {
                    String url = json.optString(sizes[i]);
                    if (!TextUtils.isEmpty(url)) {
                        return url;
                    }
                }
            }
        }
        return null;
    }

    public static String[] getPhotoSizes(final Context context, final String type) {
        Resources resources = context.getResources();
        switch (type) {
            case "photo":
                return resources.getStringArray(R.array._photo_sizes);
            case "sticker":
                return resources.getStringArray(R.array._sticker_sizes);
            default:
                return null;
        }
    }

    public static String getPhotoKey(final Context context, final String type) {
        switch (type) {
            case "photo":
                return context.getString(R.string._photo_size);
            case "sticker":
                return context.getString(R.string._sticker_size);
            default:
                return null;
        }
    }
}
