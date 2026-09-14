package pl.szynolandia.szybkaklawiatura;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FastKeyboardService extends InputMethodService {
    public static final String PROFILE_WORK = "WORK";
    public static final String PROFILE_NORMAL = "NORMAL";

    private static final String[][] ROWS = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"z","c","b","n","m","⌫"},
            {"PROFIL",","," ",".","WKLEJ","⏎"}
    };

    private static final Map<String, String[]> LONG_PRESS = new HashMap<>();
    static {
        LONG_PRESS.put("a", new String[]{"ą"});
        LONG_PRESS.put("c", new String[]{"ć"});
        LONG_PRESS.put("e", new String[]{"ę"});
        LONG_PRESS.put("l", new String[]{"ł"});
        LONG_PRESS.put("n", new String[]{"ń"});
        LONG_PRESS.put("o", new String[]{"ó"});
        LONG_PRESS.put("s", new String[]{"ś"});
        LONG_PRESS.put("z", new String[]{"ź", "ż"});
        LONG_PRESS.put(".", new String[]{"?", "!"});
    }

    private LearningStore store;
    private SharedPreferences prefs;
    private ClipboardManager clipboard;
    private final StringBuilder token = new StringBuilder();
    private final List<TextView> suggestionViews = new ArrayList<>();
    private Button profileButton;
    private String previousWord = "";
    private String pendingWord = "";
    private String pendingPreviousWord = "";
    private boolean learningAllowedInField = true;

    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private boolean repeatingBackspace = false;
    private final Runnable repeatDelete = new Runnable() {
        @Override public void run() {
            if (!repeatingBackspace) return;
            handleBackspace();
            repeatHandler.postDelayed(this, 60);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        store = new LearningStore(this);
        prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override public View onCreateInputView() {
        suggestionViews.clear();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(2), dp(2), dp(2), dp(4));
        root.setBackgroundColor(Color.rgb(29, 31, 40));

        LinearLayout suggestionRow = new LinearLayout(this);
        suggestionRow.setOrientation(LinearLayout.HORIZONTAL);
        suggestionRow.setPadding(dp(1), 0, dp(1), dp(3));
        for (int i = 0; i < 3; i++) {
            TextView text = new TextView(this);
            text.setGravity(Gravity.CENTER);
            text.setTextSize(i == 1 ? 21 : 19);
            text.setTextColor(Color.WHITE);
            text.setSingleLine(true);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setBackgroundColor(Color.rgb(56, 57, 65));
            text.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                useSuggestion(((TextView) v).getText().toString());
            });
            text.setOnLongClickListener(v -> {
                String value = ((TextView) v).getText().toString();
                if (!TextUtils.isEmpty(value) && PROFILE_WORK.equals(profile())) {
                    store.forgetWord(PROFILE_WORK, value);
                    Toast.makeText(this, "Usunięto z nauki: " + value, Toast.LENGTH_SHORT).show();
                    refreshSuggestions();
                    return true;
                }
                return false;
            });
            LinearLayout.LayoutParams suggestionParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
            suggestionParams.setMargins(dp(1), 0, dp(1), 0);
            suggestionRow.addView(text, suggestionParams);
            suggestionViews.add(text);
        }
        root.addView(suggestionRow);

        for (int rowIndex = 0; rowIndex < ROWS.length; rowIndex++) {
            String[] rowKeys = ROWS[rowIndex];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_HORIZONTAL);

            if (rowIndex == 1) {
                addSpacer(row, 0.30f);
            } else if (rowIndex == 2) {
                addSpacer(row, 0.55f);
            }

            for (String key : rowKeys) {
                Button button = makeKey(key);
                float weight = keyWeight(key);
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, dp(72), weight);
                keyParams.setMargins(dp(1), dp(2), dp(1), dp(2));
                row.addView(button, keyParams);
                if ("PROFIL".equals(key)) profileButton = button;
            }

            if (rowIndex == 1) {
                addSpacer(row, 0.30f);
            } else if (rowIndex == 2) {
                addSpacer(row, 0.55f);
            }

            root.addView(row, new LinearLayout.LayoutParams(-1, dp(76)));
        }

        refreshSuggestions();
        return root;
    }

    private Button makeKey(String key) {
        Button button = new Button(this);
        button.setText("PROFIL".equals(key) ? profileLabel() : key);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(key.length() > 3 ? 13 : 24);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(makeKeyBackground());

        if ("⌫".equals(key)) {
            setupBackspace(button);
        } else {
            button.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                handleKey(key);
            });
            if (LONG_PRESS.containsKey(key)) {
                button.setOnLongClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    showAlternatives(v, key, LONG_PRESS.get(key));
                    return true;
                });
            }
        }
        return button;
    }

    private GradientDrawable makeKeyBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(62, 63, 72));
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(1), Color.rgb(18, 19, 24));
        return drawable;
    }

    private void addSpacer(LinearLayout row, float weight) {
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, weight));
    }

    private float keyWeight(String key) {
        if (" ".equals(key)) return 3.6f;
        if ("PROFIL".equals(key)) return 1.7f;
        if ("WKLEJ".equals(key)) return 1.35f;
        if ("⌫".equals(key)) return 1.25f;
        if ("⏎".equals(key)) return 1.2f;
        return 1f;
    }

    private void setupBackspace(Button button) {
        button.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            handleBackspace();
        });
        button.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                repeatingBackspace = true;
                repeatHandler.postDelayed(repeatDelete, 420);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                repeatingBackspace = false;
                repeatHandler.removeCallbacks(repeatDelete);
            }
            return false;
        });
    }

    private void showAlternatives(View anchor, String sourceKey, String[] alternatives) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(dp(3), dp(3), dp(3), dp(3));
        container.setBackgroundColor(Color.rgb(35, 36, 44));

        final PopupWindow[] holder = new PopupWindow[1];
        for (String alt : alternatives) {
            Button choice = new Button(this);
            choice.setText(alt);
            choice.setTextSize(24);
            choice.setTextColor(Color.WHITE);
            choice.setAllCaps(false);
            choice.setBackground(makeKeyBackground());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(66), dp(66));
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            container.addView(choice, params);
            choice.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                insertAlternative(sourceKey, alt);
                if (holder[0] != null) holder[0].dismiss();
            });
        }

        PopupWindow popup = new PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        holder[0] = popup;
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(8));
        popup.showAsDropDown(anchor, 0, -dp(142), Gravity.CENTER_HORIZONTAL);
    }

    private void insertAlternative(String sourceKey, String value) {
        if (getCurrentInputConnection() == null) return;
        if (".".equals(sourceKey)) {
            finishToken(false);
            commitPendingLearning();
            getCurrentInputConnection().commitText(value, 1);
            refreshSuggestions();
            return;
        }

        commitPendingLearning();
        getCurrentInputConnection().commitText(value, 1);
        token.append(value.toLowerCase(new Locale("pl", "PL")));
        refreshSuggestions();
    }

    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        token.setLength(0);
        previousWord = "";
        pendingWord = "";
        pendingPreviousWord = "";
        learningAllowedInField = !isSensitiveField(attribute);
        refreshSuggestions();
    }

    @Override public void onFinishInput() {
        repeatingBackspace = false;
        repeatHandler.removeCallbacks(repeatDelete);
        commitPendingLearning();
        super.onFinishInput();
    }

    private void handleKey(String key) {
        if (getCurrentInputConnection() == null) return;
        if ("⌫".equals(key)) {
            handleBackspace();
        } else if ("⏎".equals(key)) {
            finishToken(false);
            commitPendingLearning();
            getCurrentInputConnection().commitText("\n", 1);
            previousWord = "";
            refreshSuggestions();
        } else if ("PROFIL".equals(key)) {
            commitPendingLearning();
            toggleProfile();
        } else if ("WKLEJ".equals(key)) {
            commitPendingLearning();
            pasteClipboard();
        } else if (" ".equals(key)) {
            finishToken(true);
        } else if (",".equals(key) || ".".equals(key)) {
            finishToken(false);
            commitPendingLearning();
            getCurrentInputConnection().commitText(key, 1);
        } else {
            if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                commitPendingLearning();
                getCurrentInputConnection().commitText(key, 1);
                token.append(key.toLowerCase(new Locale("pl", "PL")));
            } else {
                getCurrentInputConnection().commitText(key, 1);
            }
            refreshSuggestions();
        }
    }

    private void handleBackspace() {
        if (getCurrentInputConnection() == null) return;
        if (token.length() > 0) {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
            token.deleteCharAt(token.length() - 1);
        } else if (!pendingWord.isEmpty()) {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
            token.append(pendingWord);
            previousWord = pendingPreviousWord;
            pendingWord = "";
            pendingPreviousWord = "";
        } else {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
        }
        refreshSuggestions();
    }

    private void finishToken(boolean addSpace) {
        if (token.length() == 0) {
            if (addSpace) getCurrentInputConnection().commitText(" ", 1);
            return;
        }

        String typed = LearningStore.normalizeWord(token.toString());
        String finalWord = typed;

        if (!typed.isEmpty() && prefs.getBoolean("autocorrect", true)) {
            String polishCorrection = PolishAutocorrect.correct(typed);
            if (!polishCorrection.equals(typed)) {
                getCurrentInputConnection().deleteSurroundingText(token.length(), 0);
                getCurrentInputConnection().commitText(polishCorrection, 1);
                finalWord = polishCorrection;
            } else if (!store.isKnown(profile(), typed)) {
                String learnedCorrection = store.bestCorrection(profile(), typed);
                if (!learnedCorrection.equals(typed)) {
                    getCurrentInputConnection().deleteSurroundingText(token.length(), 0);
                    getCurrentInputConnection().commitText(learnedCorrection, 1);
                    finalWord = learnedCorrection;
                }
            }
        }

        pendingWord = finalWord;
        pendingPreviousWord = previousWord;
        if (!finalWord.isEmpty()) previousWord = finalWord;
        token.setLength(0);
        if (addSpace) getCurrentInputConnection().commitText(" ", 1);
        refreshSuggestions();
    }

    private void commitPendingLearning() {
        if (pendingWord.isEmpty()) return;
        if (PROFILE_WORK.equals(profile()) && learningAllowedInField) {
            store.learn(PROFILE_WORK, pendingPreviousWord, pendingWord);
        }
        pendingWord = "";
        pendingPreviousWord = "";
    }

    private void useSuggestion(String value) {
        if (TextUtils.isEmpty(value) || getCurrentInputConnection() == null) return;
        commitPendingLearning();
        String word = LearningStore.normalizeWord(value);
        getCurrentInputConnection().deleteSurroundingText(token.length(), 0);
        getCurrentInputConnection().commitText(word, 1);
        token.setLength(0);
        token.append(word);
        refreshSuggestions();
    }

    private void pasteClipboard() {
        if (clipboard == null || !clipboard.hasPrimaryClip() || getCurrentInputConnection() == null) return;
        ClipData data = clipboard.getPrimaryClip();
        if (data == null || data.getItemCount() == 0) return;
        CharSequence text = data.getItemAt(0).coerceToText(this);
        if (!TextUtils.isEmpty(text)) {
            getCurrentInputConnection().commitText(text, 1);
            token.setLength(0);
            previousWord = "";
            refreshSuggestions();
        }
    }

    private void refreshSuggestions() {
        if (suggestionViews.isEmpty() || store == null) return;
        List<String> ranked = store.suggest(profile(), token.toString(), previousWord, 3);
        String[] display = new String[]{"", "", ""};
        if (ranked.size() == 1) {
            display[1] = ranked.get(0);
        } else if (ranked.size() == 2) {
            display[0] = ranked.get(1);
            display[1] = ranked.get(0);
        } else if (ranked.size() >= 3) {
            display[0] = ranked.get(1);
            display[1] = ranked.get(0);
            display[2] = ranked.get(2);
        }
        for (int i = 0; i < suggestionViews.size(); i++) suggestionViews.get(i).setText(display[i]);
        if (profileButton != null) profileButton.setText(profileLabel());
    }

    private void toggleProfile() {
        String next = PROFILE_WORK.equals(profile()) ? PROFILE_NORMAL : PROFILE_WORK;
        prefs.edit().putString("profile", next).apply();
        token.setLength(0);
        previousWord = "";
        pendingWord = "";
        pendingPreviousWord = "";
        refreshSuggestions();
    }

    private boolean isSensitiveField(EditorInfo info) {
        if (info == null) return false;
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        if (inputClass == InputType.TYPE_CLASS_TEXT) {
            return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        }
        return inputClass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    }

    private String profile() { return prefs.getString("profile", PROFILE_WORK); }
    private String profileLabel() { return PROFILE_WORK.equals(profile()) ? "PRACA" : "NORMAL"; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
