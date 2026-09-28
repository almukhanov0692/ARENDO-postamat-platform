package com.example.postamatmodbus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Full-screen, independently scrollable event journal. */
public final class LogActivity extends Activity {
    private static final int COLOR_BACKGROUND = Color.rgb(7, 12, 18);
    private static final int COLOR_NAVY = Color.rgb(10, 16, 24);
    private static final int COLOR_CARD = Color.rgb(16, 23, 33);
    private static final int COLOR_TEXT = Color.rgb(232, 237, 244);
    private static final int COLOR_MUTED = Color.rgb(120, 134, 151);
    private static final int COLOR_BLUE = Color.rgb(57, 136, 246);
    private static final int COLOR_RED = Color.rgb(248, 93, 110);
    private static final int SAVE_LOG_REQUEST = 401;
    private static volatile LogActivity active;
    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        active = this;
        getWindow().setStatusBarColor(COLOR_NAVY);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(buildUi());
        refreshLog(true);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(18));
        root.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), dp(10), dp(16), dp(10));
        header.setBackground(rounded(COLOR_CARD, 14));

        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(30);
        back.setTextColor(Color.WHITE);
        back.setAllCaps(false);
        back.setMinWidth(dp(44));
        back.setMinHeight(dp(44));
        back.setPadding(0, 0, 0, dp(4));
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));

        TextView title = text("Журнал событий", 19, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView badge = text("LIVE", 11, Color.rgb(147, 197, 253));
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(badge);
        root.addView(header, blockParams(12));

        TextView hint = text("Обратная связь Backend · Modbus · RS-485", 12,
                COLOR_MUTED);
        root.addView(hint, blockParams(10));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button save = actionButton("Сохранить", COLOR_CARD, COLOR_BLUE);
        save.setOnClickListener(v -> saveLog());
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button clear = actionButton("Очистить", COLOR_CARD, COLOR_RED);
        clear.setOnClickListener(v -> {
            MainActivity.clearLog();
            refreshLog(true);
            Toast.makeText(this, "Журнал очищен", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        clearParams.leftMargin = dp(8);
        actions.addView(clear, clearParams);
        root.addView(actions, blockParams(10));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        logView = text("", 12, COLOR_TEXT);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextIsSelectable(true);
        logView.setPadding(dp(16), dp(16), dp(16), dp(16));
        logView.setBackground(rounded(COLOR_CARD, 12));
        scroll.addView(logView);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView footer = text("Журнал действует до закрытия приложения.", 11,
                COLOR_MUTED);
        root.addView(footer, blockParams(0));
        return root;
    }

    private void refreshLog(boolean scrollToBottom) {
        if (logView != null) {
            logView.setText(MainActivity.getLogText());
            if (scrollToBottom) {
                logView.post(() -> {
                    if (logView.getParent() instanceof ScrollView) {
                        ((ScrollView) logView.getParent()).fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }
    }

    static void notifyLogUpdated() {
        LogActivity page = active;
        if (page != null) {
            page.runOnUiThread(() -> page.refreshLog(false));
        }
    }

    private void saveLog() {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                .format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "postamat-log-" + stamp + ".txt");
        startActivityForResult(intent, SAVE_LOG_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_LOG_REQUEST || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
            if (output == null) {
                throw new IllegalStateException("Не удалось открыть файл");
            }
            output.write(MainActivity.getLogText().getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Журнал сохранён", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "Ошибка сохранения: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (active == this) {
            active = null;
        }
        super.onDestroy();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button actionButton(String label, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(backgroundColor, 12));
        return button;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams blockParams(int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottomDp);
        return params;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
