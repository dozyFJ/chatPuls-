package dev.dozyfj.chatpuls.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** chatPuls Admin: модерация сервера chatPuls — обзор, жалобы, пользователи. */
public final class AdminActivity extends Activity {
    private static final int OVERVIEW = 0;
    private static final int REPORTS = 1;
    private static final int USERS = 2;
    private static final String[] TABS = {"Обзор", "Жалобы", "Пользователи"};
    private static final String[][] STAT_LABELS = {
            {"users", "Пользователи"}, {"activeToday", "Активны за сутки"}, {"messagesToday", "Сообщений за сутки"},
            {"messages", "Всего сообщений"}, {"sessions", "Активных сеансов"}, {"banned", "Забанено"}, {"openReports", "Открытых жалоб"}};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedPreferences preferences;
    private boolean dark;
    private int tab = OVERVIEW;
    private boolean allReports;
    private String userQuery = "";
    private int generation;
    private LinearLayout content;
    private LinearLayout tabRow;
    private TextView subtitle;
    private Runnable pendingSearch;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        this.preferences = getSharedPreferences("admin", MODE_PRIVATE);
        this.dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (state != null) {
            this.tab = state.getInt("tab", OVERVIEW);
            this.allReports = state.getBoolean("allReports");
            this.userQuery = state.getString("userQuery", "");
        }
        AdminApi.Credentials shared = sharedCredentials(getIntent());
        if (shared != null) {
            showLogin(shared);
        } else if (api() == null) {
            showLogin(null);
        } else {
            showMain();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AdminApi.Credentials shared = sharedCredentials(intent);
        if (shared != null) {
            showLogin(shared);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("tab", this.tab);
        state.putBoolean("allReports", this.allReports);
        state.putString("userQuery", this.userQuery);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        this.handler.removeCallbacksAndMessages(null);
        this.worker.shutdownNow();
        super.onDestroy();
    }

    private static AdminApi.Credentials sharedCredentials(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            return null;
        }
        return AdminApi.parse(intent.getStringExtra(Intent.EXTRA_TEXT));
    }

    private AdminApi api() {
        String server = this.preferences.getString("server", "");
        String key = this.preferences.getString("key", "");
        String token = this.preferences.getString("token", "");
        return server.isEmpty() || (key.isEmpty() && token.isEmpty()) ? null : new AdminApi(server, key, token);
    }

    private String myRole() {
        return this.preferences.getString("role", "owner");
    }

    private String myName() {
        return this.preferences.getString("name", "");
    }

    private static String roleTitle(String role) {
        switch (role) {
            case "owner":
                return "владелец сервера";
            case "superadmin":
                return "главный админ";
            case "admin":
                return "админ";
            default:
                return "пользователь";
        }
    }

    private void signOut() {
        this.preferences.edit().remove("key").remove("token").remove("role").remove("name").apply();
    }

    // ---- Оформление ----

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int ink() {
        return this.dark ? 0xFFF1F2F4 : 0xFF1F2328;
    }

    private int muted() {
        return this.dark ? 0xFFA3A8B0 : 0xFF5F6670;
    }

    private int surface() {
        return this.dark ? 0xFF22252A : 0xFFFFFFFF;
    }

    private int outline() {
        return this.dark ? 0xFF33373D : 0xFFE1E4E8;
    }

    private int accent() {
        return this.dark ? 0xFFFB923C : 0xFFC2410C;
    }

    private static final int DANGER = 0xFFDC2626;

