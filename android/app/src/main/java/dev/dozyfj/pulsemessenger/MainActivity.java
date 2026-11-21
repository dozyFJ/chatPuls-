package dev.dozyfj.pulsemessenger;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import dev.dozyfj.pulsemessenger.CallHistory;
import dev.dozyfj.pulsemessenger.ChatStore;
import dev.dozyfj.pulsemessenger.MainActivity;
import dev.dozyfj.pulsemessenger.PhoneHost;
import dev.dozyfj.pulsemessenger.PulseApi;
import dev.dozyfj.pulsemessenger.PulseCalls;
import dev.dozyfj.pulsemessenger.UpdateManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import javax.net.ssl.SSLException;
import org.json.JSONException;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int ACCOUNT_SETTINGS = 1;
    private static final int APP_SETTINGS = 0;
    private static final int CALLS = 7;
    private static final int CALLS_TAB = 6;
    private static final int CHANNELS_TAB = 2;
    private static final int CHATS = 0;
    private static final int CLIPS = 5;
    private static final int COMMUNITIES = 4;
    private static final int COMMUNITIES_TAB = 3;
    private static final int CONTACTS = 1;
    private static final int CONTACTS_TAB = 4;
    private static final int FEED = 6;
    private static final int FEED_CHAT_OFFSET = 1;
    private static final int FEED_TAB = 5;
    private static final int INSTALL_PERMISSION = 41;
    private static final int LEGACY_FEED_OR_NEWS = 3;
    private static final int MESSAGES_TAB = 0;
    private static final int PICK_CLIP_VIDEO = 44;
    private static final int PICK_PHONE_CONTACT = 43;
    private static final int PICK_PROFILE_PHOTO = 42;
    private static final int RECORD_VOICE_PERMISSION = 45;
    private static final int SERVER_SETTINGS = 2;
    private static final int SETTINGS = 2;
    private TextView alertsChoice;
    private int allChatsTab;
    private AlphabetRail alphabetRail;
    private TextView audioCallButton;
    private TextView authStatus;
    private int authenticationGeneration;
    private boolean awaitingInstallPermission;
    private HorizontalScrollView callFilterStrip;
    private CallHistory callHistory;
    private PulseCalls calls;
    private boolean canPullRefresh;
    private boolean clipRecommendations;
    private SlidingTabs clipsTabs;
    private LinearLayout compose;
    private LinearLayout content;
    private Button createButton;
    private boolean dark;
    private String displayedAvatarKey;
    private String displayedAvatarPhoto;
    private LinearLayout dock;
    private TextView drawerClose;
    private TextView drawerTitle;
    private TextView eventsChoice;
    private boolean eventsSelected;
    private FrameLayout feedAvatarPhoto;
    private LinearLayout feedContent;
    private LinearLayout feedHeader;
    private GradientDrawable feedOnlineDot;
    private boolean feedOpen;
    private FrameLayout feedOverlay;
    private LinearLayout feedPanel;
    private int feedSection;
    private boolean feedSectionSwipe;
    private SlidingTabs feedSectionTabs;
    private HorizontalScrollView feedSectionViewport;
    private View feedShortcut;
    private TextView feedTabLabel;
    private TextView headerAction;
    private TextView headerBack;
    private TextView headerTitle;
    private Button hostButton;
    private TextView hostText;
    private LinearLayout list;
    private boolean mainBodySwipe;
    private EditText messageInput;
    private boolean notificationDrawer;
    private SlidingTabs notificationTabs;
    private ImageView notificationsButton;
    private String peer;
    private PhoneHost phoneHost;
    private String photoProfileKey;
    private File playingFile;
    private SharedPreferences preferences;
    private TextView pullRefreshHint;
    private boolean pullingRefresh;
    private View pulseBrand;
    private File recordingFile;
    private String requestedVoicePeer;
    private boolean resumed;
    private boolean revealOnRight;
    private String revealedChat;
    private ScrollView scroll;
    private LinearLayout scrollActions;
    private Button scrollDownButton;
    private Button scrollUpButton;
    private boolean sectionArchive;
    private SlidingTabs settingsTabs;
    private Button shareHostButton;
    private TextView status;
    private ChatStore store;
    private float swipeX;
    private float swipeY;
    private String swipedChat;
    private int tabSwipeDistance;
    private boolean tabSwipeHandled;
    private Button updateButton;
    private TextView updateText;
    private UpdateManager updates;
    private Button useHostButton;
    private ImageView videoCallButton;
    private Button voiceButton;
    private MediaPlayer voicePlayer;
    private MediaRecorder voiceRecorder;
    private boolean waitingForRefresh;
    private boolean watchDrawerSwipe;
    private boolean watchTabSwipe;
    private static final int[] ALL_CHATS_ORDER = {5, 0, 2, 3, 4, 6};
    private static final String[] ALL_CHATS_LABELS = {"Чаты", "Группы", "Каналы", "Сообщества", "Контакты", "Фид", "Звонки"};
    private static final String[] FEED_SECTIONS = {"Мой профиль", "Чаты", "Каналы", "Сообщества", "Контакты", "Звонки"};
    private static final int[] FEED_SECTION_TABS = {-1, 0, 2, 3, 4, 6};
    private static final int[] FEED_SECTION_ICONS = {R.drawable.ic_feed_profile, R.drawable.ic_pulse_mark, R.drawable.ic_feed_channels, R.drawable.ic_community_rail, R.drawable.ic_feed_profile, R.drawable.ic_audio_call};
    private static final String[] PROFILE_SECTIONS = {"Стена", "Фотоальбомы", "Реки"};
    private static final String[] PROFILE_SECTION_EMPTY = {"Публикаций пока нет.", "Фотоальбомов пока нет.", "Рекомендаций пока нет."};
    private static final String[] SETTINGS_TABS = {"Приложение", "Аккаунт", "Сервер"};
    private static final String[] SETTINGS_ICONS = {"⚙", "♙", "▤"};
    private int page = 0;
    private int chatFilter = 0;
    private final ImageView[] feedSectionChoices = new ImageView[FEED_SECTIONS.length];
    private int settingsTab = 0;
    private final TextView[] clipsChoices = new TextView[2];
    private final TextView[] settingsChoices = new TextView[3];
    private String connectionStatus = "";
    private final TextView[] callFilterChoices = new TextView[CallFilter.values().length];
    private CallFilter callFilter = CallFilter.ALL;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean syncing = new AtomicBoolean();
    private final Runnable hidePullRefreshHint = () -> MainActivity.this.hidePullRefreshHintNow();
    private final Runnable hostListener = () -> MainActivity.this.refreshHostControls();
    private final UpdateManager.Listener updateListener = () -> MainActivity.this.onUpdateChanged();
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            MainActivity.this.sync();
            MainActivity.this.refreshHostControls();
            MainActivity.this.handler.postDelayed(this, 6000L);
        }
    };

    enum CallFilter {
        ALL("Все", "Звонков пока нет."),
        INCOMING("Входящие", "Входящих звонков пока нет."),
        OUTGOING("Исходящие", "Исходящих звонков пока нет."),
        MISSED("Пропущенные", "Пропущенных звонков пока нет."),
        DECLINED("Отклонённые", "Отклонённых звонков пока нет."),
        UNAVAILABLE("Недоступные", "Недоступных звонков пока нет.");

        final String empty;
        final String label;

        CallFilter(String label, String empty) {
            this.label = label;
            this.empty = empty;
        }

        boolean matches(CallHistory.Entry entry) {
            switch (this) {
                case ALL:
                    return true;
                case INCOMING:
                    return entry.incoming();
                case OUTGOING:
                    return !entry.incoming();
                case MISSED:
                    return entry.outcome() == CallHistory.Outcome.MISSED;
                case DECLINED:
                    return entry.outcome() == CallHistory.Outcome.DECLINED;
                case UNAVAILABLE:
                    return entry.outcome() == CallHistory.Outcome.UNAVAILABLE;
                default:
                    throw new IncompatibleClassChangeError();
            }
        }
    }

    public void hidePullRefreshHintNow() {
        this.pullRefreshHint.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        this.preferences = getSharedPreferences("pulse", 0);
        this.dark = this.preferences.getBoolean("dark", false);
        setTheme(this.dark ? R.style.PulseDarkTheme : R.style.PulseTheme);
        super.onCreate(bundle);
        getWindow().setSoftInputMode(16);
        this.store = ChatStore.get(this);
        this.callHistory = new CallHistory(this.preferences);
        this.calls = new PulseCalls(this, () -> {
                PulseCalls.Account currentCallAccount;
                currentCallAccount = MainActivity.this.currentCallAccount();
                return currentCallAccount;
        }, obj -> {
                boolean isPeerBlocked;
                isPeerBlocked = MainActivity.this.isPeerBlocked((String) obj);
                return isPeerBlocked;
        }, (obj, obj2) -> MainActivity.this.recordCall((PulseCalls.Account) obj, (CallHistory.Entry) obj2), obj -> {
                Bitmap peerAvatar;
                peerAvatar = MainActivity.this.peerAvatar((String) obj);
                return peerAvatar;
        });
        this.updates = UpdateManager.get(this);
        this.phoneHost = PhoneHost.get(this);
        if (bundle != null) {
            boolean z = true;
            this.feedSection = Math.max(0, Math.min(PROFILE_SECTIONS.length - 1, bundle.getInt("feedSection")));
            this.page = bundle.getInt("page", 0);
            this.sectionArchive = bundle.getBoolean("sectionArchive");
            int i = bundle.getInt("callFilter");
            if (i >= 0 && i < this.callFilterChoices.length) {
                this.callFilter = CallFilter.values()[i];
            }
            this.chatFilter = bundle.getInt("chatFilter", 0);
            if (this.chatFilter != 2) {
                this.chatFilter = 0;
            }
            this.allChatsTab = bundle.getInt("allChatsTab", bundle.getBoolean("groupsSelected") ? 1 : 0);
            if (this.page == 0 && this.allChatsTab == 3 && this.chatFilter == 0) {
                this.page = 4;
            }
            if (this.allChatsTab < 0 || this.allChatsTab >= 3 || this.allChatsTab == 1) {
                this.allChatsTab = 0;
            }
            this.clipRecommendations = bundle.getBoolean("clipRecommendations");
            this.settingsTab = bundle.getInt("settingsTab", 0);
            if (this.settingsTab < 0 || this.settingsTab > 2) {
                this.settingsTab = 0;
            }
            this.peer = bundle.getString("peer");
            this.awaitingInstallPermission = bundle.getBoolean("awaitingInstallPermission", false);
            this.photoProfileKey = bundle.getString("photoProfileKey");
            boolean legacyFeedPage = this.page == 3 && !bundle.containsKey("feedOpen");
            if (!bundle.getBoolean("feedOpen") && !legacyFeedPage) {
                z = false;
            }
            this.feedOpen = z;
            this.notificationDrawer = bundle.getBoolean("notificationDrawer");
            this.eventsSelected = bundle.getBoolean("eventsSelected");
            if (this.page < 0 || this.page == 3 || this.page > 7) {
                this.page = 0;
            }
            if (this.chatFilter < 0 || this.chatFilter > 2) {
                this.chatFilter = 0;
            }
        }
        buildShell();
        render();
        if (this.feedOpen) {
            showDrawer(this.notificationDrawer, false);
        }
    }

    public PulseCalls.Account currentCallAccount() {
        if (onlineAccount()) {
            return new PulseCalls.Account(this.preferences.getString("server", ""), this.preferences.getString("token", ""), user());
        }
        return null;
    }

    public boolean isPeerBlocked(String name) {
        return this.store.isBlocked(owner(), name);
    }

    public void recordCall(PulseCalls.Account account, CallHistory.Entry entry) {
        String historyOwner = account.server() + "|" + account.username();
        this.callHistory.add(historyOwner, entry);
        if (!this.resumed || this.page != 7 || !historyOwner.equals(owner())) {
            return;
        }
        showCallHistory();
    }

    public Bitmap peerAvatar(String name) {
        return loadProfilePhoto(chatAvatarKey(name));
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.resumed = true;
        this.calls.resume();
        if (this.peer != null) {
            refreshMessages();
        }
        this.handler.post(this.poll);
        this.updates.bind(this.updateListener);
        this.phoneHost.bind(this.hostListener);
        this.updates.checkIfNeeded();
    }

    @Override
    protected void onPause() {
        this.calls.pause();
        finishVoiceRecording(false);
        stopVoicePlayback();
        this.resumed = false;
        this.handler.removeCallbacks(this.poll);
        this.updates.unbind(this.updateListener);
        this.phoneHost.unbind(this.hostListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        this.calls.destroy();
        this.handler.removeCallbacksAndMessages(null);
        this.worker.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("photoProfileKey", this.photoProfileKey);
        state.putInt("allChatsTab", this.allChatsTab);
        state.putBoolean("sectionArchive", this.sectionArchive);
        state.putInt("callFilter", this.callFilter.ordinal());
        state.putBoolean("feedOpen", this.feedOpen);
        state.putBoolean("notificationDrawer", this.notificationDrawer);
        state.putBoolean("eventsSelected", this.eventsSelected);
        state.putInt("settingsTab", this.settingsTab);
        state.putBoolean("clipRecommendations", this.clipRecommendations);
        state.putInt("feedSection", this.feedSection);
        state.putInt("page", this.page);
        state.putInt("chatFilter", this.chatFilter);
        state.putString("peer", this.peer);
        state.putBoolean("awaitingInstallPermission", this.awaitingInstallPermission);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if (!this.feedOpen) {
            if (this.peer == null) {
                if (!this.sectionArchive) {
                    if (this.page == 0) {
                        if (this.chatFilter != 2) {
                            if (!isMainSectionOpen() || this.allChatsTab == 0) {
                                super.onBackPressed();
                                return;
                            } else {
                                selectAllChatsTab(0);
                                return;
                            }
                        }
                        selectHeaderFilter(0);
                        return;
                    }
                    showMainMessages();
                    return;
                }
                setSectionArchive(false);
                return;
            }
            this.peer = null;
            this.page = 0;
            render();
            return;
        }
        hideFeedDrawer();
    }

    private boolean onlineAccount() {
        return !this.preferences.getString("token", "").isEmpty();
    }

    private String user() {
        return onlineAccount() ? this.preferences.getString("user", "") : "я";
    }

    private String owner() {
        return onlineAccount() ? this.preferences.getString("server", "") + "|" + user() : "local";
    }

    public int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    public int ink() {
        return this.dark ? 0xFFF2F3F4 : 0xFF242424;
    }

    private int accent() {
        return getColor(this.dark ? R.color.pulse_accent_dark : R.color.pulse_accent_light);
    }

    private int onAccent() {
        return this.dark ? 0xFF1A1B1D : -1;
    }

    private GradientDrawable surface(int fill, int radius, int outline) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (outline != 0) {
            shape.setStroke(dp(1.0f), outline);
        }
        return shape;
    }

    private GradientDrawable accentPill(int radius) {
        return surface(accent(), radius, 0);
    }

    private GradientDrawable headerAccentPill() {
        return surface(this.dark ? accent() : 0xFF343A41, 24, 0);
    }

    private int onHeaderAccent() {
        return onAccent();
    }

    private int muted() {
        return this.dark ? 0xFFB9BCC0 : 0xFF5C5C5C;
    }

    private int headerInk() {
        return ink();
    }

    private Drawable barSurface(int fill, int edge) {
        LayerDrawable background = new LayerDrawable(new Drawable[]{new ColorDrawable(fill), new ColorDrawable(surfaceOutline())});
        background.setLayerHeight(1, dp(1.0f));
        background.setLayerGravity(1, edge | 7);
        return background;
    }

    private Drawable headerSurface() {
        return barSurface(this.dark ? 0xFF222426 : 0xFFE6E9EC, 80);
    }

    private int surfaceOutline() {
        return this.dark ? 0xFF4B4E52 : 0x7A747D88;
    }

    private GradientDrawable card() {
        int fill = this.dark ? 0xFF292B2E : 0xFFF0F2F4;
        return surface(fill, 14, surfaceOutline());
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    public TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, 1);
        }
        return view;
    }

    private Button button(String title, boolean primary) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(15.0f);
        b.setMinHeight(dp(48.0f));
        b.setTextColor(primary ? onAccent() : ink());
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(this.dark ? 0x33FFFFFF : 0x26303030), primary ? accentPill(16) : card(), null));
        return b;
    }

    private void applySystemBarColors(int background) {
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        WindowInsetsController insets = Build.VERSION.SDK_INT >= 30 ? getWindow().getInsetsController() : null;
        if (insets != null) {
            insets.setSystemBarsAppearance(this.dark ? 0 : 24, 24);
        } else if (!this.dark) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() | 8192 | 16);
        }
    }

    private void buildShell() {
        LinearLayout column = column();
        int background = this.dark ? 0xFF191A1C : 0xFFDDE2E7;
        column.setBackgroundColor(background);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(column, new FrameLayout.LayoutParams(-1, -1));
        setContentView(frameLayout);
        if (Build.VERSION.SDK_INT >= 29) {
            frameLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> view.setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, dp(24.0f), Math.min(bottom - top, dp(200.0f))))));
        }
        column.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.open_feed_drawer, "Открыть ленту"));
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
                if (action == R.id.open_feed_drawer) {
                    MainActivity.this.showFeedDrawer(true);
                    return true;
                }
                return super.performAccessibilityAction(host, action, arguments);
            }
        });
        applySystemBarColors(background);
        LinearLayout column2 = column();
        column2.setBackground(headerSurface());
        ViewGroup.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        column.addView(column2, headerParams);
        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        headingRow.setMinimumHeight(dp(56.0f));
        int headerInset = dp(getResources().getConfiguration().screenWidthDp >= 360 ? 18.0f : 12.0f);
        headingRow.setPadding(headerInset, dp(6.0f), headerInset, dp(6.0f));
        FrameLayout headerLine = new FrameLayout(this);
        column2.addView(headerLine, new LinearLayout.LayoutParams(-1, -2));
        headerLine.addView(headingRow, new FrameLayout.LayoutParams(-1, -2));
        this.pulseBrand = buildPulseBrand();
        headerLine.addView(this.pulseBrand, new FrameLayout.LayoutParams(-2, dp(40.0f), Gravity.CENTER));
        this.headerBack = headerControl("‹");
        headingRow.addView(this.headerBack, new LinearLayout.LayoutParams(dp(40.0f), dp(40.0f)));
        this.feedShortcut = buildFeedTab(headingRow);
        this.settingsTabs = buildSettingsTabs(headingRow);
        buildClipsTabs(headingRow);
        LinearLayout heading = column();
        heading.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        headingParams.setMargins(dp(6.0f), 0, dp(6.0f), 0);
        headingRow.addView(heading, headingParams);
        this.headerTitle = text("", 20, headerInk(), true);
        this.headerTitle.setGravity(Gravity.CENTER);
        this.headerTitle.setSingleLine(true);
        this.headerTitle.setEllipsize(TextUtils.TruncateAt.END);
        heading.addView(this.headerTitle, new LinearLayout.LayoutParams(-1, -2));
        this.status = text("", 12, this.dark ? 0xFFB9BCC0 : 0xFF5C5C5C, false);
        this.status.setGravity(Gravity.CENTER);
        heading.addView(this.status);
        this.notificationsButton = new ImageView(this);
        GradientDrawable notificationBackground = surface(this.dark ? 0xFF2D2F32 : 0xFFF2F4F6, 24, this.dark ? 0x55FFFFFF : 0x55798089);
        notificationBackground.setShape(1);
        this.notificationsButton.setBackground(notificationBackground);
        this.notificationsButton.setPadding(dp(11.0f), dp(11.0f), dp(11.0f), dp(11.0f));
        this.notificationsButton.setImageResource(R.drawable.ic_notification_bell);
        this.notificationsButton.setImageTintList(ColorStateList.valueOf(headerInk()));
        this.notificationsButton.setContentDescription("Уведомления");
        this.notificationsButton.setFocusable(true);
        this.notificationsButton.setOnClickListener(v -> showDrawer(true, true));
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(dp(44.0f), dp(44.0f));
        notificationParams.setMarginStart(dp(6.0f));
        headingRow.addView(this.notificationsButton, notificationParams);
        this.audioCallButton = text("Звонок", 12, headerInk(), true);
        this.audioCallButton.setGravity(Gravity.CENTER);
        Drawable handset = getDrawable(R.drawable.ic_audio_call).mutate();
        handset.setTint(headerInk());
        handset.setBounds(0, 0, dp(15.0f), dp(15.0f));
        this.audioCallButton.setPadding(dp(7.0f), 0, dp(7.0f), 0);
        this.audioCallButton.setCompoundDrawablePadding(dp(4.0f));
        this.audioCallButton.setCompoundDrawablesRelative(handset, null, null, null);
        this.audioCallButton.setBackground(surface(this.dark ? 0xFF2D2F32 : 0xFFF2F4F6, 18, surfaceOutline()));
        this.audioCallButton.setContentDescription("Аудиозвонок");
        this.audioCallButton.setFocusable(true);
        this.audioCallButton.setOnClickListener(v -> {
            if (this.peer != null) {
                this.calls.call(this.peer);
            }

        });
        headingRow.addView(this.audioCallButton, new LinearLayout.LayoutParams(dp(76.0f), dp(36.0f)));
        this.videoCallButton = addCallButton(headingRow, R.drawable.ic_video_call, "Видеозвонок");
        this.headerAction = headerControl("↻");
        headingRow.addView(this.headerAction, new LinearLayout.LayoutParams(dp(38.0f), dp(40.0f)));
        buildCallFilters(column);
        FrameLayout body = new FrameLayout(this);
        column.addView(body, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.scroll = new ScrollView(this);
        this.scroll.setFillViewport(true);
        body.addView(this.scroll, new FrameLayout.LayoutParams(-1, -1));
        this.content = column();
        this.content.setPadding(dp(16.0f), dp(12.0f), dp(16.0f), dp(12.0f));
        this.scroll.addView(this.content);
        this.scrollActions = new LinearLayout(this);
        this.scrollActions.setGravity(Gravity.CENTER_VERTICAL);
        this.scrollUpButton = floatingButton("↑", "К началу списка");
        this.scrollUpButton.setVisibility(View.GONE);
        this.scrollUpButton.setOnClickListener(v -> this.scroll.smoothScrollTo(0, 0));
        this.scrollDownButton = floatingButton("↓", "К концу списка");
        this.scrollDownButton.setOnClickListener(v -> this.scroll.smoothScrollTo(0, this.content.getHeight()));
        int i = 2;
        int i2 = 0;
        Button[] buttonArr = {this.scrollUpButton, this.scrollDownButton};
        int i3 = 0;
        while (i3 < i) {
            Button arrow = buttonArr[i3];
            arrow.setPadding(i2, i2, i2, i2);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(48.0f), dp(48.0f));
            arrowParams.setMarginEnd(dp(6.0f));
            this.scrollActions.addView(arrow, arrowParams);
            i3++;
            buttonArr = buttonArr;
            i = 2;
            i2 = 0;
        }
        FrameLayout.LayoutParams arrowGroupParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.LEFT);
        arrowGroupParams.leftMargin = dp(18.0f);
        arrowGroupParams.bottomMargin = dp(18.0f);
        body.addView(this.scrollActions, arrowGroupParams);
        this.scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> refreshScrollControls());
        this.scroll.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> refreshScrollControls());
        this.createButton = floatingButton("", "");
        this.createButton.setOnClickListener(v -> newChat());
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.RIGHT);
        fabParams.rightMargin = dp(18.0f);
        fabParams.bottomMargin = dp(18.0f);
        fabParams.height = dp(54.0f);
        body.addView(this.createButton, fabParams);
        this.alphabetRail = new AlphabetRail();
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(dp(26.0f), -1, Gravity.RIGHT);
        railParams.setMargins(0, dp(10.0f), dp(6.0f), dp(84.0f));
        body.addView(this.alphabetRail, railParams);
        this.pullRefreshHint = text("", 13, ink(), true);
        this.pullRefreshHint.setGravity(Gravity.CENTER);
        this.pullRefreshHint.setPadding(dp(14.0f), dp(8.0f), dp(14.0f), dp(8.0f));
        this.pullRefreshHint.setBackground(card());
        this.pullRefreshHint.setAccessibilityLiveRegion(1);
        this.pullRefreshHint.setVisibility(View.GONE);
        body.addView(this.pullRefreshHint, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        this.compose = column();
        column.addView(this.compose, new LinearLayout.LayoutParams(-1, -2));
        buildFeedSectionTabs(column);
        this.dock = new LinearLayout(this);
        this.dock.setGravity(Gravity.CENTER_VERTICAL);
        this.dock.setPadding(dp(4.0f), dp(3.0f), dp(4.0f), dp(3.0f));
        int dockColor = this.dark ? 0xFF242628 : 0xFFE9EDF0;
        this.dock.setBackground(barSurface(dockColor, 48));
        column.addView(this.dock, new LinearLayout.LayoutParams(-1, -2));
        buildFeedDrawer(frameLayout, background);
    }

    private Button floatingButton(String label, String description) {
        Button action = new Button(this);
        action.setText(label);
        action.setContentDescription(description);
        action.setTextSize(15.0f);
        action.setAllCaps(false);
        action.setMinWidth(0);
        action.setMinimumWidth(0);
        action.setPadding(dp(18.0f), 0, dp(18.0f), 0);
        action.setTextColor(onAccent());
        GradientDrawable pill = accentPill(28);
        action.setBackground(new RippleDrawable(ColorStateList.valueOf(this.dark ? 0x33303030 : 0x33FFFFFF), pill, null));
        action.setStateListAnimator(null);
        return action;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                this.tabSwipeHandled = false;
                this.swipeX = event.getRawX();
                this.swipeY = event.getRawY();
                this.feedSectionSwipe = !this.feedOpen && containsTouch(this.feedSectionViewport, event);
                this.canPullRefresh = !this.feedSectionSwipe && !this.feedOpen && !this.waitingForRefresh
                        && (isMainPage() || this.peer != null)
                        && containsTouch(this.scroll, event) && !this.scroll.canScrollVertically(1)
                        && !containsTouch(this.alphabetRail, event) && !containsTouch(this.createButton, event)
                        && !containsTouch(this.scrollActions, event);
                this.pullingRefresh = false;
                this.swipedChat = null;
                if (!this.feedOpen && this.peer == null && this.page == 0 && this.list != null && containsTouch(this.scroll, event)) {
                    for (int i = 0; i < this.list.getChildCount(); i++) {
                        View row = this.list.getChildAt(i);
                        Object tag = row.getTag();
                        if ((tag instanceof String) && containsTouch(row, event)) {
                            this.swipedChat = (String) tag;
                            break;
                        }
                    }
                }
                boolean onMainHeader = containsTouch(this.feedShortcut, event);
                boolean onSettingsHeaderTabs = containsTouch(this.settingsTabs, event);
                boolean onClipsHeaderTabs = containsTouch(this.clipsTabs, event);
                boolean onHeaderTabs = onSettingsHeaderTabs || onClipsHeaderTabs;
                boolean onDock = containsTouch(this.dock, event);
                this.tabSwipeDistance = dp(onHeaderTabs ? 24.0f : 40.0f);
                this.mainBodySwipe = isMainPage() && containsTouch(this.scroll, event);
                if (this.feedOpen) {
                    this.watchDrawerSwipe = containsTouch(this.feedHeader, event);
                } else {
                    this.watchDrawerSwipe = this.swipeX <= dp(28.0f) && !onHeaderTabs && !onMainHeader && !onDock
                            && !this.feedSectionSwipe && !containsTouch(this.headerBack, event);
                }
                this.watchTabSwipe = !this.feedSectionSwipe && !this.feedOpen && !this.watchDrawerSwipe && this.peer == null
                        && !containsTouch(this.alphabetRail, event) && (this.mainBodySwipe || onHeaderTabs);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE: {
                boolean finished = event.getActionMasked() == MotionEvent.ACTION_UP;
                if (this.tabSwipeHandled) {
                    if (finished) {
                        this.tabSwipeHandled = false;
                    }
                    return true;
                }
                if (this.canPullRefresh) {
                    float lift = this.swipeY - event.getRawY();
                    float sideways = Math.abs(event.getRawX() - this.swipeX);
                    if (!this.pullingRefresh && lift >= dp(14.0f) && lift > sideways * 1.5f) {
                        this.pullingRefresh = true;
                        this.watchTabSwipe = false;
                        this.watchDrawerSwipe = false;
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                    }
                    if (this.pullingRefresh) {
                        boolean ready = lift >= dp(56.0f);
                        if (finished) {
                            this.canPullRefresh = false;
                            this.pullingRefresh = false;
                            if (ready) {
                                refreshFromBottom();
                            } else {
                                this.pullRefreshHint.setVisibility(View.GONE);
                            }
                        } else {
                            showPullRefreshHint(ready ? "Отпустите, чтобы обновить" : "Потяните вверх, чтобы обновить", false);
                        }
                        return true;
                    }
                    if (finished || (sideways > dp(18.0f) && sideways > Math.abs(lift))) {
                        this.canPullRefresh = false;
                    }
                }
                if (this.watchTabSwipe || this.watchDrawerSwipe || this.feedSectionSwipe) {
                    float dx = event.getRawX() - this.swipeX;
                    float dy = Math.abs(event.getRawY() - this.swipeY);
                    if (dy > dp(20.0f) && dy > Math.abs(dx)) {
                        this.watchTabSwipe = false;
                        this.watchDrawerSwipe = false;
                    }
                    int drawerDistance = dp(40.0f);
                    boolean drawerGesture = this.watchDrawerSwipe && (this.feedOpen ? dx <= -drawerDistance : dx >= drawerDistance);
                    boolean tabGesture = this.watchTabSwipe && Math.abs(dx) >= this.tabSwipeDistance;
                    boolean feedGesture = this.feedSectionSwipe && Math.abs(dx) >= dp(24.0f) && Math.abs(dx) > dy * 1.5f;
                    if (feedGesture || ((tabGesture || drawerGesture) && Math.abs(dx) > dy * 1.5f)) {
                        this.feedSectionSwipe = false;
                        this.watchTabSwipe = false;
                        this.watchDrawerSwipe = false;
                        this.tabSwipeHandled = !finished;
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                        int step = dx < 0.0f ? 1 : -1;
                        if (feedGesture) {
                            selectFeedSection(activeFeedSection() + step);
                            return true;
                        }
                        if (drawerGesture) {
                            if (this.feedOpen) {
                                hideFeedDrawer();
                            } else {
                                showFeedDrawer(true);
                            }
                            return true;
                        }
                        if (this.swipedChat != null) {
                            revealChatArchive(this.swipedChat, dx < 0.0f);
                        } else if (this.mainBodySwipe) {
                            swipeMainPage(step);
                        } else if (containsTouch(this.clipsTabs, this.swipeX, this.swipeY)) {
                            selectClipsTab(step > 0);
                        } else if (containsTouch(this.settingsTabs, this.swipeX, this.swipeY)) {
                            selectSectionTab(Math.max(0, Math.min(2, this.settingsTab + step)));
                        }
                        return true;
                    }
                }
                if (finished) {
                    this.feedSectionSwipe = false;
                    this.watchDrawerSwipe = false;
                    this.watchTabSwipe = false;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:
                this.feedSectionSwipe = false;
                if (this.pullingRefresh) {
                    this.pullRefreshHint.setVisibility(View.GONE);
                }
                this.canPullRefresh = false;
                this.pullingRefresh = false;
                this.watchDrawerSwipe = false;
                this.watchTabSwipe = false;
                if (this.tabSwipeHandled) {
                    this.tabSwipeHandled = false;
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void showPullRefreshHint(String message, boolean dismiss) {
        this.handler.removeCallbacks(this.hidePullRefreshHint);
        this.pullRefreshHint.setText(message);
        this.pullRefreshHint.setVisibility(View.VISIBLE);
        if (dismiss) {
            this.handler.postDelayed(this.hidePullRefreshHint, 2200L);
        }
    }

    private void refreshFromBottom() {
        if (this.page == 7) {
            showCallHistory();
            showPullRefreshHint("Обновлено", true);
            return;
        }
        if (onlineAccount()) {
            this.waitingForRefresh = true;
            showPullRefreshHint("Обновление…", false);
            sync();
        } else {
            if (this.peer != null) {
                refreshMessages();
            } else if (this.list != null) {
                refreshChats();
            } else {
                render();
            }
            showPullRefreshHint("Обновлено", true);
        }
    }

    private boolean containsTouch(View view, MotionEvent event) {
        return containsTouch(view, event.getRawX(), event.getRawY());
    }

    private boolean containsTouch(View view, float rawX, float rawY) {
        if (view == null || !view.isShown()) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = rawX - location[0];
        float y = rawY - location[1];
        if (x < 0.0f || x >= view.getWidth() || y < 0.0f || y >= view.getHeight()) {
            return false;
        }
        return true;
    }

    private void addDock(String symbol, String label, boolean selected, final Runnable click) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2.0f), dp(2.0f), dp(2.0f), dp(2.0f));
        item.setMinimumHeight(dp(54.0f));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(label);
        item.setSelected(selected);
        int iconColor = selected ? onAccent() : ink();
        int circleColor = selected ? accent() : this.dark ? 0xFF36393C : 0xFFE2E2E2;
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(1);
        circle.setColor(circleColor);
        TextView icon = text(symbol, 21, iconColor, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(circle);
        icon.setImportantForAccessibility(2);
        TextView name = text(label, 11, selected ? accent() : muted(), selected);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER);
        name.setImportantForAccessibility(2);
        item.addView(icon, new LinearLayout.LayoutParams(dp(34.0f), dp(34.0f)));
        item.addView(name);
        item.setOnClickListener(view -> click.run());
        this.dock.addView(item, new LinearLayout.LayoutParams(0, -2, 1.0f));
    }

    private void render() {
        finishVoiceRecording(false);
        this.voiceButton = null;
        this.revealedChat = null;
        this.content.removeAllViews();
        this.compose.removeAllViews();
        this.dock.removeAllViews();
        this.list = null;
        this.messageInput = null;
        this.updateText = null;
        this.updateButton = null;
        this.hostText = null;
        this.hostButton = null;
        this.useHostButton = null;
        this.shareHostButton = null;
        this.authStatus = null;
        updateStatus();
        updateHeader();
        renderChatTabs();
        renderFeedSectionTabs();
        renderCallFilters();
        renderSectionTabs();
        boolean showEmptySection = this.page == 0 && isMainSectionOpen() && this.allChatsTab != 0;
        boolean showFab = this.peer == null && !showEmptySection && (this.page == 0 || this.page == 1);
        if (showFab) {
            String label = this.page == 1 ? "Новый контакт" : "Создать чат";
            this.createButton.setText("+  " + label);
            this.createButton.setContentDescription(label);
        }
        this.createButton.setVisibility(showFab ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams refreshHintParams = (FrameLayout.LayoutParams) this.pullRefreshHint.getLayoutParams();
        refreshHintParams.bottomMargin = dp(showFab ? 80.0f : 12.0f);
        this.pullRefreshHint.setLayoutParams(refreshHintParams);
        if (!isMainPage() && this.peer == null) {
            this.pullRefreshHint.setVisibility(View.GONE);
        }
        this.scrollActions.setVisibility((showFab && this.page == 0) ? View.VISIBLE : View.GONE);
        this.alphabetRail.setVisibility(View.GONE);
        int reservedBottom = showFab ? 86 : 12;
        this.content.setPadding(dp(16.0f), dp(12.0f), dp(16.0f), dp(reservedBottom));
        this.dock.setVisibility(this.peer == null ? View.VISIBLE : View.GONE);
        addDock("◉", "Главная", isMainPage(), () -> MainActivity.this.showMainMessages());
        addDock("⚙", "Настройки", this.page == 2, () -> {
            this.page = 2;
            this.peer = null;
            this.sectionArchive = false;
            render();

        });
        addDock("▶", "Клипы", this.page == 5, () -> {
            this.page = 5;
            this.peer = null;
            this.sectionArchive = false;
            render();

        });
        if (this.peer != null) {
            showConversation();
        } else if (this.page == 2) {
            showSettings();
        } else if (this.page == 4) {
            showCommunities();
        } else if (this.page == 5) {
            showClips();
        } else if (this.page == 6) {
            showFeedPage();
        } else if (this.page == 7) {
            showCallHistory();
        } else if (showEmptySection) {
            showSectionWithArchive("К каналам", "Каналов пока нет.", "В архиве пока нет каналов.");
        } else {
            showChats();
        }
        this.scroll.scrollTo(0, 0);
        refreshScrollControls();
        this.content.animate().cancel();
        this.content.setAlpha(0.85f);
        this.content.animate().alpha(1.0f).setDuration(120L).start();
    }

    private LinearLayout newCard() {
        return newCard(this.content);
    }

    private LinearLayout newCard(LinearLayout parent) {
        LinearLayout card = column();
        card.setPadding(dp(16.0f), dp(16.0f), dp(16.0f), dp(16.0f));
        card.setBackground(card());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12.0f);
        parent.addView(card, params);
        return card;
    }

    private TextView allChatsChoice(LinearLayout row, String label, int size, int width, final Runnable select) {
        TextView choice = text(label, size, ink(), true);
        choice.setGravity(Gravity.CENTER);
        choice.setSingleLine(true);
        choice.setFocusable(true);
        choice.setOnClickListener(view -> select.run());
        if (width == 0) {
            choice.setAutoSizeTextTypeUniformWithConfiguration(9, size, 1, 2);
        }
        row.addView(choice, new LinearLayout.LayoutParams(dp(width), dp(44.0f), width == 0 ? 1.0f : 0.0f));
        return choice;
    }

    private boolean isMainSectionOpen() {
        return this.peer == null && isMainPage() && (this.page != 0 || this.chatFilter == 0);
    }

    private int activeChatsTab() {
        if (this.page == 4) {
            return 3;
        }
        if (this.page == 1) {
            return 4;
        }
        if (this.page == 6) {
            return 5;
        }
        if (this.page == 7) {
            return 6;
        }
        return this.allChatsTab;
    }

    private int adjacentChatsTab(int direction) {
        int active = activeChatsTab();
        for (int i = 0; i < ALL_CHATS_ORDER.length; i++) {
            if (ALL_CHATS_ORDER[i] == active) {
                int next = i + direction;
                if (next < 0 || next >= ALL_CHATS_ORDER.length) {
                    return -1;
                }
                return ALL_CHATS_ORDER[next];
            }
        }
        return -1;
    }

    private void selectAllChatsTab(int tab) {
        int i;
        if (tab < 0 || tab >= ALL_CHATS_LABELS.length || tab == 1) {
            return;
        }
        if (isMainSectionOpen() && tab == activeChatsTab()) {
            return;
        }
        switch (tab) {
            case 3:
                i = 4;
                break;
            case 4:
                i = 1;
                break;
            case 5:
                i = 6;
                break;
            case 6:
                i = 7;
                break;
            default:
                i = 0;
                break;
        }
        this.page = i;
        if (this.page == 0) {
            this.allChatsTab = tab;
        }
        this.chatFilter = 0;
        this.sectionArchive = false;
        render();
    }

    public void showMainMessages() {
        this.page = 0;
        this.peer = null;
        this.chatFilter = 0;
        this.allChatsTab = 0;
        this.sectionArchive = false;
        render();
    }

    private View buildPulseBrand() {
        LinearLayout badge = new LinearLayout(this);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12.0f), 0, dp(12.0f), 0);
        badge.setBackground(surface(this.dark ? 0xFF2A2C2F : 0xFFF1F3F5, 17, surfaceOutline()));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_pulse_mark);
        icon.setImageTintList(ColorStateList.valueOf(headerInk()));
        icon.setImportantForAccessibility(2);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(18.0f), dp(18.0f));
        iconParams.setMarginEnd(dp(5.0f));
        badge.addView(icon, iconParams);
        TextView wordmark = text("Pulse", 16, headerInk(), true);
        wordmark.setLetterSpacing(0.04f);
        badge.addView(wordmark);
        return badge;
    }

    private void buildCallFilters(LinearLayout root) {
        this.callFilterStrip = new HorizontalScrollView(this);
        this.callFilterStrip.setHorizontalScrollBarEnabled(false);
        this.callFilterStrip.setClipToPadding(false);
        this.callFilterStrip.setPadding(dp(16.0f), dp(8.0f), dp(16.0f), 0);
        LinearLayout filters = new LinearLayout(this);
        this.callFilterStrip.addView(filters, new FrameLayout.LayoutParams(-2, -2));
        for (final CallFilter filter : CallFilter.values()) {
            TextView choice = text(filter.label, 14, ink(), true);
            choice.setGravity(Gravity.CENTER);
            choice.setPadding(dp(14.0f), 0, dp(14.0f), 0);
            choice.setFocusable(true);
            choice.setOnClickListener(v -> {
                if (this.callFilter == filter) {
                    return;
                }
                this.callFilter = filter;
                renderCallFilters();
                showCallHistory();
                this.scroll.scrollTo(0, 0);

            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(44.0f));
            params.setMarginEnd(dp(6.0f));
            filters.addView(choice, params);
            this.callFilterChoices[filter.ordinal()] = choice;
        }
        root.addView(this.callFilterStrip, new LinearLayout.LayoutParams(-1, -2));
    }

    private void renderCallFilters() {
        boolean visible = this.page == 7 && this.peer == null;
        this.callFilterStrip.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            int i = 0;
            while (i < this.callFilterChoices.length) {
                TextView choice = this.callFilterChoices[i];
                boolean selected = i == this.callFilter.ordinal();
                choice.setSelected(selected);
                choice.setTextColor(selected ? onAccent() : ink());
                choice.setBackground(selected ? accentPill(22) : surface(0, 22, surfaceOutline()));
                i++;
            }
        }
    }

    private void showCallHistory() {
        this.content.removeAllViews();
        boolean empty = true;
        for (CallHistory.Entry entry : this.callHistory.entries(owner())) {
            if (this.callFilter.matches(entry)) {
                empty = false;
                LinearLayout card = newCard();
                card.addView(text(entry.peer(), 16, ink(), true));
                String direction = entry.incoming() ? "Входящий" : "Исходящий";
                String date = DateUtils.formatDateTime(this, entry.time(), 17);
                TextView details = text(direction + " · " + entry.outcome().label + " · " + date, 13, muted(), false);
                details.setPadding(0, dp(4.0f), 0, 0);
                card.addView(details);
            }
        }
        if (empty) {
            newCard().addView(text(this.callFilter.empty, 15, muted(), false));
        }
    }

    private boolean isMainPage() {
        return this.page == 0 || this.page == 6 || this.page == 1 || this.page == 4 || this.page == 7;
    }

    private void swipeMainPage(int direction) {
        selectAllChatsTab(adjacentChatsTab(direction));
    }

    private void selectHeaderFilter(int filter) {
        if (this.chatFilter == filter) {
            return;
        }
        this.chatFilter = filter;
        render();
    }

    private void renderChatTabs() {
        GradientDrawable surface;
        boolean visible = isMainPage() && this.peer == null;
        this.feedShortcut.setVisibility(visible ? View.VISIBLE : View.GONE);
        this.pulseBrand.setVisibility(visible ? View.VISIBLE : View.GONE);
        boolean selected = this.page == 6;
        this.feedShortcut.setSelected(selected);
        View view = this.feedShortcut;
        if (selected) {
            surface = headerAccentPill();
        } else {
            surface = surface(this.dark ? 0xFF2A2C2F : 0xFFF1F3F5, 22, surfaceOutline());
        }
        view.setBackground(surface);
        this.feedTabLabel.setTextColor(selected ? onHeaderAccent() : headerInk());
    }

    final class SlidingTabs extends FrameLayout {
        private final Rect bounds;
        private final GradientDrawable pill;
        private ValueAnimator slide;

        SlidingTabs(GradientDrawable pill) {
            super(MainActivity.this);
            this.bounds = new Rect();
            this.pill = pill;
            setWillNotDraw(false);
        }

        void moveTo(final View tab, boolean animate) {
            SlidingTabs slidingTabs;
            View tab2;
            if (tab.getWidth() == 0) {
                return;
            }
            if (this.slide != null) {
                this.slide.cancel();
            }
            if (!animate) {
                slidingTabs = this;
                tab2 = tab;
            } else {
                if (!this.bounds.isEmpty()) {
                    final int fromLeft = this.bounds.left;
                    final int fromRight = this.bounds.right;
                    final int fromTop = this.bounds.top;
                    final int fromBottom = this.bounds.bottom;
                    this.slide = ValueAnimator.ofFloat(0.0f, 1.0f);
                    this.slide.setDuration(180L);
                    this.slide.addUpdateListener(animation -> {
                            float progress = animation.getAnimatedFraction();
                            int left = Math.round((tab.getLeft() - fromLeft) * progress) + fromLeft;
                            int right = Math.round((tab.getRight() - fromRight) * progress) + fromRight;
                            int top = Math.round((tab.getTop() - fromTop) * progress) + fromTop;
                            int bottom = Math.round((tab.getBottom() - fromBottom) * progress) + fromBottom;
                            this.bounds.set(left, top, right, bottom);
                            invalidate();

                    });
                    this.slide.start();
                    return;
                }
                slidingTabs = this;
                tab2 = tab;
            }
            slidingTabs.bounds.set(tab2.getLeft(), tab2.getTop(), tab2.getRight(), tab2.getBottom());
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            this.pill.setBounds(this.bounds);
            this.pill.draw(canvas);
        }
    }

    final class AlphabetRail extends LinearLayout {
        private static final String LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
        private int lastIndex;
        private final TextView[] letters;
        private final View[] targets;

        AlphabetRail() {
            super(MainActivity.this);
            this.targets = new View[LABELS.length()];
            this.letters = new TextView[LABELS.length()];
            this.lastIndex = -1;
            setOrientation(LinearLayout.VERTICAL);
            setPadding(0, MainActivity.this.dp(4.0f), 0, MainActivity.this.dp(4.0f));
            setContentDescription("Алфавитная прокрутка");
            for (int i = 0; i < LABELS.length(); i++) {
                final int index = i;
                TextView letter = MainActivity.this.text("•", 9, MainActivity.this.ink(), true);
                letter.setContentDescription(String.valueOf(LABELS.charAt(i)));
                letter.setGravity(Gravity.CENTER);
                letter.setOnClickListener(view -> MainActivity.AlphabetRail.this.jumpToLetter(index, view));
                this.letters[i] = letter;
                addView(letter, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            }
        }

        public void jumpToLetter(int index, View view) {
            jumpTo(index);
        }

        static int bucket(String name) {
            int letter = name.isEmpty() ? -1 : Character.toUpperCase(name.charAt(0)) - 'A';
            if (letter < 0 || letter >= 26) {
                return 26;
            }
            return letter;
        }

        void bind(LinearLayout rows, List<String> names) {
            Arrays.fill(this.targets, (Object) null);
            for (int i = 0; i < names.size(); i++) {
                int letter = bucket(names.get(i));
                if (this.targets[letter] == null) {
                    this.targets[letter] = rows.getChildAt(i);
                }
            }
            for (int i2 = 0; i2 < this.letters.length; i2++) {
                this.letters[i2].setEnabled(this.targets[i2] != null);
                this.letters[i2].setAlpha(this.targets[i2] != null ? 1.0f : 0.4f);
            }
            MainActivity.this.refreshScrollControls();
        }

        private void jumpTo(int index) {
            View target = this.targets[index];
            if (target == null) {
                return;
            }
            Rect position = new Rect();
            target.getDrawingRect(position);
            MainActivity.this.scroll.offsetDescendantRectToMyCoords(target, position);
            MainActivity.this.scroll.scrollTo(0, position.top);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return event.getActionMasked() == 0 || super.onInterceptTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int index;
            switch (event.getActionMasked()) {
                case 0:
                    showMarker(-1);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case 1:
                case 2:
                    break;
                case 3:
                    showMarker(-1);
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
            int height = (getHeight() - getPaddingTop()) - getPaddingBottom();
            if (height > 0 && this.lastIndex != (index = Math.max(0, Math.min(this.letters.length - 1, (int) (((event.getY() - getPaddingTop()) * this.letters.length) / height))))) {
                jumpTo(index);
                showMarker(index);
            }
            if (event.getActionMasked() == 1) {
                showMarker(-1);
            }
            return true;
        }

        private void showMarker(int index) {
            if (this.lastIndex >= 0) {
                this.letters[this.lastIndex].setText("•");
            }
            if (index >= 0) {
                this.letters[index].setText(String.valueOf(LABELS.charAt(index)));
            }
            this.lastIndex = index;
        }
    }

    private void buildFeedDrawer(FrameLayout windowContent, int background) {
        this.feedOverlay = new FrameLayout(this);
        this.feedOverlay.setBackgroundColor(Integer.MIN_VALUE);
        this.feedOverlay.setOnClickListener(v -> hideFeedDrawer());
        this.feedOverlay.setVisibility(View.GONE);
        windowContent.addView(this.feedOverlay, new FrameLayout.LayoutParams(-1, -1));
        this.feedPanel = column();
        this.feedPanel.setClickable(true);
        this.feedPanel.setBackgroundColor(background);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(-1, -1, Gravity.LEFT);
        panelParams.rightMargin = dp(24.0f);
        this.feedOverlay.addView(this.feedPanel, panelParams);
        this.feedHeader = new LinearLayout(this);
        this.feedHeader.setGravity(Gravity.CENTER_VERTICAL);
        this.feedHeader.setPadding(dp(12.0f), dp(12.0f), dp(12.0f), dp(12.0f));
        this.feedHeader.setBackground(headerSurface());
        this.feedPanel.addView(this.feedHeader, new LinearLayout.LayoutParams(-1, -2));
        this.drawerTitle = text("Лента", 20, headerInk(), true);
        this.drawerTitle.setGravity(Gravity.CENTER);
        this.feedHeader.addView(this.drawerTitle, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.drawerClose = headerControl("×");
        this.drawerClose.setOnClickListener(v -> hideFeedDrawer());
        this.feedHeader.addView(this.drawerClose, new LinearLayout.LayoutParams(dp(40.0f), dp(40.0f)));
        buildNotificationTabs();
        ScrollView feedScroll = new ScrollView(this);
        this.feedPanel.addView(feedScroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.feedContent = column();
        this.feedContent.setPadding(dp(16.0f), dp(12.0f), dp(16.0f), dp(12.0f));
        feedScroll.addView(this.feedContent, new FrameLayout.LayoutParams(-1, -2));
    }

    private void buildNotificationTabs() {
        this.notificationTabs = new SlidingTabs(accentPill(22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(16.0f), dp(8.0f), dp(16.0f), 0);
        this.feedPanel.addView(this.notificationTabs, params);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(3.0f), dp(3.0f), dp(3.0f), dp(3.0f));
        this.notificationTabs.addView(row, new FrameLayout.LayoutParams(-1, -2));
        this.alertsChoice = allChatsChoice(row, "Уведомления", 13, 0, () -> selectNotificationTab(false));
        this.eventsChoice = allChatsChoice(row, "События", 13, 0, () -> selectNotificationTab(true));
        row.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
                this.notificationTabs.moveTo(this.eventsSelected ? this.eventsChoice : this.alertsChoice, false);
            }

        });
    }

    private void selectNotificationTab(boolean events) {
        if (this.eventsSelected == events) {
            return;
        }
        this.eventsSelected = events;
        this.notificationTabs.moveTo(events ? this.eventsChoice : this.alertsChoice, true);
        renderFeedDrawer();
    }

    public void showFeedDrawer(boolean animate) {
        showDrawer(false, animate);
    }

    private void showDrawer(boolean notifications, boolean animate) {
        this.notificationDrawer = notifications;
        this.feedOpen = true;
        this.feedOverlay.setVisibility(View.VISIBLE);
        ((ViewGroup) this.feedOverlay.getParent()).getChildAt(0).setImportantForAccessibility(4);
        renderFeedDrawer();
        this.feedPanel.animate().cancel();
        if (!animate) {
            this.feedPanel.setTranslationX(0.0f);
        } else {
            this.feedPanel.setTranslationX(-this.feedOverlay.getWidth());
            this.feedPanel.animate().translationX(0.0f).setDuration(220L).start();
        }
    }

    private void hideFeedDrawer() {
        if (this.feedOpen) {
            this.feedOpen = false;
            this.feedPanel.animate().cancel();
            this.feedPanel.animate().translationX(-this.feedPanel.getWidth()).setDuration(170L).withEndAction(() -> {
                if (!this.feedOpen) {
                    this.feedOverlay.setVisibility(View.GONE);
                    if (this.peer != null) {
                        refreshMessages();
                    }
                    ((ViewGroup) this.feedOverlay.getParent()).getChildAt(0).setImportantForAccessibility(0);
                }

            }).start();
        }
    }

    private void renderFeedDrawer() {
        this.drawerTitle.setText(this.notificationDrawer ? "Центр уведомлений" : "Лента");
        this.drawerClose.setContentDescription(this.notificationDrawer ? "Закрыть центр уведомлений" : "Закрыть ленту");
        this.notificationTabs.setVisibility(this.notificationDrawer ? View.VISIBLE : View.GONE);
        this.feedContent.removeAllViews();
        if (!this.notificationDrawer) {
            showStories(this.feedContent);
            showMyWall(this.feedContent);
            return;
        }
        this.alertsChoice.setSelected(!this.eventsSelected);
        this.alertsChoice.setTextColor(this.eventsSelected ? ink() : onAccent());
        this.eventsChoice.setSelected(this.eventsSelected);
        this.eventsChoice.setTextColor(this.eventsSelected ? onAccent() : ink());
        newCard(this.feedContent).addView(text(this.eventsSelected ? "Событий пока нет." : "Уведомлений пока нет.", 15, muted(), false));
    }

    private void refreshWall() {
        updateFeedAvatar();
        if (!this.feedOpen || this.notificationDrawer) {
            if (this.page == 6) {
                render();
                return;
            }
            return;
        }
        renderFeedDrawer();
    }

    // ---- Истории (кружки в стиле Instagram/Telegram под шапкой ленты) ----

    private static final int STORY_PICKER = 45;
    private static final long STORY_LIFETIME = 24L * 60 * 60 * 1000;
    private static final int[] STORY_RING = {0xFFFEDA75, 0xFFFA7E1E, 0xFFD62976, 0xFF962FBF, 0xFF4F5BD5, 0xFFFEDA75};

    private String storyKey() {
        return "story." + owner() + ".";
    }

    private boolean hasStory() {
        long time = this.preferences.getLong(storyKey() + "time", 0L);
        return !this.preferences.getString(storyKey() + "uri", "").isEmpty() && System.currentTimeMillis() - time < STORY_LIFETIME;
    }

    private void showStories(LinearLayout parent) {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(true);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setPadding(0, dp(2.0f), 0, dp(2.0f));
        scroller.addView(row, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollerParams = new LinearLayout.LayoutParams(-1, -2);
        scrollerParams.bottomMargin = dp(12.0f);
        parent.addView(scroller, scrollerParams);

        boolean mine = hasStory();
        FrameLayout myPhoto = outlinedAvatar();
        showProfilePhoto(myPhoto, wallProfileKey(), "♙", 22, ink());
        View me = storyBubble(myPhoto, "Вы", mine, !mine);
        me.setContentDescription(mine ? "Моя история" : "Добавить историю");
        me.setOnClickListener(v -> {
            if (hasStory()) {
                openStory();
            } else {
                pickStory();
            }
        });
        me.setOnLongClickListener(v -> {
            storyActions();
            return true;
        });
        row.addView(me);

        List<String> peers = this.store.chats(owner());
        for (int i = 0; i < peers.size() && i < 30; i++) {
            final String name = peers.get(i);
            View bubble = storyBubble(chatAvatar(name), name, false, false);
            bubble.setContentDescription("Открыть чат " + name);
            bubble.setOnClickListener(v -> {
                hideFeedDrawer();
                this.peer = name;
                this.page = 0;
                render();
            });
            row.addView(bubble);
        }
    }

    private View storyBubble(View avatar, String label, boolean active, boolean addBadge) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setPadding(dp(5.0f), 0, dp(5.0f), 0);
        item.setFocusable(true);
        item.setClickable(true);
        FrameLayout holder = new FrameLayout(this);
        FrameLayout ring = new FrameLayout(this);
        GradientDrawable ringShape;
        if (active) {
            ringShape = new GradientDrawable();
            ringShape.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            ringShape.setColors(STORY_RING);
        } else {
            ringShape = new GradientDrawable();
            ringShape.setColor(0);
            ringShape.setStroke(dp(1.5f), surfaceOutline());
        }
        ringShape.setShape(GradientDrawable.OVAL);
        ring.setBackground(ringShape);
        int gap = dp(active ? 2.5f : 3.0f);
        ring.setPadding(gap, gap, gap, gap);
        FrameLayout inner = new FrameLayout(this);
        GradientDrawable innerShape = new GradientDrawable();
        innerShape.setShape(GradientDrawable.OVAL);
        innerShape.setColor(this.dark ? 0xFF191A1C : 0xFFF4F4F4);
        inner.setBackground(innerShape);
        int innerGap = dp(active ? 2.5f : 0.0f);
        inner.setPadding(innerGap, innerGap, innerGap, innerGap);
        avatar.setClipToOutline(true);
        GradientDrawable avatarShape = new GradientDrawable();
        avatarShape.setShape(GradientDrawable.OVAL);
        avatarShape.setColor(this.dark ? 0xFF2A2C2F : 0xFFE6EAED);
        avatar.setBackground(avatarShape);
        inner.addView(avatar, new FrameLayout.LayoutParams(-1, -1));
        ring.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        holder.addView(ring, new FrameLayout.LayoutParams(dp(64.0f), dp(64.0f)));
        if (addBadge) {
            TextView plus = text("+", 14, onAccent(), true);
            plus.setGravity(Gravity.CENTER);
            plus.setIncludeFontPadding(false);
            GradientDrawable badge = accentPill(11);
            badge.setStroke(dp(2.0f), this.dark ? 0xFF191A1C : 0xFFF4F4F4);
            plus.setBackground(badge);
            holder.addView(plus, new FrameLayout.LayoutParams(dp(22.0f), dp(22.0f), Gravity.END | Gravity.BOTTOM));
        }
        item.addView(holder, new LinearLayout.LayoutParams(dp(64.0f), dp(64.0f)));
        TextView name = text(label, 11, active ? ink() : muted(), active);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(dp(68.0f), -2);
        nameParams.topMargin = dp(4.0f);
        item.addView(name, nameParams);
        return item;
    }

    private void pickStory() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
        try {
            startActivityForResult(picker, STORY_PICKER);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Выбор фото недоступен", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveStory(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
        this.preferences.edit().putString(storyKey() + "uri", uri.toString()).putLong(storyKey() + "time", System.currentTimeMillis()).apply();
        Toast.makeText(this, "История опубликована на 24 часа", Toast.LENGTH_SHORT).show();
        if (this.feedOpen && !this.notificationDrawer) {
            renderFeedDrawer();
        }
    }

    private void storyActions() {
        boolean mine = hasStory();
        String[] items = mine ? new String[]{"Смотреть", "Новая история", "Удалить историю"} : new String[]{"Добавить историю"};
        showDialog(new AlertDialog.Builder(this).setTitle("Моя история").setItems(items, (dialog, which) -> {
            if (!mine || which == 1) {
                pickStory();
            } else if (which == 0) {
                openStory();
            } else {
                this.preferences.edit().remove(storyKey() + "uri").remove(storyKey() + "time").apply();
                renderFeedDrawer();
            }
        }).setNegativeButton("Отмена", null).create());
    }

    private void openStory() {
        Uri uri = Uri.parse(this.preferences.getString(storyKey() + "uri", ""));
        android.app.Dialog viewer = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(Color.BLACK);
        ImageView picture = new ImageView(this);
        picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            picture.setImageURI(uri);
        } catch (SecurityException e) {
            Toast.makeText(this, "Нет доступа к фото истории", Toast.LENGTH_SHORT).show();
            return;
        }
        if (picture.getDrawable() == null) {
            Toast.makeText(this, "Фото истории недоступно", Toast.LENGTH_SHORT).show();
            return;
        }
        screen.addView(picture, new FrameLayout.LayoutParams(-1, -1));
        View track = new View(this);
        track.setBackgroundColor(0x55FFFFFF);
        FrameLayout.LayoutParams trackParams = new FrameLayout.LayoutParams(-1, dp(3.0f), Gravity.TOP);
        trackParams.setMargins(dp(10.0f), dp(14.0f), dp(10.0f), 0);
        screen.addView(track, trackParams);
        View progress = new View(this);
        progress.setBackgroundColor(Color.WHITE);
        progress.setPivotX(0.0f);
        progress.setScaleX(0.0f);
        screen.addView(progress, trackParams);
        long age = System.currentTimeMillis() - this.preferences.getLong(storyKey() + "time", 0L);
        TextView caption = text("Вы · " + DateUtils.getRelativeTimeSpanString(System.currentTimeMillis() - age, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS), 14, Color.WHITE, true);
        FrameLayout.LayoutParams captionParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        captionParams.setMargins(dp(14.0f), dp(26.0f), 0, 0);
        screen.addView(caption, captionParams);
        screen.setOnClickListener(v -> viewer.dismiss());
        viewer.setContentView(screen);
        viewer.show();
        progress.animate().scaleX(1.0f).setDuration(5000L).setInterpolator(new android.view.animation.LinearInterpolator()).withEndAction(() -> {
            if (viewer.isShowing()) {
                viewer.dismiss();
            }
        }).start();
    }

    private String wallProfileKey() {
        return "wall." + owner() + ".";
    }

    private void buildFeedSectionTabs(LinearLayout root) {
        this.feedSectionViewport = new HorizontalScrollView(this);
        this.feedSectionViewport.setHorizontalScrollBarEnabled(false);
        this.feedSectionViewport.setFillViewport(true);
        this.feedSectionViewport.setBackground(surface(this.dark ? 0xFF242628 : 0xFFE9EDF0, 24, surfaceOutline()));
        this.feedSectionViewport.setClipToOutline(true);
        this.feedSectionTabs = new SlidingTabs(accentPill(22));
        this.feedSectionViewport.addView(this.feedSectionTabs, new FrameLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(8.0f), dp(4.0f), dp(8.0f), dp(6.0f));
        root.addView(this.feedSectionViewport, params);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(3.0f), dp(3.0f), dp(3.0f), dp(3.0f));
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        this.feedSectionTabs.addView(row, new FrameLayout.LayoutParams(-1, -2));
        for (int i = 0; i < FEED_SECTIONS.length; i++) {
            final int index = i;
            ImageView choice = new ImageView(this);
            choice.setImageResource(FEED_SECTION_ICONS[i]);
            choice.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            choice.setPadding(dp(9.0f), dp(10.0f), dp(9.0f), dp(10.0f));
            choice.setContentDescription(FEED_SECTIONS[i]);
            choice.setTooltipText(FEED_SECTIONS[i]);
            choice.setFocusable(true);
            choice.setOnClickListener(v -> selectFeedSection(index));
            row.addView(choice, new LinearLayout.LayoutParams(dp(44.0f), dp(46.0f)));
            this.feedSectionChoices[i] = choice;
        }
        row.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (this.feedSectionTabs.isShown() && right - left != oldRight - oldLeft) {
                this.feedSectionTabs.moveTo(this.feedSectionChoices[activeFeedSection()], false);
            }

        });
    }

    private int activeFeedSection() {
        if (this.page == 6) {
            return 0;
        }
        int tab = activeChatsTab();
        for (int i = 1; i < FEED_SECTION_TABS.length; i++) {
            if (FEED_SECTION_TABS[i] == tab) {
                return i;
            }
        }
        return 1;
    }

    private void selectFeedSection(int index) {
        if (index < 0 || index >= FEED_SECTIONS.length) {
            return;
        }
        if (index >= 1) {
            selectAllChatsTab(FEED_SECTION_TABS[index]);
        } else if (this.page != 6 || this.feedSection != index) {
            this.feedSection = index;
            if (this.page != 6) {
                selectAllChatsTab(5);
            } else {
                render();
            }
        }
    }

    private void renderFeedSectionTabs() {
        boolean visible = this.peer == null && isMainPage();
        boolean wasVisible = this.feedSectionViewport.getVisibility() == View.VISIBLE;
        this.feedSectionViewport.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            final int active = activeFeedSection();
            int i = 0;
            while (i < this.feedSectionChoices.length) {
                ImageView choice = this.feedSectionChoices[i];
                choice.setSelected(i == active);
                choice.setImageTintList(ColorStateList.valueOf(i == active ? onAccent() : ink()));
                i++;
            }
            final View selected = this.feedSectionChoices[active];
            this.feedSectionTabs.moveTo(selected, wasVisible);
            this.feedSectionViewport.post(() -> {
                if (!this.feedSectionViewport.isShown() || activeFeedSection() != active) {
                    return;
                }
                int left = this.feedSectionViewport.getScrollX();
                int width = this.feedSectionViewport.getWidth();
                if (selected.getLeft() < left || selected.getRight() > left + width) {
                    this.feedSectionViewport.smoothScrollTo(selected.getLeft() - ((width - selected.getWidth()) / 2), 0);
                }

            });
        }
    }

    private void showFeedPage() {
        LinearLayout banner = showMyWall(this.content);
        LinearLayout sections = new LinearLayout(this);
        LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(-1, -2);
        row.topMargin = dp(12.0f);
        banner.addView(sections, row);
        for (int i = 0; i < PROFILE_SECTIONS.length; i++) {
            final int section = i;
            boolean selected = this.feedSection == section;
            TextView choice = allChatsChoice(sections, PROFILE_SECTIONS[i], 13, 0, () -> {
                if (this.feedSection == section) {
                    return;
                }
                this.feedSection = section;
                render();

            });
            choice.setSelected(selected);
            choice.setTextColor(selected ? onAccent() : ink());
            choice.setPadding(dp(4.0f), 0, dp(4.0f), 0);
            choice.setBackground(selected ? accentPill(20) : surface(0, 20, surfaceOutline()));
            if (i > 0) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) choice.getLayoutParams();
                params.setMarginStart(dp(6.0f));
            }
        }
        TextView empty = text(PROFILE_SECTION_EMPTY[this.feedSection], 13, muted(), false);
        LinearLayout.LayoutParams result = new LinearLayout.LayoutParams(-1, -2);
        result.topMargin = dp(10.0f);
        banner.addView(empty, result);
    }

    private LinearLayout showMyWall(LinearLayout parent) {
        final String key = wallProfileKey();
        LinearLayout newCard = newCard(parent);
        LinearLayout details = new LinearLayout(this);
        details.setGravity(Gravity.TOP);
        newCard.addView(details, new LinearLayout.LayoutParams(-1, -2));
        FrameLayout photo = new FrameLayout(this);
        photo.setBackground(accentPill(18));
        photo.setClipToOutline(true);
        showProfilePhoto(photo, key, "♙", 36, onAccent());
        photo.setContentDescription("Выбрать фото профиля");
        photo.setFocusable(true);
        photo.setOnClickListener(v -> pickProfilePhoto(key));
        details.addView(photo, new LinearLayout.LayoutParams(dp(96.0f), dp(116.0f)));
        LinearLayout fields = column();
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        fieldParams.setMarginStart(dp(14.0f));
        details.addView(fields, fieldParams);
        addProfileField(fields, "Имя пользователя", onlineAccount() ? "@" + user() : "");
        addProfileField(fields, "Почта", this.preferences.getString(key + "email", ""));
        ImageView edit = new ImageView(this);
        edit.setImageResource(R.drawable.ic_profile_edit);
        edit.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        edit.setImageTintList(ColorStateList.valueOf(ink()));
        edit.setPadding(dp(11.0f), dp(11.0f), dp(11.0f), dp(11.0f));
        edit.setBackground(new RippleDrawable(ColorStateList.valueOf(this.dark ? 0x33FFFFFF : 0x22000000), surface(0, 16, surfaceOutline()), null));
        edit.setContentDescription("Редактировать профиль");
        edit.setTooltipText("Редактировать профиль");
        edit.setFocusable(true);
        edit.setOnClickListener(v -> editWallProfile(key));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(44.0f), dp(44.0f));
        editParams.setMarginStart(dp(6.0f));
        details.addView(edit, editParams);
        return newCard;
    }

    /** Декодированные аватарки: раньше каждую заново распаковывали из base64 при каждой перерисовке списка. */
    private final android.util.LruCache<String, Bitmap> photoCache = new android.util.LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private Bitmap loadProfilePhoto(String key) {
        String encoded = this.preferences.getString(key + "photo", "");
        if (encoded.isEmpty()) {
            return null;
        }
        String cacheKey = key + "#" + encoded.length() + ":" + encoded.hashCode();
        Bitmap cached = this.photoCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] data = Base64.decode(encoded, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap != null) {
                this.photoCache.put(cacheKey, bitmap);
            }
            return bitmap;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showProfilePhoto(FrameLayout frame, String key, String fallback, int placeholderSize, int placeholderColor) {
        frame.removeAllViews();
        Bitmap avatar = loadProfilePhoto(key);
        if (avatar != null) {
            ImageView picture = new ImageView(this);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
            picture.setImageBitmap(avatar);
            frame.addView(picture, new FrameLayout.LayoutParams(-1, -1));
            return;
        }
        TextView placeholder = text(fallback, placeholderSize, placeholderColor, true);
        placeholder.setGravity(Gravity.CENTER);
        frame.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addProfileField(LinearLayout fields, String label, String value) {
        fields.addView(text(label, 11, muted(), true));
        TextView field = text(value.isEmpty() ? "—" : value, 14, ink(), false);
        field.setPadding(0, 0, 0, dp(6.0f));
        fields.addView(field);
    }

    private void editWallProfile(final String key) {
        LinearLayout form = column();
        form.setPadding(dp(20.0f), dp(8.0f), dp(20.0f), 0);
        final EditText name = field("ФИО", 97);
        final EditText email = field("Почта", 33);
        final EditText phone = field("Телефон", 3);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        email.setFilters(new InputFilter[]{new InputFilter.LengthFilter(254)});
        phone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        name.setText(this.preferences.getString(key + "name", ""));
        email.setText(this.preferences.getString(key + "email", ""));
        phone.setText(this.preferences.getString(key + "phone", ""));
        form.addView(name);
        form.addView(email);
        form.addView(phone);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Профиль").setView(form).setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).setPositiveButton("Сохранить", (DialogInterface.OnClickListener) null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            String address = email.getText().toString().trim();
            if (!address.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                email.setError("Проверьте адрес почты");
                return;
            }
            this.preferences.edit().putString(key + "name", name.getText().toString().trim()).putString(key + "email", address).putString(key + "phone", phone.getText().toString().trim()).apply();
            dialog.dismiss();
            refreshWall();

        }));
        showDialog(dialog);
    }

    private void pickProfilePhoto(String key) {
        Intent picker = new Intent("android.intent.action.OPEN_DOCUMENT").addCategory("android.intent.category.OPENABLE").setType("image/*");
        try {
            this.photoProfileKey = key;
            startActivityForResult(picker, 42);
        } catch (ActivityNotFoundException e) {
            this.photoProfileKey = null;
            Toast.makeText(this, "Выбор фото недоступен", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfilePhoto(final Uri uri, final String key) {
        this.worker.execute(() -> {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(input, null, options);
                }
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    throw new IOException("Invalid photo");
                }
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 384) {
                    options.inSampleSize *= 2;
                }
                try (InputStream input = getContentResolver().openInputStream(uri); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
                    if (bitmap == null) {
                        throw new IOException("Could not decode photo");
                    }
                    boolean saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output);
                    bitmap.recycle();
                    if (!saved) {
                        throw new IOException("Could not save photo");
                    }
                    this.preferences.edit().putString(key + "photo", Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)).apply();
                }
                this.handler.post(() -> {
                    if (isDestroyed()) {
                        return;
                    }
                    if (key.equals(wallProfileKey())) {
                        refreshWall();
                    } else if (this.page == 0 || this.page == 1) {
                        refreshChats();
                    }

                });
            } catch (IOException | SecurityException e) {
                this.handler.post(this::onProfilePhotoFailed);
            }

        });
    }

    public void onProfilePhotoFailed() {
        if (!isDestroyed()) {
            Toast.makeText(this, "Не удалось открыть фото", Toast.LENGTH_LONG).show();
        }
    }

    public void refreshScrollControls() {
        boolean chatList = this.peer == null && this.list != null && (this.page == 0 || this.page == 1);
        boolean hasChatRows = chatList && this.list.getChildCount() > 0 && (this.list.getChildAt(0).getTag() instanceof String);
        boolean canMoveUp = hasChatRows && this.scroll.canScrollVertically(-1);
        boolean canMoveDown = hasChatRows && this.scroll.canScrollVertically(1);
        this.scrollUpButton.setVisibility((this.page == 0 && canMoveUp) ? View.VISIBLE : View.GONE);
        this.scrollDownButton.setVisibility((this.page == 0 && canMoveDown) ? View.VISIBLE : View.GONE);
        if (this.alphabetRail == null) {
            return;
        }
        boolean showRail = chatList && this.list.getHeight() > this.scroll.getHeight() - this.content.getPaddingTop() && this.scroll.getHeight() > 0;
        this.alphabetRail.setVisibility(showRail ? View.VISIBLE : View.GONE);
        int rightPadding = dp(showRail ? 40.0f : 16.0f);
        if (this.content.getPaddingRight() != rightPadding) {
            this.content.setPadding(this.content.getPaddingLeft(), this.content.getPaddingTop(), rightPadding, this.content.getPaddingBottom());
        }
    }

    private void setSectionArchive(boolean open) {
        this.sectionArchive = open;
        render();
    }

    private void showCommunities() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setBaselineAligned(false);
        linearLayout.setMinimumHeight(dp(180.0f));
        this.content.addView(linearLayout, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout rail = column();
        rail.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        rail.setPadding(0, dp(8.0f), 0, dp(8.0f));
        rail.setBackground(surface(this.dark ? 0xFF222426 : 0xFFE6EAEE, 18, surfaceOutline()));
        LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(dp(54.0f), -1);
        railParams.setMarginEnd(dp(10.0f));
        linearLayout.addView(rail, railParams);
        ImageView all = new ImageView(this);
        all.setImageResource(R.drawable.ic_community_rail);
        all.setPadding(dp(9.0f), dp(9.0f), dp(9.0f), dp(9.0f));
        all.setBackground(this.sectionArchive ? card() : accentPill(14));
        all.setImageTintList(ColorStateList.valueOf(this.sectionArchive ? ink() : onAccent()));
        all.setContentDescription("Все сообщества");
        all.setTooltipText("Все сообщества");
        all.setSelected(!this.sectionArchive);
        all.setFocusable(true);
        all.setOnClickListener(v -> {
            if (this.sectionArchive) {
                setSectionArchive(false);
            }

        });
        rail.addView(all, new LinearLayout.LayoutParams(dp(42.0f), dp(42.0f)));
        LinearLayout pane = column();
        linearLayout.addView(pane, new LinearLayout.LayoutParams(0, -2, 1.0f));
        addArchiveRow(pane, this.sectionArchive, "К сообществам", () -> setSectionArchive(!this.sectionArchive));
        newCard(pane).addView(text(this.sectionArchive ? "В архиве пока нет сообществ." : "Сообществ пока нет.", 15, muted(), false));
    }

    private void addArchiveRow(boolean open, String backLabel, Runnable action) {
        addArchiveRow(this.content, open, backLabel, action);
    }

    private void addArchiveRow(LinearLayout parent, boolean open, String backLabel, final Runnable action) {
        Button archive = button(open ? "‹  " + backLabel : "Архив", false);
        archive.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        archive.setPadding(dp(16.0f), 0, dp(16.0f), 0);
        archive.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48.0f));
        params.bottomMargin = dp(8.0f);
        parent.addView(archive, params);
    }

    private void showSectionWithArchive(String backLabel, String empty, String archiveEmpty) {
        addArchiveRow(this.sectionArchive, backLabel, () -> setSectionArchive(!this.sectionArchive));
        newCard().addView(text(this.sectionArchive ? archiveEmpty : empty, 15, muted(), false));
    }

    private void showChats() {
        if (this.page == 0) {
            final boolean archived = this.chatFilter == 2;
            addArchiveRow(archived, "К чатам", () -> selectHeaderFilter(archived ? 0 : 2));
        }
        this.list = column();
        this.content.addView(this.list, new LinearLayout.LayoutParams(-1, -2));
        this.list.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> refreshScrollControls());
        refreshChats();
    }

    private void refreshChats() {
        int i;
        boolean z;
        String formatted;
        String message;
        if (this.list == null || this.peer != null || this.page == 2) {
            return;
        }
        this.list.removeAllViews();
        boolean z2 = true;
        int filter = this.page == 1 ? 0 : this.chatFilter;
        List<String> names = this.page == 1 ? this.store.chats(owner()) : this.store.chats(owner(), user(), filter);
        names.sort(Comparator.<String>comparingInt(AlphabetRail::bucket).thenComparing(String.CASE_INSENSITIVE_ORDER));
        float f = 8.0f;
        if (names.isEmpty()) {
            this.alphabetRail.bind(this.list, names);
            if (filter == 3) {
                message = "Непрочитанных сообщений пока нет.";
            } else if (filter == 1) {
                message = "Входящих сообщений пока нет.";
            } else if (filter == 2) {
                message = "В архиве пока нет чатов.";
            } else {
                message = this.page == 1 ? "Пока здесь пусто. Нажмите «Новый контакт»." : "Пока здесь пусто. Нажмите «Создать чат».";
            }
            TextView empty = text(message, 15, muted(), false);
            empty.setPadding(0, dp(14.0f), 0, dp(8.0f));
            this.list.addView(empty);
            return;
        }
        Map<String, ChatStore.Message> latest = this.page == 0 ? this.store.latestMessages(owner(), user()) : Collections.emptyMap();
        for (final String name : names) {
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setGravity(Gravity.CENTER_VERTICAL);
            linearLayout.setBackground(card());
            linearLayout.setClipToOutline(z2);
            ChatStore.Message preview = latest.get(name);
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setGravity(Gravity.CENTER_VERTICAL);
            linearLayout2.setPadding(dp(12.0f), 0, dp(f), 0);
            linearLayout2.setFocusable(z2);
            linearLayout2.setOnClickListener(v -> {
                this.peer = name;
                this.page = 0;
                render();

            });
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(36.0f), dp(36.0f));
            avatarParams.setMarginEnd(dp(10.0f));
            linearLayout2.addView(chatAvatar(name), avatarParams);
            LinearLayout column = column();
            column.setGravity(Gravity.CENTER_VERTICAL);
            linearLayout2.addView(column, new LinearLayout.LayoutParams(0, -1, 1.0f));
            LinearLayout linearLayout3 = new LinearLayout(this);
            linearLayout3.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = text(name, 16, ink(), preview != null);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            int filter2 = filter;
            linearLayout3.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));
            if (preview != null) {
                if (DateUtils.isToday(preview.time())) {
                    formatted = DateFormat.getTimeFormat(this).format(Long.valueOf(preview.time()));
                } else {
                    formatted = DateFormat.getDateFormat(this).format(Long.valueOf(preview.time()));
                }
                TextView text = text(formatted, 11, muted(), false);
                text.setPadding(dp(6.0f), 0, 0, 0);
                linearLayout3.addView(text);
            }
            column.addView(linearLayout3, new LinearLayout.LayoutParams(-1, -2));
            if (preview != null) {
                TextView excerpt = text(preview.text(), 13, muted(), false);
                excerpt.setSingleLine(true);
                excerpt.setEllipsize(TextUtils.TruncateAt.END);
                i = -1;
                column.addView(excerpt, new LinearLayout.LayoutParams(-1, -2));
            } else {
                i = -1;
            }
            linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, i, 1.0f));
            if (this.page == 0) {
                TextView read = chatRowAction("✓", 18);
                read.setContentDescription("Отметить прочитанным: " + name);
                read.setOnClickListener(v -> {
                    this.store.markRead(owner(), user(), name, this.store.cursor(owner()));
                    refreshChats();

                });
                linearLayout.addView(read, new LinearLayout.LayoutParams(dp(44.0f), -1));
            }
            TextView actions = chatRowAction("⋯", 20);
            actions.setContentDescription("Действия чата " + name);
            actions.setOnClickListener(v -> chatActions(name));
            linearLayout.addView(actions, new LinearLayout.LayoutParams(dp(48.0f), -1));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(preview == null ? 56.0f : 68.0f));
            params.bottomMargin = dp(8.0f);
            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.setTag(name);
            frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
            this.list.addView(frameLayout, params);
            if (this.page == 0 && name.equals(this.revealedChat)) {
                z = false;
                showArchiveAction(frameLayout, name, this.revealOnRight, false);
            } else {
                z = false;
            }
            filter = filter2;
            z2 = true;
            f = 8.0f;
        }
        this.alphabetRail.bind(this.list, names);
    }

    private void revealChatArchive(String name, boolean onRight) {
        if (this.list == null) {
            return;
        }
        String previous = this.revealedChat;
        this.revealedChat = (name.equals(previous) && this.revealOnRight == onRight) ? null : name;
        this.revealOnRight = onRight;
        for (int i = 0; i < this.list.getChildCount(); i++) {
            View childAt = this.list.getChildAt(i);
            if (childAt instanceof FrameLayout) {
                FrameLayout frame = (FrameLayout) childAt;
                if (name.equals(frame.getTag()) && this.revealedChat != null) {
                    showArchiveAction(frame, name, onRight, true);
                } else if (previous != null && previous.equals(frame.getTag())) {
                    if (frame.getChildCount() > 1) {
                        frame.removeViewAt(0);
                    }
                    frame.getChildAt(0).animate().translationX(0.0f).setDuration(140L).start();
                }
            }
        }
    }

    private void showArchiveAction(FrameLayout frame, final String name, boolean onRight, boolean animate) {
        StringBuilder sb;
        String str;
        if (frame.getChildCount() > 1) {
            frame.removeViewAt(0);
        }
        View row = frame.getChildAt(0);
        final boolean archived = this.store.isArchived(owner(), name);
        Button archive = button(archived ? "Вернуть" : "В архив", true);
        if (archived) {
            sb = new StringBuilder();
            str = "Вернуть из архива ";
        } else {
            sb = new StringBuilder();
            str = "В архив ";
        }
        archive.setContentDescription(sb.append(str).append(name).toString());
        archive.setMinWidth(0);
        archive.setMinimumWidth(0);
        archive.setOnClickListener(v -> {
            this.store.setArchived(owner(), name, !archived);
            this.revealedChat = null;
            refreshChats();

        });
        int width = dp(104.0f);
        frame.addView(archive, 0, new FrameLayout.LayoutParams(width, -1, onRight ? 5 : 3));
        int offset = onRight ? -width : width;
        if (!animate) {
            row.setTranslationX(offset);
        } else {
            row.animate().translationX(offset).setDuration(140L).start();
        }
    }

    private String chatAvatarKey(String name) {
        return "chat.avatar/" + Uri.encode(owner()) + "/" + Uri.encode(name) + "/";
    }

    private FrameLayout outlinedAvatar() {
        FrameLayout avatar = new FrameLayout(this);
        GradientDrawable surface = new GradientDrawable();
        surface.setShape(1);
        surface.setColor(this.dark ? 0xFF36393C : 0xFFDCE2E8);
        avatar.setBackground(surface);
        avatar.setClipToOutline(true);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(1);
        ring.setColor(0);
        ring.setStroke(dp(1.0f), (ink() & 0xFFFFFF) | (0xAC000000));
        avatar.setForeground(ring);
        avatar.setImportantForAccessibility(4);
        return avatar;
    }

    private FrameLayout chatAvatar(String name) {
        FrameLayout avatar = outlinedAvatar();
        String label = name.trim();
        String initial = label.isEmpty() ? "♙" : label.substring(0, label.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
        showProfilePhoto(avatar, chatAvatarKey(name), initial, 15, ink());
        return avatar;
    }

    private TextView chatRowAction(String label, int size) {
        TextView action = text(label, size, ink(), false);
        action.setGravity(Gravity.CENTER);
        action.setFocusable(true);
        action.setBackground(new RippleDrawable(ColorStateList.valueOf(this.dark ? 0x33FFFFFF : 0x26303030), null, new ColorDrawable(-1)));
        return action;
    }

    private void newChat() {
        newChat(null);
    }

    private void newChat(String selectedContact) {
        boolean contact = this.page == 1;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        LinearLayout panel = column();
        panel.setBackground(card());
        panel.setPadding(dp(20.0f), dp(12.0f), dp(20.0f), dp(20.0f));
        panel.setFocusableInTouchMode(true);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text(this.page == 1 ? "Новый контакт" : "Новый чат", 20, ink(), true), new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button close = button("×", false);
        close.setContentDescription("Закрыть");
        close.setMinWidth(0);
        close.setMinimumWidth(0);
        close.setPadding(0, 0, 0, 0);
        close.setOnClickListener(view -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(48.0f), dp(48.0f)));
        panel.addView(header);
        if (this.page == 1) {
            Button phonebook = button("Телефонная книга", false);
            panel.addView(phonebook, new LinearLayout.LayoutParams(-1, -2));
            phonebook.setOnClickListener(view -> MainActivity.this.pickFromPhonebook(dialog, view));
            if (selectedContact != null) {
                TextView picked = text(selectedContact, 14, ink(), true);
                picked.setPadding(0, dp(8.0f), 0, dp(4.0f));
                panel.addView(picked);
                panel.addView(text("Для чата введи имя пользователя Pulse.", 13, muted(), false));
            }
        }
        final EditText input = field("Имя пользователя (латиница, цифры, _)", 524289);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        panel.addView(input, new LinearLayout.LayoutParams(-1, -2));
        Button open = button("Открыть", true);
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(-1, -2);
        openParams.topMargin = dp(8.0f);
        panel.addView(open, openParams);
        open.setOnClickListener(view -> MainActivity.this.openTypedChat(input, dialog, view));
        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.setGravity(contact ? 80 : 17);
        window.setSoftInputMode(19);
        panel.requestFocus();
        dialog.show();
        int width = contact ? -1 : Math.min(getResources().getDisplayMetrics().widthPixels - dp(32.0f), dp(420.0f));
        window.setLayout(width, -2);
    }

    public void pickFromPhonebook(Dialog sheet, View v) {
        try {
            startActivityForResult(new Intent("android.intent.action.PICK", ContactsContract.CommonDataKinds.Phone.CONTENT_URI), 43);
            sheet.dismiss();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Телефонная книга недоступна", Toast.LENGTH_SHORT).show();
        }
    }

    public void openTypedChat(EditText input, Dialog sheet, View v) {
        String name = input.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_]{3,32}") || name.equals(user())) {
            input.setError("Введите другое имя: 3–32 символа");
            return;
        }
        this.store.createChat(owner(), name);
        this.store.setArchived(owner(), name, false);
        this.peer = name;
        this.page = 0;
        sheet.dismiss();
        render();
    }

    private void chatActions(final String name) {
        final String currentOwner = owner();
        final boolean archived = this.store.isArchived(currentOwner, name);
        final boolean blocked = this.store.isBlocked(currentOwner, name);
        String[] actions = new String[4];
        actions[0] = archived ? "Вернуть из архива" : "В архив";
        actions[1] = "Выбрать фото аватара";
        actions[2] = "Удалить";
        actions[3] = blocked ? "Разблокировать" : "Заблокировать";
        showDialog(new AlertDialog.Builder(this).setTitle(name).setItems(actions, (dialog, which) -> {
            switch (which) {
                case 0:
                    this.store.setArchived(currentOwner, name, !archived);
                    refreshChats();
                    break;
                case 1:
                    pickProfilePhoto(chatAvatarKey(name));
                    break;
                case 2:
                    confirmDeleteChat(name);
                    break;
                case 3:
                    this.store.setBlocked(currentOwner, name, !blocked);
                    Toast.makeText(this, blocked ? "Контакт разблокирован" : "Контакт заблокирован на этом устройстве", Toast.LENGTH_SHORT).show();
                    refreshChats();
                    break;
            }

        }).setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).create());
    }

    private void confirmDeleteChat(final String name) {
        final String currentOwner = owner();
        final String currentUser = user();
        showDialog(new AlertDialog.Builder(this).setTitle("Удалить чат?").setMessage("Переписка с " + name + " будет удалена с этого устройства.").setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).setPositiveButton("Удалить", (dialog, which) -> {
            this.store.deleteChat(currentOwner, currentUser, name);
            this.preferences.edit().remove("draft|" + currentOwner + "|" + name).remove(chatAvatarKey(name) + "photo").apply();
            refreshChats();

        }).create());
    }

    private void showConversation() {
        this.list = column();
        this.content.addView(this.list, new LinearLayout.LayoutParams(-1, -2));
        refreshMessages();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12.0f), dp(4.0f), dp(12.0f), dp(4.0f));
        this.compose.addView(row);
        this.messageInput = new EditText(this);
        this.messageInput.setHint("Сообщение");
        this.messageInput.setMinHeight(dp(48.0f));
        this.messageInput.setTextColor(headerInk());
        this.messageInput.setHintTextColor(muted());
        this.messageInput.setBackground(surface(0, 16, surfaceOutline()));
        this.messageInput.setPadding(dp(12.0f), dp(10.0f), dp(12.0f), dp(10.0f));
        this.messageInput.setMaxLines(4);
        this.messageInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        final String draftKey = "draft|" + owner() + "|" + this.peer;
        this.messageInput.setText(this.preferences.getString(draftKey, ""));
        this.messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.preferences.edit().putString(draftKey, s.toString()).apply();
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        inputParams.setMarginEnd(dp(6.0f));
        row.addView(this.messageInput, inputParams);
        this.voiceButton = button("●", false);
        this.voiceButton.setContentDescription("Записать голосовое");
        this.voiceButton.setMinWidth(0);
        this.voiceButton.setMinimumWidth(0);
        this.voiceButton.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams voiceParams = new LinearLayout.LayoutParams(dp(44.0f), dp(48.0f));
        voiceParams.setMarginEnd(dp(6.0f));
        row.addView(this.voiceButton, voiceParams);
        this.voiceButton.setOnClickListener(v -> {
            if (this.voiceRecorder != null) {
                finishVoiceRecording(true);
            } else {
                beginVoiceRecording();
            }

        });
        Button send = button("➤", true);
        send.setContentDescription("Отправить сообщение");
        row.addView(send, new LinearLayout.LayoutParams(dp(56.0f), dp(48.0f)));
        send.setOnClickListener(v -> {
            String value = this.messageInput.getText().toString().trim();
            if (value.isEmpty()) {
                return;
            }
            if (this.store.isBlocked(owner(), this.peer)) {
                this.messageInput.setError("Сначала разблокируйте контакт");
                return;
            }
            this.store.queue(owner(), user(), this.peer, value, !onlineAccount());
            this.messageInput.setText("");
            refreshMessages();
            this.scroll.post(() -> this.scroll.fullScroll(130));
            sync();

        });
        this.scroll.post(() -> this.scroll.fullScroll(130));
    }

    private void beginVoiceRecording() {
        if (this.peer == null || this.store.isBlocked(owner(), this.peer)) {
            return;
        }
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            this.requestedVoicePeer = this.peer;
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 45);
            return;
        }
        try {
            this.recordingFile = File.createTempFile("pulse-record-", ".m4a", getCacheDir());
            this.voiceRecorder = Build.VERSION.SDK_INT >= 31 ? new MediaRecorder(this) : new MediaRecorder();
            this.voiceRecorder.setAudioSource(1);
            this.voiceRecorder.setOutputFormat(2);
            this.voiceRecorder.setAudioEncoder(3);
            this.voiceRecorder.setAudioChannels(1);
            this.voiceRecorder.setAudioSamplingRate(16000);
            this.voiceRecorder.setAudioEncodingBitRate(24000);
            this.voiceRecorder.setMaxDuration(15000);
            this.voiceRecorder.setMaxFileSize(68000L);
            this.voiceRecorder.setOutputFile(this.recordingFile.getPath());
            this.voiceRecorder.setOnInfoListener((recorder, what, extra) -> {
                if (recorder == this.voiceRecorder) {
                    if (what == 800 || what == 801) {
                        finishVoiceRecording(true);
                    }
                }

            });
            this.voiceRecorder.prepare();
            this.voiceRecorder.start();
            this.voiceButton.setText("■");
            this.voiceButton.setContentDescription("Остановить запись и отправить");
            Toast.makeText(this, "Запись до 15 секунд", Toast.LENGTH_SHORT).show();
        } catch (IOException | RuntimeException e) {
            finishVoiceRecording(false);
            Toast.makeText(this, "Не удалось включить микрофон", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishVoiceRecording(boolean send) {
        MediaRecorder recorder = this.voiceRecorder;
        File file = this.recordingFile;
        this.voiceRecorder = null;
        this.recordingFile = null;
        if (recorder == null && file == null) {
            return;
        }
        boolean recorded = false;
        if (recorder != null) {
            try {
                recorder.stop();
                recorded = true;
            } catch (RuntimeException ignored) {
            } finally {
                recorder.release();
            }
        }
        if (this.voiceButton != null) {
            this.voiceButton.setText("●");
            this.voiceButton.setContentDescription("Записать голосовое");
        }
        if (!send) {
            if (file != null) {
                file.delete();
            }
            return;
        }
        try {
            if (!recorded || file == null || file.length() > 72000 || this.peer == null) {
                throw new IOException();
            }
            String audio = java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            if (!VoiceNote.valid(audio)) {
                throw new IOException();
            }
            this.store.queue(owner(), user(), this.peer, "Голосовое сообщение", !onlineAccount(), audio);
            refreshMessages();
            this.scroll.post(this::scrollToBottom);
            sync();
        } catch (IOException e) {
            Toast.makeText(this, "Голосовое сообщение не записалось", Toast.LENGTH_SHORT).show();
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public void scrollToBottom() {
        this.scroll.fullScroll(130);
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        boolean z = false;
        if (request == 46) {
            PulseCalls pulseCalls = this.calls;
            if (results.length > 0 && results[0] == 0) {
                z = true;
            }
            pulseCalls.microphoneResult(z);
            return;
        }
        if (request != 45) {
            return;
        }
        String target = this.requestedVoicePeer;
        this.requestedVoicePeer = null;
        if (results.length == 0 || results[0] != 0) {
            Toast.makeText(this, "Для записи нужен доступ к микрофону", Toast.LENGTH_SHORT).show();
        } else if (target != null && target.equals(this.peer) && this.voiceButton != null) {
            beginVoiceRecording();
        }
    }

    private void stopVoicePlayback() {
        if (this.voicePlayer != null) {
            this.voicePlayer.release();
            this.voicePlayer = null;
        }
        if (this.playingFile != null) {
            this.playingFile.delete();
            this.playingFile = null;
        }
    }

    private void playVoice(String audio) {
        stopVoicePlayback();
        try {
            this.playingFile = File.createTempFile("pulse-play-", ".m4a", getCacheDir());
            Files.write(this.playingFile.toPath(), java.util.Base64.getDecoder().decode(audio), new OpenOption[0]);
            this.voicePlayer = new MediaPlayer();
            this.voicePlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(1).build());
            this.voicePlayer.setDataSource(this.playingFile.getAbsolutePath());
            this.voicePlayer.setOnPreparedListener(player -> {
                if (player == this.voicePlayer) {
                    player.start();
                }

            });
            this.voicePlayer.setOnCompletionListener(player -> {
                if (player == this.voicePlayer) {
                    stopVoicePlayback();
                }

            });
            this.voicePlayer.setOnErrorListener((mediaPlayer, i, i2) -> {
                    boolean onVoicePlaybackError;
                    onVoicePlaybackError = MainActivity.this.onVoicePlaybackError(mediaPlayer, i, i2);
                    return onVoicePlaybackError;
            });
            this.voicePlayer.prepareAsync();
        } catch (IOException | RuntimeException e) {
            stopVoicePlayback();
            Toast.makeText(this, "Не удалось воспроизвести запись", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onVoicePlaybackError(MediaPlayer player, int what, int extra) {
        if (player == this.voicePlayer) {
            stopVoicePlayback();
            Toast.makeText(this, "Не удалось воспроизвести запись", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }

    private void refreshMessages() {
        if (this.list == null || this.peer == null) {
            return;
        }
        this.list.removeAllViews();
        List<ChatStore.Message> messages = this.store.messages(owner(), user(), this.peer);
        if (this.resumed && !this.feedOpen && !messages.isEmpty()) {
            long through = messages.stream().mapToLong(obj -> ((ChatStore.Message) obj).sequence()).max().orElse(0L);
            this.store.markRead(owner(), user(), this.peer, through);
        }
        if (messages.isEmpty()) {
            TextView empty = text("Сообщений пока нет", 14, muted(), false);
            empty.setPadding(0, dp(16.0f), 0, dp(8.0f));
            this.list.addView(empty);
        }
        for (final ChatStore.Message message : messages) {
            boolean mine = message.from().equals(user());
            LinearLayout bubble = column();
            bubble.setPadding(dp(12.0f), dp(9.0f), dp(12.0f), dp(9.0f));
            GradientDrawable outline = new GradientDrawable();
            outline.setColor(0);
            outline.setCornerRadius(dp(16.0f));
            int outlineColor = mine ? accent() : headerInk();
            outline.setStroke(dp(1.0f), (outlineColor & 0xFFFFFF) | (this.dark ? 0x65000000 : 0x50000000));
            bubble.setBackground(outline);
            if (message.audio().isEmpty()) {
                TextView body = text(message.text(), 16, headerInk(), false);
                body.setMaxWidth(Math.max(dp(120.0f), this.scroll.getWidth() > 0 ? Math.round(this.scroll.getWidth() * 0.76f) : dp(240.0f)));
                bubble.addView(body);
            } else {
                Button play = button("▶  Голосовое сообщение", false);
                play.setOnClickListener(v -> playVoice(message.audio()));
                bubble.addView(play);
            }
            String suffix = message.status() == 1 ? "В очереди" : message.status() == 2 ? "Не отправлено · нажмите для действий" : message.status() == 3 ? "Только на устройстве" : "";
            if (!suffix.isEmpty()) {
                TextView state = text(suffix, 11, muted(), false);
                state.setPadding(0, dp(4.0f), 0, 0);
                bubble.addView(state);
            }
            if (mine && message.status() == 2) {
                bubble.setClickable(true);
                bubble.setFocusable(true);
                bubble.setOnClickListener(v -> messageActions(message));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.gravity = mine ? Gravity.END : Gravity.START;
            params.topMargin = dp(8.0f);
            params.setMarginStart(mine ? dp(28.0f) : 0);
            params.setMarginEnd(mine ? 0 : dp(28.0f));
            this.list.addView(bubble, params);
        }
    }

    private void messageActions(final ChatStore.Message message) {
        final String currentOwner = owner();
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Сообщение").setItems(new String[]{"Повторить", "Удалить"}, (ignored, which) -> {
            if (which == 0) {
                this.store.retry(currentOwner, message);
                sync();
            } else {
                this.store.removeUnsent(currentOwner, message);
            }
            refreshMessages();

        }).setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).create();
        showDialog(dialog);
    }

    private SlidingTabs buildSettingsTabs(LinearLayout linearLayout) {
        final SlidingTabs tabs = new SlidingTabs(headerAccentPill());
        GradientDrawable track = new GradientDrawable();
        track.setColor(this.dark ? 0x26FFFFFF : 0x16000000);
        track.setCornerRadius(dp(28.0f));
        tabs.setBackground(track);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMarginEnd(dp(6.0f));
        linearLayout.addView(tabs, params);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(3.0f), dp(3.0f), dp(3.0f), dp(3.0f));
        tabs.addView(row, new FrameLayout.LayoutParams(-2, -2));
        final TextView[] choices = this.settingsChoices;
        for (int i = 0; i < choices.length; i++) {
            final int tab = i;
            TextView choice = text(SETTINGS_ICONS[i], 22, ink(), true);
            choice.setGravity(Gravity.CENTER);
            choice.setContentDescription(SETTINGS_TABS[i]);
            choice.setTooltipText(SETTINGS_TABS[i]);
            choice.setFocusable(true);
            choice.setOnClickListener(v -> selectSectionTab(tab));
            choices[i] = choice;
            row.addView(choice, new LinearLayout.LayoutParams(dp(44.0f), dp(44.0f)));
        }
        row.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
                tabs.moveTo(choices[this.settingsTab], false);
            }

        });
        return tabs;
    }

    private void buildClipsTabs(LinearLayout header) {
        this.clipsTabs = new SlidingTabs(headerAccentPill());
        GradientDrawable track = new GradientDrawable();
        track.setColor(this.dark ? 0x26FFFFFF : 0x16000000);
        track.setCornerRadius(dp(28.0f));
        this.clipsTabs.setBackground(track);
        header.addView(this.clipsTabs, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(3.0f), dp(3.0f), dp(3.0f), dp(3.0f));
        this.clipsTabs.addView(row, new FrameLayout.LayoutParams(-2, -2));
        clipsHeaderButton(row, "+", "Добавить клип", () -> MainActivity.this.pickClipVideo());
        this.clipsChoices[0] = clipsHeaderButton(row, "▦", "Все клипы", () -> selectClipsTab(false));
        this.clipsChoices[1] = clipsHeaderButton(row, "☆", "Рекомендации", () -> selectClipsTab(true));
        row.addOnLayoutChangeListener((view, i, i2, i3, i4, i5, i6, i7, i8) -> {
            if (i3 - i != i7 - i5) {
                this.clipsTabs.moveTo(this.clipsChoices[this.clipRecommendations ? 1 : 0], false);
            }

        });
    }

    private TextView clipsHeaderButton(LinearLayout row, String icon, String label, final Runnable action) {
        TextView button = text(icon, 22, headerInk(), true);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label);
        button.setTooltipText(label);
        button.setFocusable(true);
        button.setOnClickListener(view -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(dp(44.0f), dp(44.0f)));
        return button;
    }

    private void selectClipsTab(boolean z) {
        if (this.clipRecommendations == z) {
            return;
        }
        this.clipRecommendations = z;
        this.clipsTabs.moveTo(this.clipsChoices[z ? 1 : 0], true);
        render();
    }

    private String clipsKey() {
        return "clips." + owner();
    }

    private void showClips() {
        if (this.clipRecommendations) {
            newCard().addView(text("Рекомендаций пока нет.", 15, muted(), false));
            return;
        }
        TreeSet<String> clips = new TreeSet<>(this.preferences.getStringSet(clipsKey(), Collections.emptySet()));
        if (clips.isEmpty()) {
            newCard().addView(text("Клипов пока нет.", 15, muted(), false));
            return;
        }
        int number = 1;
        Iterator<String> it = clips.iterator();
        while (it.hasNext()) {
            final String clip = it.next();
            int number2 = number + 1;
            Button play = button("▶  Клип " + number, false);
            newCard().addView(play);
            play.setOnClickListener(v -> {
                try {
                    startActivity(new Intent("android.intent.action.VIEW").setDataAndType(Uri.parse(clip), "video/*").addFlags(1));
                } catch (ActivityNotFoundException | SecurityException e) {
                    Toast.makeText(this, "Не удалось открыть видео", Toast.LENGTH_SHORT).show();
                }

            });
            number = number2;
        }
    }

    public void pickClipVideo() {
        Intent picker = new Intent("android.intent.action.OPEN_DOCUMENT").addCategory("android.intent.category.OPENABLE").setType("video/*");
        try {
            startActivityForResult(picker, 44);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Выбор видео недоступен", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderSectionTabs() {
        updateSectionTabs(this.settingsTabs, this.settingsChoices, this.page == 2, this.settingsTab);
        updateSectionTabs(this.clipsTabs, this.clipsChoices, this.page == 5, this.clipRecommendations ? 1 : 0);
    }

    private void updateSectionTabs(SlidingTabs tabs, TextView[] choices, boolean visible, int selected) {
        boolean wasVisible = tabs.getVisibility() == View.VISIBLE;
        tabs.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            int i = 0;
            while (i < choices.length) {
                choices[i].setSelected(i == selected);
                choices[i].setTextColor(i == selected ? onHeaderAccent() : headerInk());
                i++;
            }
            if (!wasVisible) {
                tabs.moveTo(choices[selected], false);
            }
        }
    }

    private void selectSectionTab(int tab) {
        if (this.settingsTab == tab) {
            return;
        }
        this.settingsTab = tab;
        this.settingsTabs.moveTo(this.settingsChoices[tab], true);
        render();
    }

    private void showSettings() {
        switch (this.settingsTab) {
            case 1:
                showAccountSettings();
                break;
            case 2:
                showServerSettings();
                break;
            default:
                showAppSettings();
                break;
        }
    }

    private void showAppSettings() {
        LinearLayout card = newCard();
        Switch theme = new Switch(this);
        theme.setText("Тёмная тема");
        theme.setTextColor(ink());
        theme.setPadding(0, dp(12.0f), 0, dp(12.0f));
        theme.setChecked(this.dark);
        card.addView(theme);
        theme.setOnCheckedChangeListener((v, checked) -> {
            this.preferences.edit().putBoolean("dark", checked).apply();
            recreate();

        });
        this.updateText = text("", 14, muted(), false);
        this.updateText.setPadding(0, dp(8.0f), 0, dp(8.0f));
        card.addView(this.updateText);
        this.updateButton = button("Проверить обновления", false);
        card.addView(this.updateButton, new LinearLayout.LayoutParams(-1, -2));
        this.updateButton.setOnClickListener(v -> {
            if (this.updates.phase() == UpdateManager.Phase.READY) {
                requestInstall();
            } else {
                this.updates.check();
            }

        });
        refreshUpdateControls();
    }

    private void showAccountSettings() {
        LinearLayout card = newCard();
        if (onlineAccount()) {
            card.addView(text("Аккаунт: " + user(), 16, ink(), true));
            TextView host = text(this.preferences.getString("server", ""), 13, muted(), false);
            host.setPadding(0, dp(6.0f), 0, dp(10.0f));
            card.addView(host);
            Button local = button("Перейти в локальный режим", false);
            card.addView(local);
            local.setOnClickListener(v -> {
                this.authenticationGeneration++;
                String session = serverSessionKey(this.preferences.getString("server", ""));
                this.preferences.edit().remove(session + "user").remove(session + "token").remove("token").remove("user").remove("server").apply();
                this.connectionStatus = "";
                this.peer = null;
                render();

            });
            return;
        }
        TextView hint = text("Локальные чаты доступны без сервера. Для переписки укажите интернет-сервер или ссылку с телефона в той же сети Wi-Fi. Затем войдите или создайте аккаунт.", 14, muted(), false);
        hint.setPadding(0, dp(4.0f), 0, dp(8.0f));
        card.addView(hint);
        final EditText server = field("Адрес сервера или ссылка подключения", 17);
        server.setText(this.preferences.getString("serverDraft", ""));
        card.addView(server);
        final EditText username = field("Имя пользователя", 524289);
        username.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        card.addView(username);
        final EditText password = field("Пароль (минимум 10 символов)", 129);
        password.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});
        card.addView(password);
        final Button login = button("Войти", true);
        final Button register = button("Создать аккаунт", false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(8.0f);
        card.addView(login, p);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(-1, -2);
        p2.topMargin = dp(8.0f);
        card.addView(register, p2);
        this.authStatus = text("", 14, muted(), false);
        this.authStatus.setPadding(0, dp(8.0f), 0, 0);
        this.authStatus.setAccessibilityLiveRegion(1);
        card.addView(this.authStatus);
        updateStatus();
        login.setOnClickListener(v -> authenticate(server, username, password, false, login, register));
        register.setOnClickListener(v -> authenticate(server, username, password, true, login, register));
    }

    private void showServerSettings() {
        String active;
        final String cloud = this.preferences.getString("cloudServer", "");
        final String local = this.preferences.getString("localServer", "");
        String current = this.preferences.getString("server", "");
        LinearLayout overview = newCard();
        overview.addView(text("Текущее подключение", 19, ink(), true));
        if (current.isEmpty()) {
            active = "Локальный режим · без сервера";
        } else if (current.equals(cloud)) {
            active = "Pulse Cloud";
        } else {
            active = (current.equals(local) || current.equals(this.phoneHost.localUrl()) || current.contains("#sha256=")) ? "Локальный сервер" : "Свой сервер";
        }
        overview.addView(text(active, 15, muted(), false));
        LinearLayout cloudCard = newCard();
        cloudCard.addView(text("Pulse Cloud", 19, ink(), true));
        cloudCard.addView(text(cloud.isEmpty() ? "Укажите HTTPS-адрес сервера Oracle Cloud." : cloud, 14, muted(), false));
        Button cloudConnect = button(cloud.isEmpty() ? "Настроить Pulse Cloud" : "Выбрать Pulse Cloud", false);
        cloudCard.addView(cloudConnect, new LinearLayout.LayoutParams(-1, -2));
        cloudConnect.setOnClickListener(v -> {
            if (!cloud.isEmpty()) {
                selectServer(cloud);
            } else {
                editServerAddress(true);
            }

        });
        if (!cloud.isEmpty()) {
            Button edit = button("Изменить адрес Pulse Cloud", false);
            cloudCard.addView(edit, new LinearLayout.LayoutParams(-1, -2));
            edit.setOnClickListener(v -> editServerAddress(true));
        }
        LinearLayout localCard = newCard();
        localCard.addView(text("Локальный сервер", 19, ink(), true));
        localCard.addView(text(local.isEmpty() ? "Запустите сервер на телефоне или укажите ссылку другого устройства." : local, 14, muted(), false));
        Button localLink = button(local.isEmpty() ? "Указать ссылку" : "Выбрать локальный сервер", false);
        localCard.addView(localLink, new LinearLayout.LayoutParams(-1, -2));
        localLink.setOnClickListener(v -> {
            if (!local.isEmpty()) {
                selectServer(local);
            } else {
                editServerAddress(false);
            }

        });
        if (!local.isEmpty()) {
            Button edit2 = button("Изменить локальную ссылку", false);
            localCard.addView(edit2, new LinearLayout.LayoutParams(-1, -2));
            edit2.setOnClickListener(v -> editServerAddress(false));
        }
        showPhoneHost();
    }

    private void editServerAddress(final boolean cloud) {
        final String key = cloud ? "cloudServer" : "localServer";
        final EditText input = field(cloud ? "HTTPS-адрес" : "Ссылка подключения", 17);
        input.setText(this.preferences.getString(key, ""));
        LinearLayout form = column();
        form.setPadding(dp(20.0f), dp(8.0f), dp(20.0f), 0);
        form.addView(input);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle(cloud ? "Pulse Cloud" : "Локальный сервер").setView(form).setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).setPositiveButton("Подключиться", (DialogInterface.OnClickListener) null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            try {
                String address = PulseApi.normalizeUrl(input.getText().toString());
                if (cloud && (!address.startsWith("https://") || address.contains("#"))) {
                    throw new IllegalArgumentException("Expected public HTTPS URL");
                }
                this.preferences.edit().putString(key, address).apply();
                dialog.dismiss();
                selectServer(address);
            } catch (Exception e) {
                input.setError(cloud ? "Укажите HTTPS-адрес Pulse Cloud" : "Укажите полную ссылку подключения");
            }

        }));
        showDialog(dialog);
    }

    private String serverSessionKey(String address) {
        return "session." + address + ".";
    }

    private void selectServer(String address) {
        this.authenticationGeneration++;
        String current = this.preferences.getString("server", "");
        SharedPreferences.Editor change = this.preferences.edit().putString("serverDraft", address);
        if (!address.equals(current)) {
            if (onlineAccount()) {
                String oldSession = serverSessionKey(current);
                change.putString(oldSession + "user", user()).putString(oldSession + "token", this.preferences.getString("token", ""));
            }
            String session = serverSessionKey(address);
            String name = this.preferences.getString(session + "user", "");
            String token = this.preferences.getString(session + "token", "");
            if (name.isEmpty() || token.isEmpty()) {
                change.remove("server").remove("user").remove("token");
            } else {
                change.putString("server", address).putString("user", name).putString("token", token);
            }
        }
        change.apply();
        this.connectionStatus = "";
        this.peer = null;
        selectSectionTab(1);
        sync();
    }

    private void showPhoneHost() {
        LinearLayout card = newCard();
        card.addView(text("Сервер на этом телефоне", 19, ink(), true));
        TextView hint = text("Общий сервер для телефонов в одной сети Wi-Fi. Аккаунты и сообщения сервера хранятся здесь отдельно от локальных чатов.", 14, muted(), false);
        hint.setPadding(0, dp(8.0f), 0, dp(8.0f));
        card.addView(hint);
        this.hostText = text("", 13, muted(), false);
        this.hostText.setTextIsSelectable(true);
        card.addView(this.hostText);
        this.hostButton = button("Запустить сервер", false);
        card.addView(this.hostButton, new LinearLayout.LayoutParams(-1, -2));
        this.hostButton.setOnClickListener(v -> {
            if (this.phoneHost.phase() != PhoneHost.Phase.RUNNING) {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 74);
                }
                this.phoneHost.start();
                return;
            }
            this.phoneHost.stop();

        });
        this.useHostButton = button("Подключиться на этом телефоне", false);
        card.addView(this.useHostButton, new LinearLayout.LayoutParams(-1, -2));
        this.useHostButton.setOnClickListener(v -> {
            String address = this.phoneHost.localUrl();
            if (address.isEmpty()) {
                return;
            }
            this.preferences.edit().putString("localServer", address).apply();
            selectServer(address);

        });
        this.shareHostButton = button("Поделиться подключением", false);
        card.addView(this.shareHostButton, new LinearLayout.LayoutParams(-1, -2));
        this.shareHostButton.setOnClickListener(v -> {
            List<String> addresses = this.phoneHost.lanUrls();
            if (addresses.isEmpty()) {
                return;
            }
            final String[] options = (String[]) addresses.toArray(new String[0]);
            if (options.length == 1) {
                shareHost(options[0]);
            } else {
                showDialog(new AlertDialog.Builder(this).setTitle("Выберите адрес своей сети Wi-Fi").setItems(options, (dialog, which) -> shareHost(options[which])).setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).create());
            }

        });
        refreshHostControls();
    }

    private void shareHost(String address) {
        try {
            startActivity(Intent.createChooser(new Intent("android.intent.action.SEND").setType("text/plain").putExtra("android.intent.extra.TEXT", address), "Подключение к Pulse"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Скопируйте адрес сервера из настроек", Toast.LENGTH_LONG).show();
        }
    }

    public void refreshHostControls() {
        String description;
        if (this.hostText == null) {
            return;
        }
        PhoneHost.Phase phase = this.phoneHost.phase();
        boolean running = phase == PhoneHost.Phase.RUNNING;
        if (running) {
            List<String> addresses = this.phoneHost.lanUrls();
            description = "Сервер включён · HTTPS · порт 8443\n" + (addresses.isEmpty() ? "Подключите Wi-Fi для других телефонов." : String.join("\n\n", addresses)) + "\n\nНа другом телефоне вставьте полную ссылку в настройки Pulse. Для интернета нужен отдельный HTTPS-туннель. Сервер расходует заряд; Android может остановить его.";
        } else if (phase == PhoneHost.Phase.STARTING) {
            description = "Запускаем сервер…";
        } else if (phase == PhoneHost.Phase.STOPPING) {
            description = "Останавливаем сервер…";
        } else {
            description = phase == PhoneHost.Phase.ERROR ? "Сервер не прошёл проверку подключения. Проверьте порт 8443 и повторите запуск." : "Сервер выключен. Данные сохраняются после остановки.";
        }
        this.hostText.setText(description);
        this.hostButton.setText(running ? "Остановить сервер" : "Запустить сервер");
        this.hostButton.setEnabled((phase == PhoneHost.Phase.STARTING || phase == PhoneHost.Phase.STOPPING) ? false : true);
        this.useHostButton.setVisibility(running ? View.VISIBLE : View.GONE);
        this.shareHostButton.setVisibility(running ? View.VISIBLE : View.GONE);
        this.shareHostButton.setEnabled(running && !this.phoneHost.lanUrls().isEmpty());
    }

    private EditText field(String hint, int inputType) {
        EditText v = new EditText(this);
        v.setHint(hint);
        v.setTextColor(ink());
        v.setHintTextColor(muted());
        v.setSingleLine(true);
        v.setInputType(inputType);
        return v;
    }

    private void showDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.PulseDialogAnimation);
        }
        dialog.show();
    }

    private void refreshUpdateControls() {
        String description;
        if (this.updateText == null || this.updateButton == null) {
            return;
        }
        UpdateManager.Phase phase = this.updates.phase();
        switch (phase) {
            case CHECKING:
                description = "Проверяем обновления…";
                break;
            case DOWNLOADING:
                description = "Скачиваем обновление…";
                break;
            case CURRENT:
                description = "Установлена последняя версия";
                break;
            case UNPUBLISHED:
                description = "Обновления ещё не опубликованы";
                break;
            case READY:
                description = "Версия " + this.updates.update().name() + " готова к установке";
                break;
            case ERROR:
                description = "Не удалось проверить или скачать обновление";
                break;
            default:
                description = "Обновления";
                break;
        }
        this.updateText.setText(description);
        this.updateButton.setText(phase == UpdateManager.Phase.READY ? "Установить обновление" : "Проверить обновления");
        this.updateButton.setEnabled((phase == UpdateManager.Phase.CHECKING || phase == UpdateManager.Phase.DOWNLOADING) ? false : true);
    }

    public void onUpdateChanged() {
        refreshUpdateControls();
        updateHeader();
        if (this.updates.takeOffer()) {
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Обновление готово").setMessage("Версия " + this.updates.update().name() + " скачана и проверена. Подтвердите установку в Android, затем откройте приложение снова.").setNegativeButton("Позже", (DialogInterface.OnClickListener) null).setPositiveButton("Установить", (ignored, which) -> requestInstall()).create();
            showDialog(dialog);
        }
    }

    private void requestInstall() {
        if (!getPackageManager().canRequestPackageInstalls()) {
            try {
                this.awaitingInstallPermission = true;
                startActivityForResult(new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES", Uri.parse("package:" + getPackageName())), 41);
                return;
            } catch (ActivityNotFoundException | SecurityException e) {
                this.awaitingInstallPermission = false;
                Toast.makeText(this, "Не удалось открыть настройки установки", Toast.LENGTH_LONG).show();
                return;
            }
        }
        this.updates.verify(obj -> MainActivity.this.openInstaller((File) obj), () -> {
            if (isDestroyed()) {
                return;
            }
            Toast.makeText(this, "Не удалось проверить обновление", Toast.LENGTH_LONG).show();

        });
    }

    public void openInstaller(File file) {
        if (isDestroyed() || isFinishing()) {
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".updates", file);
            startActivity(new Intent("android.intent.action.VIEW").setDataAndType(uri, "application/vnd.android.package-archive").addFlags(1));
        } catch (ActivityNotFoundException | IllegalArgumentException | SecurityException e) {
            Toast.makeText(this, "Не удалось открыть установщик", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 43) {
            if (resultCode == -1 && data != null && data.getData() != null && this.page == 1) {
                showPickedPhoneContact(data.getData());
                return;
            }
            return;
        }
        if (requestCode == STORY_PICKER) {
            if (resultCode == -1 && data != null && data.getData() != null) {
                saveStory(data.getData());
            }
            return;
        }
        if (requestCode == 44) {
            if (resultCode == -1 && data != null && data.getData() != null) {
                Uri clip = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(clip, 1);
                    String key = clipsKey();
                    HashSet<String> clips = new HashSet<>(this.preferences.getStringSet(key, Collections.emptySet()));
                    clips.add(clip.toString());
                    this.preferences.edit().putStringSet(key, clips).apply();
                    if (this.page == 5) {
                        if (this.clipRecommendations) {
                            selectClipsTab(false);
                        } else {
                            render();
                        }
                    }
                    return;
                } catch (SecurityException e) {
                    Toast.makeText(this, "Не удалось сохранить доступ к видео", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            return;
        }
        if (requestCode == 42) {
            String key2 = this.photoProfileKey;
            this.photoProfileKey = null;
            if (resultCode == -1 && key2 != null && data != null && data.getData() != null) {
                saveProfilePhoto(data.getData(), key2);
                return;
            }
            return;
        }
        if (requestCode == 41 && this.awaitingInstallPermission) {
            this.awaitingInstallPermission = false;
            if (getPackageManager().canRequestPackageInstalls()) {
                requestInstall();
            }
        }
    }

    private void showPickedPhoneContact(Uri uri) {
        try (Cursor contact = getContentResolver().query(uri, new String[]{"display_name", "data1"}, null, null, null)) {
            if (contact != null && contact.moveToFirst()) {
                String name = contact.getString(0);
                String phone = contact.getString(1);
                if (phone != null && !phone.trim().isEmpty()) {
                    newChat(name != null && !name.trim().isEmpty() ? name + "\n" + phone : phone);
                    return;
                }
            }
        } catch (IllegalArgumentException | SecurityException ignored) {
        }
        Toast.makeText(this, "Не удалось прочитать выбранный контакт", Toast.LENGTH_SHORT).show();
        newChat();
    }

    private void authenticate(EditText server, EditText username, EditText password, final boolean register, final Button login, final Button signup) {
        try {
            final String base = PulseApi.normalizeUrl(server.getText().toString());
            this.preferences.edit().putString("serverDraft", base).apply();
            final String name = username.getText().toString().trim().toLowerCase(Locale.ROOT);
            final String secret = password.getText().toString();
            if (!PhoneServerStore.validName(name)) {
                username.setError("Выберите допустимое имя: 3–32 латинские буквы, цифры или _");
                return;
            }
            if (secret.length() < 10 || secret.length() > 128) {
                password.setError("Пароль: от 10 до 128 символов");
                return;
            }
            final int generation = this.authenticationGeneration + 1;
            this.authenticationGeneration = generation;
            login.setEnabled(false);
            signup.setEnabled(false);
            this.connectionStatus = "Подключение…";
            updateStatus();
            this.worker.execute(() -> {
                try {
                    JSONObject result = new PulseApi(base, null).request(register ? "/v1/register" : "/v1/login", new JSONObject().put("username", name).put("password", secret));
                    String returnedUser = result.getString("username");
                    final String token = result.getString("token");
                    if (!name.equals(returnedUser) || !token.matches("[a-f0-9]{64}")) {
                        throw new IllegalStateException("Некорректный ответ");
                    }
                    this.handler.post(() -> {
                        if (isFinishing() || isDestroyed() || generation != this.authenticationGeneration) {
                            return;
                        }
                        String session = serverSessionKey(base);
                        this.preferences.edit().putString("server", base).putString("user", name).putString("token", token).putString(session + "user", name).putString(session + "token", token).apply();
                        this.connectionStatus = "Подключено";
                        this.peer = null;
                        this.page = 0;
                        render();
                        sync();

                    });
                } catch (Exception error) {
                    this.handler.post(() -> {
                        if (isFinishing() || isDestroyed() || generation != this.authenticationGeneration) {
                            return;
                        }
                        login.setEnabled(true);
                        signup.setEnabled(true);
                        this.connectionStatus = authenticationError(error, register);
                        updateStatus();

                    });
                }

            });
        } catch (Exception e) {
            server.setError("Укажите HTTPS-адрес или полную ссылку подключения");
        }
    }

    private static String authenticationError(Exception error, boolean register) {
        if (error instanceof PulseApi.ApiError) {
            int code = ((PulseApi.ApiError) error).code;
            switch (code) {
                case 400:
                    return "Сервер отклонил имя или пароль";
                case 401:
                    return register ? "Сервер отклонил регистрацию" : "Неверное имя пользователя или пароль";
                case 404:
                    return "По этому адресу не найден API Pulse";
                case 409:
                    return register ? "Имя уже занято. Войдите или выберите другое имя" : "Конфликт при входе";
                case 429:
                    return "Слишком много попыток. Повторите через минуту";
                default:
                    return "Ошибка сервера (HTTP " + code + ")";
            }
        }
        if (error instanceof SSLException) {
            return "Ошибка HTTPS. Проверьте сертификат или скопируйте полную ссылку подключения";
        }
        if (error instanceof SocketTimeoutException) {
            return "Сервер не ответил вовремя";
        }
        if ((error instanceof JSONException) || (error instanceof IllegalStateException)) {
            return "Некорректный ответ API Pulse";
        }
        return "Нет соединения. Проверьте адрес, порт и сеть";
    }

    private View buildFeedTab(LinearLayout linearLayout) {
        LinearLayout tab = new LinearLayout(this);
        tab.setGravity(Gravity.CENTER);
        tab.setFocusable(true);
        tab.setContentDescription("Фид");
        tab.setOnClickListener(v -> {
            if (this.page != 6) {
                this.feedSection = 0;
                selectAllChatsTab(5);
            } else {
                selectFeedSection(0);
            }

        });
        linearLayout.addView(tab, new LinearLayout.LayoutParams(dp(94.0f), dp(44.0f)));
        FrameLayout avatar = new FrameLayout(this);
        avatar.setImportantForAccessibility(4);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(29.0f), dp(29.0f));
        avatarParams.setMarginEnd(dp(5.0f));
        tab.addView(avatar, avatarParams);
        this.feedAvatarPhoto = outlinedAvatar();
        avatar.addView(this.feedAvatarPhoto, new FrameLayout.LayoutParams(dp(25.0f), dp(25.0f), Gravity.CENTER));
        View dot = new View(this);
        this.feedOnlineDot = new GradientDrawable();
        this.feedOnlineDot.setShape(1);
        this.feedOnlineDot.setStroke(dp(1.0f), this.dark ? 0xFF222426 : 0xFFE8EAEC);
        dot.setBackground(this.feedOnlineDot);
        avatar.addView(dot, new FrameLayout.LayoutParams(dp(9.0f), dp(9.0f), Gravity.END | Gravity.BOTTOM));
        this.feedTabLabel = text("Фид", 13, ink(), true);
        this.feedTabLabel.setImportantForAccessibility(2);
        tab.addView(this.feedTabLabel);
        return tab;
    }

    private void updateFeedAvatar() {
        MainActivity mainActivity;
        if (this.feedAvatarPhoto == null) {
            return;
        }
        String key = wallProfileKey();
        String photo = this.preferences.getString(key + "photo", "");
        if (key.equals(this.displayedAvatarKey) && photo.equals(this.displayedAvatarPhoto)) {
            mainActivity = this;
        } else {
            this.displayedAvatarKey = key;
            this.displayedAvatarPhoto = photo;
            String username = user();
            String initial = (!onlineAccount() || username.isEmpty()) ? "♙" : username.substring(0, username.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
            mainActivity = this;
            mainActivity.showProfilePhoto(this.feedAvatarPhoto, key, initial, 13, headerInk());
        }
        boolean connected = onlineAccount() && "Подключено".equals(mainActivity.connectionStatus);
        mainActivity.feedOnlineDot.setColor(connected ? mainActivity.dark ? 0xFF65D49A : 0xFF237C4A : 0xFF757C85);
        if (Build.VERSION.SDK_INT >= 30) {
            mainActivity.feedShortcut.setStateDescription(connected ? "Онлайн" : "Офлайн");
        }
    }

    private void updateStatus() {
        String str;
        updateFeedAvatar();
        this.status.setVisibility(this.peer != null ? View.VISIBLE : View.GONE);
        if (this.peer != null) {
            TextView textView = this.status;
            if (this.connectionStatus.isEmpty()) {
                str = onlineAccount() ? "Ожидание сети" : "Локальный режим";
            } else {
                str = this.connectionStatus;
            }
            textView.setText(str);
        }
        if (this.authStatus != null) {
            this.authStatus.setText(this.connectionStatus);
            this.authStatus.setVisibility(this.connectionStatus.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private TextView headerControl(String glyph) {
        TextView control = text(glyph, 24, headerInk(), true);
        control.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setShape(1);
        background.setColor(this.dark ? 0x26FFFFFF : 0x55FFFFFF);
        control.setBackground(background);
        control.setClickable(true);
        control.setFocusable(true);
        return control;
    }

    private ImageView addCallButton(LinearLayout row, int icon, final String label) {
        ImageView button = new ImageView(this);
        button.setImageResource(icon);
        button.setImageTintList(ColorStateList.valueOf(headerInk()));
        button.setPadding(dp(9.0f), dp(9.0f), dp(9.0f), dp(9.0f));
        button.setContentDescription(label);
        button.setTooltipText(label);
        button.setFocusable(true);
        button.setOnClickListener(v -> showCallUnavailable(label));
        row.addView(button, new LinearLayout.LayoutParams(dp(38.0f), dp(40.0f)));
        return button;
    }

    private void showCallUnavailable(String label) {
        showDialog(new AlertDialog.Builder(this).setTitle(label).setMessage("Звонки пока недоступны.").setPositiveButton("Закрыть", (DialogInterface.OnClickListener) null).create());
    }

    private void updateHeader() {
        if (this.headerTitle == null || this.updates == null) {
            return;
        }
        boolean chatHeader = this.peer == null && isMainPage();
        this.headerTitle.setVisibility(this.peer != null ? View.VISIBLE : View.GONE);
        this.notificationsButton.setVisibility(chatHeader ? View.VISIBLE : View.GONE);
        this.headerBack.setVisibility(this.peer != null ? View.VISIBLE : View.GONE);
        this.headerAction.setVisibility(this.peer != null ? View.VISIBLE : View.GONE);
        this.audioCallButton.setVisibility(this.peer != null ? View.VISIBLE : View.GONE);
        this.videoCallButton.setVisibility(this.peer == null ? View.GONE : View.VISIBLE);
        if (this.peer != null) {
            setTitle(this.peer);
            this.headerTitle.setText(this.peer);
            this.headerBack.setContentDescription("Назад к чатам");
            this.headerBack.setOnClickListener(v -> {
                this.peer = null;
                this.page = 0;
                render();

            });
            this.headerAction.setText("↻");
            this.headerAction.setContentDescription("Обновить сообщения");
            this.headerAction.setEnabled(onlineAccount());
            this.headerAction.setAlpha(onlineAccount() ? 1.0f : 0.45f);
            this.headerAction.setOnClickListener(v -> sync());
            return;
        }
        if (this.page == 2) {
            setTitle("Настройки");
            return;
        }
        if (this.page == 4) {
            setTitle("Сообщества");
            return;
        }
        if (this.page == 5) {
            setTitle("Клипы");
            return;
        }
        if (this.page == 7) {
            setTitle("Звонки");
            return;
        }
        if (this.page == 6) {
            setTitle("Фид");
        } else if (this.page == 1) {
            setTitle("Контакты");
        } else {
            setTitle("Чаты");
        }
    }

    public void sync() {
        if (!onlineAccount() || !this.syncing.compareAndSet(false, true)) {
            return;
        }
        final String currentOwner = owner();
        final String currentUser = user();
        final String token = this.preferences.getString("token", "");
        final String base = this.preferences.getString("server", "");
        this.worker.execute(() -> {
            String result = "Подключено";
            boolean changed = false;
            try {
                PulseApi api = new PulseApi(base, token);
                for (ChatStore.Message message : this.store.pending(currentOwner)) {
                    if (this.store.isBlocked(currentOwner, message.to())) {
                        continue;
                    }
                    try {
                        this.store.confirmPendingDelivery(currentOwner, currentUser, api.send(message));
                    } catch (PulseApi.ApiError e) {
                        if (e.code != 400 && e.code != 404 && e.code != 409 && e.code != 413) {
                            throw e;
                        }
                        this.store.markFailed(currentOwner, message);
                        result = "Есть неотправленные сообщения";
                    }
                    changed = true;
                }
                for (int batch = 0; batch < 20; batch++) {
                    long before = this.store.cursor(currentOwner);
                    List<ChatStore.Message> incoming = api.fetch(before);
                    if (incoming.isEmpty()) {
                        break;
                    }
                    this.store.received(currentOwner, currentUser, incoming);
                    changed = true;
                    if (this.store.cursor(currentOwner) <= before) {
                        break;
                    }
                }
            } catch (PulseApi.ApiError e) {
                result = e.code == 401 ? "Войдите снова" : "Нет соединения с сервером";
            } catch (Exception e) {
                result = "Нет соединения с сервером";
            } finally {
                this.syncing.set(false);
            }
            final String finalResult = result;
            final boolean finalChanged = changed;
            this.handler.post(() -> applySyncResult(currentOwner, finalResult, finalChanged));

        });
    }

    /** Фоновая синхронизация: отправляет очередь и забирает новые сообщения. UI обновляется, только если что-то изменилось. */

    public void applySyncResult(String currentOwner, String finalResult, boolean changed) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (this.waitingForRefresh) {
            this.waitingForRefresh = false;
            if (currentOwner.equals(owner()) && (isMainPage() || this.peer != null)) {
                showPullRefreshHint("Подключено".equals(finalResult) ? "Обновлено" : finalResult, true);
            } else {
                this.pullRefreshHint.setVisibility(View.GONE);
            }
        }
        if (!currentOwner.equals(owner())) {
            return;
        }
        boolean statusChanged = !finalResult.equals(this.connectionStatus);
        this.connectionStatus = finalResult;
        updateStatus();
        // Раньше список перерисовывался каждые 6 секунд: сбрасывались свайпы и прыгала прокрутка.
        if (changed || statusChanged) {
            if (this.peer == null) {
                refreshChats();
            } else {
                refreshMessages();
            }
        }
    }
}
