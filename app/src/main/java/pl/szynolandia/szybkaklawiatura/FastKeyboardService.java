package pl.szynolandia.szybkaklawiatura;

import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class FastKeyboardService extends InputMethodService {
    private static final String[][] ROWS = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"z","c","b","n","m","⌫"},
            {","," ",".","⏎"}
    };

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(4, 4, 4, 4);
        for (String[] rowKeys : ROWS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String key : rowKeys) {
                Button button = new Button(this);
                button.setText(key);
                button.setGravity(Gravity.CENTER);
                button.setTextSize(18);
                button.setMinWidth(0);
                button.setMinimumWidth(0);
                button.setOnClickListener(v -> handleKey(((Button) v).getText().toString()));
                float weight = " ".equals(key) ? 3f : 1f;
                row.addView(button, new LinearLayout.LayoutParams(0, dp(68), weight));
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, dp(70)));
        }
        return root;
    }

    private void handleKey(String key) {
        if (getCurrentInputConnection() == null) return;
        if ("⌫".equals(key)) {
            getCurrentInputConnection().deleteSurroundingText(1, 0);
        } else if ("⏎".equals(key)) {
            getCurrentInputConnection().commitText("\n", 1);
        } else {
            getCurrentInputConnection().commitText(key, 1);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
