package pl.szynolandia.szybkaklawiatura;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FastKeyboardService extends InputMethodService {
    public static final String PROFILE_WORK = "WORK";
    public static final String PROFILE_NORMAL = "NORMAL";

    private static final String[][] ROWS = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"z","c","b","n","m","⌫"},
            {"PROFIL",","," ",".","WKLEJ","⏎"}
    };

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
        root.setPadding(dp(4), dp(4), dp(4), dp(4));

        LinearLayout suggestionRow = new LinearLayout(this);
        suggestionRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 3; i++) {
            TextView text = new TextView(this);
            text.setGravity(Gravity.CENTER);
            text.setTextSize(16);
            text.setSingleLine(true);
            text.setEllipsize(TextUtils.TruncateAt.END);
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
            LinearLayout.LayoutParams suggestionParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            suggestionParams.setMargins(dp(2), dp(1), dp(2), dp(1));
            suggestionRow.addView(text, suggestionParams);
            suggestionViews.add(text);
        }
        root.addView(suggestionRow);

        for (String[] rowKeys : ROWS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String key : rowKeys) {
                Button button = new Button(this);
                button.setText("PROFIL".equals(key) ? profileLabel() : key);
                button.setGravity(Gravity.CENTER);
                button.setTextSize(key.length() > 3 ? 12 : 18);
                button.setMinWidth(0);
                button.setMinimumWidth(0);
                button.setPadding(0, 0, 0, 0);
                button.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    handleKey(((Button) v).getText().toString());
                });
                float weight = " ".equals(key) ? 3f : ("PROFIL".equals(key) ? 1.4f : 1f);
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, dp(64), weight);
                keyParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(button, keyParams);
                if ("PROFIL".equals(key)) profileButton = button;
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, dp(68)));
        }
        refreshSuggestions();
        return root;
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
        } else if ("PRACA".equals(key) || "NORMAL".equals(key)) {
            commitPendingLearning();
            toggleProfile();
        } else if ("WKLEJ".equals(key)) {
            commitPendingLearning();
            pasteClipboard();
        } else if (" ".equals(key)) {
            finishToken(true);
        } else if (",.".contains(key)) {
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
        if (!typed.isEmpty() && prefs.getBoolean("autocorrect", true) && !store.isKnown(profile(), typed)) {
            String correction = store.bestCorrection(profile(), typed);
            if (!correction.equals(typed)) {
                getCurrentInputConnection().deleteSurroundingText(token.length(), 0);
                getCurrentInputConnection().commitText(correction, 1);
                finalWord = correction;
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
