package dev.dozyfj.pulsemessenger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.util.Random;

/**
 * Фон в духе Telegram: серый фон и повторяющийся узор из тонких иконок — чаты, трубки, контакты, колокольчики.
 * Плитка рисуется один раз и дальше просто повторяется, поэтому прокрутка не тормозит.
 */
final class DoodleBackground extends Drawable {
    private static final int[] ICONS = {
            R.drawable.ic_pulse_mark, R.drawable.ic_audio_call, R.drawable.ic_feed_profile, R.drawable.ic_notification_bell,
            R.drawable.ic_feed_channels, R.drawable.ic_video_call, R.drawable.ic_community_rail, R.drawable.ic_profile_edit};

    private final Context context;
    private final int base;
    private final int ink;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap tile;

    private final boolean dark;
    private final Paint blobs = new Paint(Paint.ANTI_ALIAS_FLAG);

    DoodleBackground(Context context, int base, int ink) {
        this.context = context.getApplicationContext();
        this.base = base;
        this.ink = ink;
        this.dark = android.graphics.Color.luminance(base) < 0.3f;
    }

    /** Мягкие цветные пятна под узором — на них «стекло» карточек выглядит объёмно. */
    private void drawBlobs(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        float w = b.width();
        float h = b.height();
        int[] colors = this.dark
                ? new int[]{0x66603A9E, 0x55206E78, 0x4D9E3A6A}
                : new int[]{0x8CB9A7FF, 0x7A9FD8FF, 0x73FFC6A8};
        float[][] spots = {{0.15f, 0.18f, 0.75f}, {0.9f, 0.45f, 0.7f}, {0.3f, 0.88f, 0.8f}};
        for (int i = 0; i < spots.length; i++) {
            float cx = b.left + w * spots[i][0];
            float cy = b.top + h * spots[i][1];
            float radius = Math.max(w, h) * spots[i][2] * 0.6f;
            this.blobs.setShader(new android.graphics.RadialGradient(cx, cy, radius, colors[i], colors[i] & 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, this.blobs);
        }
    }

    private int dp(float value) {
        return Math.round(value * this.context.getResources().getDisplayMetrics().density);
    }

    private Bitmap buildTile() {
        int size = dp(240.0f);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Random random = new Random(7);
        int cells = 5;
        float cell = size / (float) cells;
        int index = 0;
        for (int row = 0; row < cells; row++) {
            for (int column = 0; column < cells; column++) {
                Drawable icon = this.context.getDrawable(ICONS[(index++ * 3 + row) % ICONS.length]);
                if (icon == null) {
                    continue;
                }
                icon = icon.mutate();
                icon.setTint(this.ink);
                int iconSize = dp(20.0f + random.nextInt(8));
                // Сдвиг через ряд и небольшой разброс — чтобы узор не выглядел сеткой.
                float cx = column * cell + cell / 2 + (row % 2 == 0 ? 0 : cell / 2) + (random.nextFloat() - 0.5f) * cell * 0.3f;
                float cy = row * cell + cell / 2 + (random.nextFloat() - 0.5f) * cell * 0.3f;
                float angle = (random.nextFloat() - 0.5f) * 50.0f;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        float x = cx + dx * size;
                        float y = cy + dy * size;
                        if (x < -iconSize || x > size + iconSize || y < -iconSize || y > size + iconSize) {
                            continue;
                        }
                        canvas.save();
                        canvas.rotate(angle, x, y);
                        icon.setBounds(Math.round(x - iconSize / 2f), Math.round(y - iconSize / 2f), Math.round(x + iconSize / 2f), Math.round(y + iconSize / 2f));
                        icon.draw(canvas);
                        canvas.restore();
                    }
                }
            }
        }
        return bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(this.base);
        drawBlobs(canvas);
        if (this.tile == null) {
            this.tile = buildTile();
            this.paint.setShader(new BitmapShader(this.tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        }
        canvas.drawRect(getBounds(), this.paint);
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