    private GradientDrawable box(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) {
            shape.setStroke(dp(1.0f), stroke);
        }
        return shape;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private Button button(String label, int color, boolean filled) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(44.0f));
        button.setMinimumHeight(dp(44.0f));
        button.setTextColor(filled ? 0xFFFFFFFF : color);
        button.setBackground(filled ? box(color, 0, 12) : box(0, color, 12));
        button.setStateListAnimator(null);
        return button;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout card() {
        LinearLayout card = column();
        card.setPadding(dp(16.0f), dp(14.0f), dp(16.0f), dp(14.0f));
        card.setBackground(box(surface(), outline(), 16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10.0f);
        card.setLayoutParams(params);
        return card;
    }

    private EditText field(String hint, String value, int type) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setTextColor(ink());
        field.setHintTextColor(muted());
        field.setInputType(type);
        field.setSingleLine(true);
        return field;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ---- Вход ----

    private void showLogin(AdminApi.Credentials prefill) {
        this.generation++;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = column();
        root.setPadding(dp(22.0f), dp(40.0f), dp(22.0f), dp(24.0f));
        scroll.addView(root);
        setContentView(scroll);
        root.addView(text("chatPuls Admin", 26, ink(), true));
        TextView hint = text("В chatPuls на телефоне-сервере откройте «Настройки → Сервер на этом телефоне → Ключ администратора» и нажмите «Поделиться», выбрав chatPuls Admin. "
                + "Для Node.js-сервера ключ печатается в консоли при первом запуске.", 14, muted(), false);
        hint.setPadding(0, dp(8.0f), 0, dp(18.0f));
        root.addView(hint);
        LinearLayout account = card();
        root.addView(account);
        account.addView(text("Войти аккаунтом", 16, ink(), true));
        TextView accountHint = text("Для админов и главного админа: тот же chatId и пароль, что в chatPuls.", 13, muted(), false);
        accountHint.setPadding(0, dp(4.0f), 0, dp(4.0f));
        account.addView(accountHint);
        EditText accountServer = field("Адрес сервера (https://…)", this.preferences.getString("server", ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        EditText chatId = field("chatId", this.preferences.getString("name", ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        EditText password = field("Пароль", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        account.addView(accountServer);
        account.addView(chatId);
        account.addView(password);
        Button accountLogin = button("Войти", accent(), true);
        LinearLayout.LayoutParams accountParams = new LinearLayout.LayoutParams(-1, -2);
        accountParams.topMargin = dp(10.0f);
        account.addView(accountLogin, accountParams);
        TextView accountStatus = text("", 13, muted(), false);
        accountStatus.setPadding(0, dp(10.0f), 0, 0);
        account.addView(accountStatus);
        accountLogin.setOnClickListener(v -> loginWithAccount(accountServer, chatId, password, accountLogin, accountStatus));

        LinearLayout form = card();
        root.addView(form);
        form.addView(text("Войти ключом сервера", 16, ink(), true));
        EditText server = field("Адрес сервера (https://…)", prefill != null ? prefill.server() : this.preferences.getString("server", ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        EditText key = field("Ключ администратора", prefill != null ? prefill.key() : "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(server);
        form.addView(key);
        Button paste = button("Вставить из буфера обмена", accent(), false);
        LinearLayout.LayoutParams pasteParams = new LinearLayout.LayoutParams(-1, -2);
        pasteParams.topMargin = dp(8.0f);
        form.addView(paste, pasteParams);
        paste.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
            CharSequence value = clip != null && clip.getItemCount() > 0 ? clip.getItemAt(0).coerceToText(this) : null;
            AdminApi.Credentials parsed = AdminApi.parse(value == null ? null : value.toString());
            if (parsed == null) {
                toast("В буфере нет адреса и ключа");
                return;
            }
            server.setText(parsed.server());
            key.setText(parsed.key());
        });
        Button login = button("Войти", accent(), true);
        LinearLayout.LayoutParams loginParams = new LinearLayout.LayoutParams(-1, -2);
        loginParams.topMargin = dp(10.0f);
        form.addView(login, loginParams);
        TextView status = text("", 13, muted(), false);
        status.setPadding(0, dp(10.0f), 0, 0);
        form.addView(status);
        login.setOnClickListener(v -> {
            final String base;
            try {
                base = AdminApi.normalizeUrl(server.getText().toString());
            } catch (Exception e) {
                server.setError("Нужен HTTPS-адрес сервера без пути");
                return;
            }
            final String secret = key.getText().toString().trim().toLowerCase();
            if (!secret.matches("[a-f0-9]{64}")) {
                key.setError("Ключ — 64 символа 0–9 и a–f");
                return;
            }
            login.setEnabled(false);
            status.setText("Проверяем…");
            this.worker.execute(() -> {
                String error = null;
                try {
                    new AdminApi(base, secret, null).get("/v1/admin/stats");
                } catch (AdminApi.Failure failure) {
                    error = failure.code == 401 ? "Неверный ключ" : failure.code == 429 ? "Слишком много попыток, подождите минуту"
                            : failure.code == 404 ? "Сервер не поддерживает модерацию — обновите chatPuls" : "Ошибка сервера " + failure.code;
                } catch (Exception e) {
                    error = "Не удалось подключиться: " + e.getMessage();
                }
                final String result = error;
                this.handler.post(() -> {
                    if (isDestroyed()) {
                        return;
                    }
                    login.setEnabled(true);
                    if (result != null) {
                        status.setText(result);
                        return;
                    }
                    this.preferences.edit().putString("server", base).putString("key", secret).remove("token").putString("role", "owner").remove("name").apply();
                    showMain();
                });
            });
        });
    }

    private void loginWithAccount(EditText serverField, EditText chatIdField, EditText passwordField, Button login, TextView status) {
        final String base;
        try {
            base = AdminApi.normalizeUrl(serverField.getText().toString());
        } catch (Exception e) {
            serverField.setError("Нужен HTTPS-адрес сервера без пути");
            return;
        }
        final String name = chatIdField.getText().toString().trim().toLowerCase();
        final String secret = passwordField.getText().toString();
        if (!name.matches("[a-z0-9_]{3,32}")) {
            chatIdField.setError("chatId: 3–32 латинские буквы, цифры или _");
            return;
        }
        login.setEnabled(false);
        status.setText("Проверяем…");
        this.worker.execute(() -> {
            String error = null;
            String token = null;
            String role = null;
            try {
                JSONObject session = new AdminApi(base, null, null).post("/v1/login", new JSONObject().put("username", name).put("password", secret));
                token = session.getString("token");
                role = new AdminApi(base, null, token).get("/v1/admin/me").getString("role");
            } catch (AdminApi.Failure failure) {
                error = failure.code == 401 ? (token == null ? "Неверный chatId или пароль" : "У этого аккаунта нет прав модератора")
                        : failure.code == 403 ? "Аккаунт заблокирован" : failure.code == 429 ? "Слишком много попыток, подождите минуту"
                        : failure.code == 404 ? "Сервер не поддерживает роли — обновите chatPuls" : "Ошибка сервера " + failure.code;
            } catch (Exception e) {
                error = "Не удалось подключиться: " + e.getMessage();
            }
            final String finalError = error;
            final String finalToken = token;
            final String finalRole = role;
            this.handler.post(() -> {
                if (isDestroyed()) {
                    return;
                }
                login.setEnabled(true);
                if (finalError != null) {
                    status.setText(finalError);
                    return;
                }
                this.preferences.edit().putString("server", base).remove("key").putString("token", finalToken)
                        .putString("role", finalRole).putString("name", name).apply();
                showMain();
            });
        });
    }

    // ---- Основной экран ----

    private void showMain() {
        LinearLayout root = column();
        setContentView(root);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18.0f), dp(14.0f), dp(10.0f), dp(6.0f));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout titles = column();
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1.0f));
        titles.addView(text("chatPuls Admin", 20, ink(), true));
        String who = myName().isEmpty() ? roleTitle(myRole()) : "@" + myName() + " · " + roleTitle(myRole());
        this.subtitle = text(who + " · " + this.preferences.getString("server", "").split("#", 2)[0].replaceFirst("^https?://", ""), 12, muted(), false);
        this.subtitle.setSingleLine(true);
        this.subtitle.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        titles.addView(this.subtitle);
        TextView refresh = headerAction("↻", "Обновить");
        refresh.setOnClickListener(v -> render());
        header.addView(refresh, new LinearLayout.LayoutParams(dp(44.0f), dp(44.0f)));
        TextView menu = headerAction("⋯", "Меню");
        menu.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(new String[]{"Сменить сервер", "Выйти"}, (dialog, which) -> {
            if (which == 1) {
                signOut();
            }
            showLogin(null);
        }).show());
        header.addView(menu, new LinearLayout.LayoutParams(dp(44.0f), dp(44.0f)));
        this.tabRow = new LinearLayout(this);
        this.tabRow.setPadding(dp(14.0f), dp(4.0f), dp(14.0f), dp(8.0f));
        root.addView(this.tabRow, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.content = column();
        this.content.setPadding(dp(14.0f), dp(4.0f), dp(14.0f), dp(24.0f));
        scroll.addView(this.content);
        render();
    }

    private TextView headerAction(String glyph, String description) {
        TextView action = text(glyph, 22, ink(), true);
        action.setGravity(Gravity.CENTER);
        action.setContentDescription(description);
        action.setClickable(true);
        action.setFocusable(true);
        return action;
    }

    private void render() {
        this.tabRow.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            boolean selected = i == this.tab;
            TextView choice = text(TABS[i], 14, selected ? 0xFFFFFFFF : ink(), true);
            choice.setGravity(Gravity.CENTER);
            choice.setPadding(dp(14.0f), 0, dp(14.0f), 0);
            choice.setBackground(selected ? box(accent(), 0, 20) : box(0, outline(), 20));
            choice.setClickable(true);
            choice.setOnClickListener(v -> {
                this.tab = index;
                render();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(38.0f));
            params.setMarginEnd(dp(6.0f));
            this.tabRow.addView(choice, params);
        }
        if (this.tab == REPORTS) {
            showReports();
        } else if (this.tab == USERS) {
            showUsers();
        } else {
            showOverview();
        }
    }

    /** Загружает данные в фоне; ответ старого экрана, если пользователь уже переключился, отбрасывается. */
    private void load(String path, java.util.function.Consumer<JSONObject> onResult) {
        final int request = ++this.generation;
        final AdminApi api = api();
        if (api == null) {
            showLogin(null);
            return;
        }
        this.worker.execute(() -> {
            JSONObject result = null;
            String error = null;
            try {
                result = api.get(path);
            } catch (AdminApi.Failure failure) {
                error = failure.code == 401 ? "auth" : "Ошибка сервера " + failure.code;
            } catch (Exception e) {
                error = "Нет связи с сервером";
            }
            final JSONObject finalResult = result;
            final String finalError = error;
            this.handler.post(() -> {
                if (isDestroyed() || request != this.generation) {
                    return;
                }
                if ("auth".equals(finalError)) {
                    toast("Доступ больше не действует, войдите снова");
                    signOut();
                    showLogin(null);
                } else if (finalError != null) {
                    message(finalError);
                } else {
                    onResult.accept(finalResult);
                }
            });
        });
    }

    private void act(String path, JSONObject body, String done) {
        final AdminApi api = api();
        if (api == null) {
            return;
        }
        this.worker.execute(() -> {
            String result;
            try {
                api.post(path, body);
                result = done;
            } catch (AdminApi.Failure failure) {
                result = failure.code == 404 ? "Уже обработано" : failure.code == 403 ? "Недостаточно прав" : "Ошибка сервера " + failure.code;
            } catch (Exception e) {
                result = "Нет связи с сервером";
            }
            final String finalResult = result;
            this.handler.post(() -> {
                if (!isDestroyed()) {
                    toast(finalResult);
                    render();
                }
            });
        });
    }

    private void message(String value) {
        this.content.removeAllViews();
        LinearLayout card = card();
        card.addView(text(value, 15, muted(), false));
        this.content.addView(card);
    }

    // ---- Обзор ----

    private void showOverview() {
        message("Загрузка…");
        load("/v1/admin/stats", stats -> {
            this.content.removeAllViews();
            LinearLayout row = null;
            for (int i = 0; i < STAT_LABELS.length; i++) {
                if (i % 2 == 0) {
                    row = new LinearLayout(this);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                    rowParams.bottomMargin = dp(10.0f);
                    this.content.addView(row, rowParams);
                }
                String key = STAT_LABELS[i][0];
                LinearLayout tile = column();
                tile.setPadding(dp(14.0f), dp(12.0f), dp(14.0f), dp(12.0f));
                boolean alert = key.equals("openReports") && stats.optLong(key) > 0;
                tile.setBackground(box(surface(), alert ? accent() : outline(), 16));
                tile.addView(text(Long.toString(stats.optLong(key)), 26, alert ? accent() : ink(), true));
                tile.addView(text(STAT_LABELS[i][1], 13, muted(), false));
                if (alert) {
                    tile.setClickable(true);
                    tile.setOnClickListener(v -> {
                        this.tab = REPORTS;
                        render();
                    });
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1.0f);
                if (i % 2 == 0) {
                    params.setMarginEnd(dp(10.0f));
                }
                row.addView(tile, params);
            }
            if (STAT_LABELS.length % 2 == 1 && row != null) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));
            }
        });
    }

    // ---- Жалобы ----

    private void showReports() {
        message("Загрузка…");
        load("/v1/admin/reports" + (this.allReports ? "?status=all" : ""), result -> {
            this.content.removeAllViews();
            LinearLayout filter = new LinearLayout(this);
            filter.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(this.allReports ? "Все жалобы" : "Открытые жалобы", 16, ink(), true);
            filter.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView toggle = text(this.allReports ? "Только открытые" : "Показать все", 14, accent(), true);
            toggle.setPadding(dp(8.0f), dp(8.0f), dp(4.0f), dp(8.0f));
            toggle.setOnClickListener(v -> {
                this.allReports = !this.allReports;
                render();
            });
            filter.addView(toggle);
            LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, -2);
            filterParams.bottomMargin = dp(6.0f);
            this.content.addView(filter, filterParams);
            JSONArray reports = result.optJSONArray("reports");
            if (reports == null || reports.length() == 0) {
                LinearLayout empty = card();
                empty.addView(text(this.allReports ? "Жалоб пока не было." : "Открытых жалоб нет 🎉", 15, muted(), false));
                this.content.addView(empty);
                return;
            }
            for (int i = 0; i < reports.length(); i++) {
                JSONObject report = reports.optJSONObject(i);
                if (report != null) {
                    this.content.addView(reportCard(report));
                }
            }
        });
    }

    private View reportCard(JSONObject report) {
        LinearLayout card = card();
        final long id = report.optLong("id");
        final String target = report.optString("target");
        boolean open = "open".equals(report.optString("status"));
        boolean hasMessage = !report.isNull("messageId") && !report.optString("messageId").isEmpty();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text("@" + target, 17, ink(), true), new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView reason = text(report.optString("reason"), 12, accent(), true);
        reason.setPadding(dp(10.0f), dp(4.0f), dp(10.0f), dp(4.0f));
        reason.setBackground(box(0, accent(), 12));
        top.addView(reason);
        card.addView(top);
        String when = DateUtils.getRelativeTimeSpanString(report.optLong("time"), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
        TextView meta = text("Пожаловался @" + report.optString("reporter") + " · " + when
                + (open ? "" : " · " + ("dismissed".equals(report.optString("status")) ? "отклонена" : "решена")), 12, muted(), false);
        meta.setPadding(0, dp(4.0f), 0, 0);
        card.addView(meta);
        if (hasMessage) {
            String body = report.optString("text");
            TextView quote = text(body.isEmpty() ? "Голосовое или пустое сообщение" : body, 15, ink(), false);
            quote.setPadding(dp(12.0f), dp(10.0f), dp(12.0f), dp(10.0f));
            quote.setBackground(box(this.dark ? 0xFF2B2F35 : 0xFFF3F4F6, 0, 10));
            quote.setTextIsSelectable(true);
            LinearLayout.LayoutParams quoteParams = new LinearLayout.LayoutParams(-1, -2);
            quoteParams.topMargin = dp(10.0f);
            card.addView(quote, quoteParams);
        } else {
            TextView whole = text("Жалоба на пользователя целиком", 13, muted(), false);
            whole.setPadding(0, dp(8.0f), 0, 0);
            card.addView(whole);
        }
        if (!open) {
            return card;
        }
        LinearLayout actions = new LinearLayout(this);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.topMargin = dp(12.0f);
        card.addView(actions, actionsParams);
        Button dismiss = button("Отклонить", muted(), false);
        dismiss.setOnClickListener(v -> resolve(id, "dismiss", "Жалоба отклонена"));
        actions.addView(dismiss, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button punish = button(hasMessage ? "Удалить и забанить" : "Забанить", DANGER, true);
        LinearLayout.LayoutParams punishParams = new LinearLayout.LayoutParams(0, -2, 1.3f);
        punishParams.setMarginStart(dp(8.0f));
        actions.addView(punish, punishParams);
        punish.setOnClickListener(v -> {
            String[] options = hasMessage
                    ? new String[]{"Удалить сообщение", "Забанить @" + target, "Удалить сообщение и забанить"}
                    : new String[]{"Забанить @" + target};
            String[] codes = hasMessage ? new String[]{"delete", "ban", "delete_ban"} : new String[]{"ban"};
            new AlertDialog.Builder(this).setTitle("Меры по жалобе").setItems(options, (dialog, which) -> {
                String code = codes[which];
                String done = code.equals("delete") ? "Сообщение удалено" : code.equals("ban") ? "Пользователь забанен" : "Удалено, пользователь забанен";
                confirm(options[which] + "?", code.contains("ban") ? "Все сеансы @" + target + " будут завершены, войти он не сможет, пока вы его не разбаните." : "Сообщение исчезнет у обоих собеседников при следующей синхронизации.",
                        () -> resolve(id, code, done));
            }).setNegativeButton("Отмена", null).show();
        });
        return card;
    }

    private void resolve(long id, String action, String done) {
        try {
            act("/v1/admin/reports", new JSONObject().put("id", id).put("action", action), done);
        } catch (Exception ignored) {
        }
    }

    private void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Да", (dialog, which) -> action.run()).show();
    }

    // ---- Пользователи ----

    private void showUsers() {
        this.content.removeAllViews();
        EditText search = field("Поиск по chatId", this.userQuery, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setBackground(box(surface(), outline(), 14));
        search.setPadding(dp(14.0f), dp(10.0f), dp(14.0f), dp(10.0f));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        searchParams.bottomMargin = dp(10.0f);
        this.content.addView(search, searchParams);
        final LinearLayout list = column();
        this.content.addView(list, new LinearLayout.LayoutParams(-1, -2));
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                AdminActivity.this.userQuery = editable.toString().trim();
                if (AdminActivity.this.pendingSearch != null) {
                    AdminActivity.this.handler.removeCallbacks(AdminActivity.this.pendingSearch);
                }
                AdminActivity.this.pendingSearch = () -> loadUsers(list);
                AdminActivity.this.handler.postDelayed(AdminActivity.this.pendingSearch, 350L);
            }
        });
        loadUsers(list);
    }

    private void loadUsers(LinearLayout list) {
        String path;
        try {
            path = "/v1/admin/users?q=" + AdminApi.encode(this.userQuery);
        } catch (Exception e) {
            return;
        }
        final int request = ++this.generation;
        final AdminApi api = api();
        if (api == null) {
            return;
        }
        this.worker.execute(() -> {
            JSONObject result = null;
            try {
                result = api.get(path);
            } catch (Exception ignored) {
            }
            final JSONObject finalResult = result;
            this.handler.post(() -> {
                if (isDestroyed() || request != this.generation || this.tab != USERS) {
                    return;
                }
                list.removeAllViews();
                JSONArray users = finalResult == null ? null : finalResult.optJSONArray("users");
                if (users == null) {
                    list.addView(text("Не удалось загрузить пользователей", 14, muted(), false));
                    return;
                }
                if (users.length() == 0) {
                    list.addView(text("Никого не нашли", 14, muted(), false));
                    return;
                }
                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.optJSONObject(i);
                    if (user != null) {
                        list.addView(userRow(user));
                    }
                }
            });
        });
    }

    private void userActions(String name, String role, boolean banned) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<Runnable> actions = new java.util.ArrayList<>();
        boolean owner = myRole().equals("owner");
        boolean superadmin = myRole().equals("superadmin");
        if (!name.equals(myName())) {
            labels.add(banned ? "Разбанить" : "Забанить");
            actions.add(() -> {
                Runnable run = () -> userAction(name, banned ? "unban" : "ban", banned ? "Пользователь разбанен" : "Пользователь забанен");
                if (banned) {
                    run.run();
                } else {
                    confirm("Забанить @" + name + "?", "Пользователь не сможет войти и писать, пока вы его не разбаните.", run);
                }
            });
            labels.add("Завершить все сеансы");
            actions.add(() -> userAction(name, "logout", "Сеансы завершены"));
        }
        if ((owner || superadmin) && !name.equals(myName())) {
            if (role.equals("user")) {
                labels.add("Сделать админом 🛡");
                actions.add(() -> setRole(name, "admin", "@" + name + " теперь админ"));
            }
            if (role.equals("admin")) {
                labels.add("Снять админа");
                actions.add(() -> setRole(name, "user", "@" + name + " больше не админ"));
            }
        }
        if (owner) {
            if (!role.equals("superadmin")) {
                labels.add("Сделать главным админом 👑");
                actions.add(() -> confirm("Сделать @" + name + " главным админом?", "Главный админ сможет назначать и снимать админов и банить кого угодно, кроме других главных.",
                        () -> setRole(name, "superadmin", "@" + name + " теперь главный админ")));
            } else {
                labels.add("Снять главного админа");
                actions.add(() -> setRole(name, "user", "@" + name + " больше не главный админ"));
            }
        }
        if (labels.isEmpty()) {
            toast("Это вы");
            return;
        }
        new AlertDialog.Builder(this).setTitle("@" + name + " · " + roleTitle(role))
                .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .setNegativeButton("Отмена", null).show();
    }

    private void userAction(String name, String action, String done) {
        try {
            act("/v1/admin/users", new JSONObject().put("name", name).put("action", action), done);
        } catch (Exception ignored) {
        }
    }

    private void setRole(String name, String role, String done) {
        try {
            act("/v1/admin/roles", new JSONObject().put("name", name).put("role", role), done);
        } catch (Exception ignored) {
        }
    }

    private View userRow(JSONObject user) {
        final String name = user.optString("name");
        final boolean banned = user.optBoolean("banned");
        final String role = user.optString("role", "user");
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14.0f), dp(12.0f), dp(14.0f), dp(12.0f));
        row.setBackground(box(surface(), outline(), 14));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = dp(8.0f);
        row.setLayoutParams(rowParams);
        TextView avatar = text(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), 16, banned ? DANGER : accent(), true);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable circle = box(0, banned ? DANGER : accent(), 20);
        avatar.setBackground(circle);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(40.0f), dp(40.0f));
        avatarParams.setMarginEnd(dp(12.0f));
        row.addView(avatar, avatarParams);
        LinearLayout info = column();
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0f));
        info.addView(text("@" + name + (role.equals("superadmin") ? "  👑" : role.equals("admin") ? "  🛡" : ""), 16, ink(), true));
        long last = user.optLong("lastActive");
        String activity = user.optLong("messages") + " сообщ. · " + (last == 0 ? "ещё не писал(а)"
                : "писал(а) " + DateUtils.getRelativeTimeSpanString(last, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        info.addView(text(activity, 12, muted(), false));
        if (banned) {
            TextView badge = text("Забанен", 12, DANGER, true);
            badge.setPadding(dp(8.0f), dp(3.0f), dp(8.0f), dp(3.0f));
            badge.setBackground(box(0, DANGER, 10));
            row.addView(badge);
        }
        row.setClickable(true);
        row.setOnClickListener(v -> userActions(name, role, banned));
        return row;
    }
}
