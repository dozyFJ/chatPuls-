package dev.dozyfj.pulsemessenger;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

final class CallScreen {
    private static final int BACKGROUND = 0xFF191A1C;
    private final Activity activity;
    private final LinearLayout controls;
    private final Dialog dialog;
    private final LinearLayout microphoneRow;
    private final LinearLayout screen;
    final TextView status;

    CallScreen(Activity activity, String str, String str2, boolean z, Bitmap bitmap) {
        View label;
        this.activity = activity;
        this.dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
        this.screen = new LinearLayout(activity);
        this.screen.setOrientation(LinearLayout.VERTICAL);
        this.screen.setGravity(Gravity.CENTER_HORIZONTAL);
        this.screen.setBackgroundColor(BACKGROUND);
        this.screen.setPadding(dp(22), dp(16), dp(22), dp(24));
        this.screen.setFitsSystemWindows(true);
        this.screen.addView(label("Pulse", 17, 0xFFC9CBCE, true), new LinearLayout.LayoutParams(-1, -2));
        this.screen.addView(new View(activity), new LinearLayout.LayoutParams(1, 0, 0.65f));
        int min = Math.min(dp(430), activity.getResources().getDisplayMetrics().widthPixels - dp(44));
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable shape = shape(0xFF292B2E, 22);
        shape.setStroke(dp(1), 0xFF4B4E52);
        linearLayout.setBackground(shape);
        this.screen.addView(linearLayout, new LinearLayout.LayoutParams(min, -2));
        String str3 = str.trim().isEmpty() ? "?" : str;
        LinearLayout linearLayout2 = new LinearLayout(activity);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.55f));
        GradientDrawable shape2 = shape(0xFF383A3E, 28);
        shape2.setShape(1);
        shape2.setStroke(dp(1), 0xFF717479);
        if (bitmap == null) {
            label = label(str3.substring(0, str3.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT), 25, 0xFFE9EBED, true);
        } else {
            ImageView imageView = new ImageView(activity);
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setClipToOutline(true);
            label = imageView;
        }
        label.setBackground(shape2);
        label.setImportantForAccessibility(2);
        linearLayout2.addView(label, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView label2 = label(str3, 22, -1, true);
        label2.setGravity(Gravity.START);
        label2.setSingleLine(true);
        label2.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(8);
        linearLayout2.addView(label2, layoutParams);
        TextView label3 = label(str3.equals("?") ? "—" : "@" + str3, 13, 0xFFB9BCC0, false);
        label3.setGravity(Gravity.START);
        label3.setSingleLine(true);
        label3.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(label3, new LinearLayout.LayoutParams(-1, -2));
        this.status = label(str2, 14, 0xFFE4E6E8, true);
        this.status.setGravity(Gravity.START);
        this.status.setAccessibilityLiveRegion(1);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(9);
        linearLayout2.addView(this.status, layoutParams2);
        View view = new View(activity);
        view.setBackgroundColor(0xFF4B4E52);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(dp(1), dp(96));
        layoutParams3.setMargins(dp(8), 0, dp(8), 0);
        linearLayout.addView(view, layoutParams3);
        LinearLayout linearLayout3 = new LinearLayout(activity);
        linearLayout3.setOrientation(LinearLayout.VERTICAL);
        linearLayout3.setGravity(Gravity.CENTER);
        linearLayout.addView(linearLayout3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout3.addView(label("Аудиозвонок", 13, 0xFFF2F3F4, true), new LinearLayout.LayoutParams(-1, -2));
        linearLayout3.addView(new PulseWave(activity), new LinearLayout.LayoutParams(-1, dp(42)));
        linearLayout3.addView(label(z ? "Входящий" : "Исходящий", 12, 0xFFB9BCC0, false), new LinearLayout.LayoutParams(-1, -2));
        this.microphoneRow = new LinearLayout(activity);
        this.microphoneRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(min, -2);
        layoutParams4.topMargin = dp(10);
        this.screen.addView(this.microphoneRow, layoutParams4);
        this.screen.addView(new View(activity), new LinearLayout.LayoutParams(1, 0, 1.3f));
        this.controls = new LinearLayout(activity);
        this.controls.setGravity(Gravity.CENTER);
        this.screen.addView(this.controls, new LinearLayout.LayoutParams(min, -2));
        this.dialog.setContentView(this.screen);
        this.dialog.setCanceledOnTouchOutside(false);
    }

    public int dp(int value) {
        return Math.round(value * this.activity.getResources().getDisplayMetrics().density);
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this.activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT, 1);
        }
        return text;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    Button action(String name, int color, Runnable run) {
        Button action = control(name, color, run);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1.0f);
        params.setMargins(dp(5), 0, dp(5), 0);
        this.controls.addView(action, params);
        return action;
    }

    Button microphone(Runnable run) {
        Button action = control("Микрофон вкл.", 0xFF34373A, run);
        action.setPadding(dp(16), 0, dp(16), 0);
        this.microphoneRow.addView(action, new LinearLayout.LayoutParams(-2, dp(48)));
        return action;
    }

    private Button control(String name, int color, final Runnable run) {
        Button action = new Button(this.activity);
        action.setText(name);
        action.setTextColor(-1);
        action.setTextSize(14.0f);
        action.setAllCaps(false);
        action.setMinWidth(0);
        action.setMinimumWidth(0);
        action.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape(color, 24), null));
        action.setOnClickListener(view -> run.run());
        return action;
    }

    private final class PulseWave extends View {
        private final Paint paint;

        PulseWave(Activity context) {
            super(context);
            this.paint = new Paint(1);
            setImportantForAccessibility(2);
            this.paint.setColor(0xFFD4D7DB);
            this.paint.setStrokeWidth(CallScreen.this.dp(3));
            this.paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            boolean animated = ValueAnimator.areAnimatorsEnabled();
            double phase = animated ? SystemClock.uptimeMillis() / 230.0d : 1.0d;
            float center = getHeight() / 2.0f;
            float interval = Math.min(CallScreen.this.dp(8), getWidth() / 8.0f);
            for (int i = -3; i <= 3; i++) {
                float x = (getWidth() / 2.0f) + (i * interval);
                float halfHeight = CallScreen.this.dp(4) + ((float) (((Math.sin((i * 0.9d) + phase) * 0.5d) + 0.5d) * CallScreen.this.dp(10)));
                canvas.drawLine(x, center - halfHeight, x, center + halfHeight, this.paint);
            }
            if (animated && isShown()) {
                postInvalidateDelayed(55L);
            }
        }
    }

    void attachMedia(View web) {
        this.screen.addView(web, new LinearLayout.LayoutParams(1, 1));
    }

    void show(final Runnable onCancel) {
        this.dialog.setOnCancelListener(dialogInterface -> onCancel.run());
        this.dialog.show();
        Window window = this.dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(BACKGROUND));
            window.addFlags(0x80000080);
            window.setStatusBarColor(BACKGROUND);
            window.setNavigationBarColor(BACKGROUND);
            if (Build.VERSION.SDK_INT >= 30 && window.getInsetsController() != null) {
                window.setDecorFitsSystemWindows(true);
                window.getInsetsController().setSystemBarsAppearance(0, 24);
                window.getInsetsController().show(WindowInsets.Type.systemBars());
            } else {
                window.getDecorView().setSystemUiVisibility(0);
            }
            window.setLayout(-1, -1);
        }
    }

    void dismiss() {
        this.dialog.dismiss();
    }
}
