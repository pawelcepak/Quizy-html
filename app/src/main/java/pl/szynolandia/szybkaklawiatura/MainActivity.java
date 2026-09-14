package pl.szynolandia.szybkaklawiatura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_EXPORT = 1001;
    private static final int REQ_IMPORT = 1002;

    private LearningStore store;
    private TextView stats;

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
        info.setText("Profil PRACA zaczyna od zera i ma własny lokalny słownik. Aktualizacje aplikacji nie kasują danych. Kopię profilu możesz zapisać do pliku przed zmianą telefonu lub reinstalacją.");
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

        stats = new TextView(this);
        stats.setTextSize(16);
        stats.setPadding(0, dp(18), 0, dp(12));
        root.addView(stats);

        Button export = new Button(this);
        export.setText("Eksportuj profil PRACA do pliku");
        export.setOnClickListener(v -> chooseExportFile());
        root.addView(export);

        Button importButton = new Button(this);
        importButton.setText("Importuj profil PRACA z pliku");
        importButton.setOnClickListener(v -> chooseImportFile());
        root.addView(importButton);

        Button reset = new Button(this);
        reset.setText("Wyczyść profil PRACA");
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Wyczyścić profil PRACA?")
                .setMessage("Usunie to wyuczone słowa i ich kolejność. Tej operacji nie można cofnąć bez wcześniejszej kopii.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Wyczyść", (d, w) -> {
                    store.resetProfile(FastKeyboardService.PROFILE_WORK);
                    refreshStats();
                    Toast.makeText(this, "Profil PRACA wyzerowany", Toast.LENGTH_SHORT).show();
                }).show());
        root.addView(reset);

        TextView note = new TextView(this);
        note.setText("Zwykła aktualizacja APK zachowuje dane aplikacji. Profil może zniknąć po odinstalowaniu aplikacji lub wyczyszczeniu jej danych, dlatego przed taką operacją użyj eksportu.");
        note.setTextSize(14);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note);

        setContentView(root);
        refreshStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stats != null) refreshStats();
    }

    private void chooseExportFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "SzybkaKlawiatura-PRACA-backup.json");
        startActivityForResult(intent, REQ_EXPORT);
    }

    private void chooseImportFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) {
            exportTo(uri);
        } else if (requestCode == REQ_IMPORT) {
            importFrom(uri);
        }
    }

    private void exportTo(Uri uri) {
        try {
            String backup = store.exportProfile(FastKeyboardService.PROFILE_WORK);
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IOException("Brak strumienia wyjściowego");
                out.write(backup.getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "Kopia profilu PRACA zapisana", Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            Toast.makeText(this, "Nie udało się zapisać kopii", Toast.LENGTH_LONG).show();
        }
    }

    private void importFrom(Uri uri) {
        try {
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            String backup = json.toString();
            new AlertDialog.Builder(this)
                    .setTitle("Przywrócić profil PRACA?")
                    .setMessage("Obecny profil PRACA zostanie zastąpiony zawartością wybranej kopii.")
                    .setNegativeButton("Anuluj", null)
                    .setPositiveButton("Przywróć", (d, w) -> {
                        try {
                            store.importProfile(FastKeyboardService.PROFILE_WORK, backup, true);
                            refreshStats();
                            Toast.makeText(this, "Profil PRACA przywrócony", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(this, "Nieprawidłowa kopia profilu PRACA", Toast.LENGTH_LONG).show();
                        }
                    }).show();
        } catch (IOException e) {
            Toast.makeText(this, "Nie udało się odczytać kopii", Toast.LENGTH_LONG).show();
        }
    }

    private void refreshStats() {
        stats.setText("Wyuczone słowa w profilu PRACA: " + store.learnedWordCount(FastKeyboardService.PROFILE_WORK));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
