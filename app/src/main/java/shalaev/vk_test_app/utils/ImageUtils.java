package shalaev.vk_test_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import shalaev.vk_test_app.R;

public final class ImageUtils {
    public static void loadSimpleAvatar(final Context context, final String url,
                                        final ImageView imageView) {
        Glide.with(context)
             .load(url)
             .diskCacheStrategy(DiskCacheStrategy.ALL)
             .placeholder(R.drawable.avatar_dummy)
             .transform(new CircleTransform(context))
             .into(imageView);
    }

    public static void loadCollageAvatar(final Context context, final Collage collage,
                                         final ImageView imageView) {
        Glide.with(context)
             .using(new CollageModelLoader(context), InputStream.class)
             .load(collage)
             .as(Bitmap.class)
             .placeholder(R.drawable.avatar_dummy)
             .transform(new CircleTransform(context))
             .into(imageView);
    }

    public static void loadAttachment(final Context context, final String url,
                                      final ImageView imageView) {
        Glide.with(context)
             .load(url)
             .diskCacheStrategy(DiskCacheStrategy.ALL)
             .placeholder(R.drawable.image_placeholder)
             .into(imageView);
    }

    public static class Collage {
        private static final int MAX_PHOTOS = 4;
        private final ArrayList<String> urls = new ArrayList<>();

        public void addUserPhoto(final String photoUrl) {
            urls.add(photoUrl);
        }

        public List<String> getUrls() {
            return urls.subList(0, Math.min(urls.size(), MAX_PHOTOS));
        }
    }

    private static class CollageModelLoader implements ModelLoader<Collage, InputStream> {
        private final Context context;

        public CollageModelLoader(final Context context) {
            this.context = context;
        }

        @Override
        public DataFetcher<InputStream> getResourceFetcher(final Collage collage,
                                                           final int width,
                                                           final int height) {
            return new CollageFetcher(context, collage, width, height);
        }
    }

    private static class CollageFetcher implements DataFetcher<InputStream> {
        private final Context context;
        private final Collage collage;
        private final int width;
        private final int height;
        private final ArrayList<Bitmap> toRecycle = new ArrayList<>();

        public CollageFetcher(final Context context, final Collage collage, final int width,
                              final int height) {
            this.context = context;
            this.collage = collage;
            this.width = width;
            this.height = height;
        }

        @Override
        public InputStream loadData(final Priority priority)
                throws Exception {
            Log.d("COLLAGE", "loadData: " + TextUtils.join("__", collage.getUrls()));
            ArrayList<Bitmap> bitmaps = new ArrayList<>();
            List<String> urls = collage.getUrls();
            for (String url : urls) {
                Bitmap bitmap = Glide.with(context)
                                     .load(url)
                                     .asBitmap()
                                     .diskCacheStrategy(DiskCacheStrategy.ALL)
                                     .into(width, height)
                                     .get();
                bitmaps.add(bitmap);
                toRecycle.add(bitmap);
            }

            Bitmap.Config config = Bitmap.Config.RGB_565;
            Bitmap bitmap = Bitmap.createBitmap(width, height, config);
            Canvas canvas = new Canvas(bitmap);
            switch (bitmaps.size()) {
                case 1:
                    Log.d("COLLAGE", "switch 1");
                    break;
                case 2:
                    Log.d("COLLAGE", "switch 2");
                    break;

                case 3:
                    Log.d("COLLAGE", "switch 3");
                    Bitmap first = bitmaps.get(0);
                    canvas.drawBitmap(first, -width / 4, 0, null);
                    Bitmap second = Bitmap.createScaledBitmap(bitmaps.get(1),
                                                              width / 2, height / 2,
                                                              false);
                    canvas.drawBitmap(second, width / 2, 0, null);
                    Bitmap third = Bitmap.createScaledBitmap(bitmaps.get(2),
                                                             width / 2, height / 2,
                                                             false);
                    canvas.drawBitmap(third, width / 2, height / 2, null);
                    toRecycle.add(second);
                    toRecycle.add(third);
                    break;
                default:
                    Log.d("COLLAGE", "switch 4+");
                    for (int i = 0; i < 4; i++) {
                        Bitmap b = Bitmap.createScaledBitmap(bitmaps.get(i),
                                                             width / 2, height / 2,
                                                             false);
                        int left = (i % 2) * (width / 2), top = (i / 2) * (height / 2);
                        canvas.drawBitmap(b, left, top, null);
                        toRecycle.add(b);
                    }
                    break;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            toRecycle.add(bitmap);

            return new ByteArrayInputStream(bos.toByteArray());
        }

        @Override
        public void cleanup() {
            Log.d("COLLAGE", String.format("cleanup %d", toRecycle.size()));
            for (Bitmap b : toRecycle) {
                b.recycle();
            }
        }

        @Override
        public String getId() {
            String id = String.format("%s_%dx%d", TextUtils.join("_", collage.getUrls()), width,
                                      height);
            Log.d("COLLAGE", "getId: " + id);
            return id;
        }

        @Override
        public void cancel() {

        }
    }
}
