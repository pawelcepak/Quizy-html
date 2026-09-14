package pl.szynolandia.szybkaklawiatura;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
            {"PROFIL",","," ",".","⏎"}
    };

    private LearningStore store;
    private SharedPreferences prefs;
    private final StringBuilder token = new StringBuilder();
    private final List<TextView> suggestionViews = new ArrayList<>();
    private Button profileButton;

    @Override public void onCreate() {
        super.onCreate();
        store = new LearningStore(this);
        prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
    }

    @Override public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(4, 4, 4, 4);

        LinearLayout suggestionRow = new LinearLayout(this);
        suggestionRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 3; i++) {
            TextView text = new TextView(this);
            text.setGravity(Gravity.CENTER);
            text.setTextSize(16);
            text.setSingleLine(true);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setOnClickListener(v -> useSuggestion(((TextView) v).getText().toString()));
            suggestionRow.addView(text, new LinearLayout.LayoutParams(0, dp(48), 1f));
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
                button.setOnClickListener(v -> handleKey(((Button) v).getText().toString()));
                float weight = " ".equals(key) ? 3f : ("PROFIL".equals(key) ? 1.4f : 1f);
                row.addView(button, new LinearLayout.LayoutParams(0, dp(68), weight));
                if ("PROFIL".equals(key)) profileButton = button;
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, dp(70)));
        }
        refreshSuggestions();
        return root;
    }

    @Override public void onStartInput(android.view.inputmethod.EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        token.setLength(0);
        refreshSuggestions();
    }

    private void handleKey(String key) {
        if (getCurrentInputConnection() == null) return;
        if ("⌫".equals(key)) {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
            if (token.length() > 0) token.deleteCharAt(token.length() - 1);
            refreshSuggestions();
        } else if ("⏎".equals(key)) {
            finishToken(false);
            getCurrentInputConnection().commitText("\n", 1);
        } else if ("PRACA".equals(key) || "NORMAL".equals(key)) {
            toggleProfile();
        } else if (" ".equals(key)) {
            finishToken(true);
        } else if (",.".contains(key)) {
            finishToken(false);
            getCurrentInputConnection().commitText(key, 1);
        } else {
            getCurrentInputConnection().commitText(key, 1);
            if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                token.append(key.toLowerCase(new Locale("pl", "PL")));
            }
            refreshSuggestions();
        }
    }

    private void finishToken(boolean addSpace) {
        String typed = LearningStore.normalizeWord(token.toString());
        if (!typed.isEmpty() && prefs.getBoolean("autocorrect", true) && !store.isKnown(profile(), typed)) {
            String correction = store.bestCorrection(profile(), typed);
            if (!correction.equals(typed)) {
                getCurrentInputConnection().deleteSurroundingText(token.length(), 0);
                getCurrentInputConnection().commitText(correction, 1);
            }
        }
        token.setLength(0);
        if (addSpace) getCurrentInputConnection().commitText(" ", 1);
        refreshSuggestions();
    }

    private void useSuggestion(String value) {
        if (TextUtils.isEmpty(value) || getCurrentInputConnection() == null) return;
        String word = LearningStore.normalizeWord(value);
        getCurrentInputConnection().deleteSurroundingText(token.length(), 0);
        getCurrentInputConnection().commitText(word, 1);
        token.setLength(0);
        token.append(word);
        refreshSuggestions();
    }

    private void refreshSuggestions() {
        if (suggestionViews.isEmpty() || store == null) return;
        List<String> values = store.suggest(profile(), token.toString(), "", 3);
        for (int i = 0; i < suggestionViews.size(); i++) {
            suggestionViews.get(i).setText(i < values.size() ? values.get(i) : "");
        }
        if (profileButton != null) profileButton.setText(profileLabel());
    }

    private void toggleProfile() {
        String next = PROFILE_WORK.equals(profile()) ? PROFILE_NORMAL : PROFILE_WORK;
        prefs.edit().putString("profile", next).apply();
        token.setLength(0);
        refreshSuggestions();
    }

    private String profile() { return prefs.getString("profile", PROFILE_WORK); }
    private String profileLabel() { return PROFILE_WORK.equals(profile()) ? "PRACA" : "NORMAL"; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
