package shalaev.vk_test_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;

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
             .using(new CollageModelLoader(), InputStream.class)
             .load(collage)
             .as(Bitmap.class)
             .encoder(new BitmapEncoder())
             .decoder(new StreamBitmapDecoder(context))
             .sourceEncoder(new StreamEncoder())
             .cacheDecoder(new FileToStreamDecoder<>(new StreamBitmapDecoder(context)))
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
        @Override
        public DataFetcher<InputStream> getResourceFetcher(final Collage collage,
                                                           final int width,
                                                           final int height) {
            return new CollageFetcher(collage, width, height);
        }
    }

    private static class CollageFetcher implements DataFetcher<InputStream> {
        private final Collage collage;
        private final int width;
        private final int height;
        private final Bitmap.Config config = Bitmap.Config.RGB_565;

        public CollageFetcher(final Collage collage, final int width,
                              final int height) {
            this.collage = collage;
            this.width = width;
            this.height = height;
        }

        @Override
        public InputStream loadData(final Priority priority)
                throws Exception {
            ArrayList<Bitmap> bitmaps = new ArrayList<>();
            List<String> urls = collage.getUrls();
            for (String url : urls) {
                HttpUrlFetcher fetcher = new HttpUrlFetcher(new GlideUrl(url));
                InputStream inputStream = fetcher.loadData(priority);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                bitmaps.add(bitmap);
                fetcher.cleanup();
            }

            Bitmap resultBitmap = Bitmap.createBitmap(width, height, config);
            Canvas canvas = new Canvas(resultBitmap);
            Bitmap b;
            switch (bitmaps.size()) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    canvas.drawBitmap(bitmaps.get(0), -width / 4, 0, null);
                    canvas.drawBitmap(b = scale(bitmaps.get(1)), width / 2, 0, null);
                    b.recycle();
                    canvas.drawBitmap(b = scale(bitmaps.get(2)), width / 2, height / 2, null);
                    b.recycle();
                    break;
                default:
                    for (int i = 0; i < 4; i++) {
                        int left = (i % 2) * (width / 2), top = (i / 2) * (height / 2);
                        canvas.drawBitmap(b = scale(bitmaps.get(i)), left, top, null);
                        b.recycle();
                    }
                    break;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            for (Bitmap bitmap : bitmaps) {
                bitmap.recycle();
            }

            return new ByteArrayInputStream(bos.toByteArray());
        }

        @Override
        public void cleanup() {

        }

        @Override
        public String getId() {
            return String.format("%s_%dx%d",
                                 TextUtils.join("_", collage.getUrls()), width, height);
        }

        @Override
        public void cancel() {

        }

        private Bitmap scale(final Bitmap bitmap) {
            return Bitmap.createScaledBitmap(bitmap,
                                             width / 2, height / 2,
                                             false);
        }
    }

    public static class CircleTransform extends BitmapTransformation {
        public CircleTransform(final Context context) {
            super(context);
        }

        @Override
        public String getId() {
            return "circle";
        }

        @Override
        protected Bitmap transform(final BitmapPool pool, final Bitmap source, final int outWidth,
                                   final int outHeight) {
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP,
                                                   BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            if (squaredBitmap != source) {
                squaredBitmap.recycle();
            }

            return bitmap;
        }
    }
}
