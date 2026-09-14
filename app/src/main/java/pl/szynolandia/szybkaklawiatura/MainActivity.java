package pl.szynolandia.szybkaklawiatura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private LearningStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LearningStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Szybka Klawiatura");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Profil PRACA ma osobny słownik. Dodawaj tutaj słowa, których używasz często. Klawiatura będzie je podpowiadać i używać do korekty.");
        info.setTextSize(16);
        info.setPadding(0, dp(18), 0, dp(18));
        root.addView(info);

        Button enable = new Button(this);
        enable.setText("1. Włącz Szybką Klawiaturę");
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        root.addView(enable);

        Button choose = new Button(this);
        choose.setText("2. Wybierz klawiaturę");
        choose.setOnClickListener(v -> ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        root.addView(choose);

        EditText word = new EditText(this);
        word.setHint("Dodaj słowo do profilu PRACA");
        root.addView(word);

        Button add = new Button(this);
        add.setText("Dodaj do słownika PRACA");
        add.setOnClickListener(v -> {
            String value = LearningStore.normalizeWord(word.getText().toString());
            if (value.isEmpty()) return;
            store.learn(FastKeyboardService.PROFILE_WORK, "", value);
            word.setText("");
            Toast.makeText(this, "Dodano: " + value, Toast.LENGTH_SHORT).show();
        });
        root.addView(add);

        Button reset = new Button(this);
        reset.setText("Wyczyść słownik PRACA");
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Wyczyścić słownik?")
                .setMessage("Usunie to wszystkie własne słowa profilu PRACA.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Wyczyść", (d, w) -> {
                    store.resetProfile(FastKeyboardService.PROFILE_WORK);
                    Toast.makeText(this, "Słownik wyczyszczony", Toast.LENGTH_SHORT).show();
                }).show());
        root.addView(reset);

        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
