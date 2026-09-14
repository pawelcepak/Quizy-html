package pl.szynolandia.szybkaklawiatura;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LearningStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "szybka_klawiatura.db";
    private static final int DB_VERSION = 1;

    public LearningStore(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE words (profile TEXT NOT NULL, word TEXT NOT NULL, count INTEGER NOT NULL DEFAULT 1, last_used INTEGER NOT NULL, PRIMARY KEY(profile, word))");
        db.execSQL("CREATE TABLE bigrams (profile TEXT NOT NULL, prev_word TEXT NOT NULL, next_word TEXT NOT NULL, count INTEGER NOT NULL DEFAULT 1, last_used INTEGER NOT NULL, PRIMARY KEY(profile, prev_word, next_word))");
        db.execSQL("CREATE INDEX idx_words_profile_count ON words(profile, count DESC)");
        db.execSQL("CREATE INDEX idx_bigrams_profile_prev_count ON bigrams(profile, prev_word, count DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void learn(String profile, String previousWord, String word) {
        String clean = normalizeWord(word);
        if (clean.length() < 2) return;
        long now = System.currentTimeMillis();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL(
                    "INSERT INTO words(profile, word, count, last_used) VALUES(?,?,1,?) " +
                            "ON CONFLICT(profile,word) DO UPDATE SET count=count+1,last_used=excluded.last_used",
                    new Object[]{profile, clean, now});
            String prev = normalizeWord(previousWord);
            if (!prev.isEmpty()) {
                db.execSQL(
                        "INSERT INTO bigrams(profile, prev_word, next_word, count, last_used) VALUES(?,?,?,1,?) " +
                                "ON CONFLICT(profile,prev_word,next_word) DO UPDATE SET count=count+1,last_used=excluded.last_used",
                        new Object[]{profile, prev, clean, now});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<String> suggest(String profile, String prefix, String previousWord, int limit) {
        Set<String> output = new LinkedHashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        String cleanPrefix = normalizeWord(prefix);
        String prev = normalizeWord(previousWord);

        if (cleanPrefix.isEmpty() && !prev.isEmpty()) {
            try (Cursor c = db.rawQuery(
                    "SELECT next_word FROM bigrams WHERE profile=? AND prev_word=? ORDER BY count DESC,last_used DESC LIMIT ?",
                    new String[]{profile, prev, String.valueOf(limit)})) {
                while (c.moveToNext()) output.add(c.getString(0));
            }
        }

        if (!cleanPrefix.isEmpty()) {
            try (Cursor c = db.rawQuery(
                    "SELECT word FROM words WHERE profile=? AND word LIKE ? ORDER BY count DESC,last_used DESC LIMIT ?",
                    new String[]{profile, cleanPrefix + "%", String.valueOf(limit * 2)})) {
                while (c.moveToNext()) output.add(c.getString(0));
            }
        }

        if (output.size() < limit) {
            try (Cursor c = db.rawQuery(
                    "SELECT word FROM words WHERE profile=? ORDER BY count DESC,last_used DESC LIMIT ?",
                    new String[]{profile, String.valueOf(limit * 2)})) {
                while (c.moveToNext()) output.add(c.getString(0));
            }
        }

        return new ArrayList<>(output).subList(0, Math.min(limit, output.size()));
    }

    public String bestCorrection(String profile, String typed) {
        String source = normalizeWord(typed);
        if (source.length() < 3) return source;
        SQLiteDatabase db = getReadableDatabase();
        String best = source;
        double bestScore = Double.MAX_VALUE;

        try (Cursor c = db.rawQuery(
                "SELECT word,count FROM words WHERE profile=? ORDER BY count DESC,last_used DESC LIMIT 400",
                new String[]{profile})) {
            while (c.moveToNext()) {
                String candidate = c.getString(0);
                int count = c.getInt(1);
                if (Math.abs(candidate.length() - source.length()) > 2) continue;
                int distance = levenshtein(source, candidate);
                int maxAllowed = source.length() >= 7 ? 2 : 1;
                if (distance > maxAllowed) continue;
                double score = distance - Math.min(0.45, Math.log10(count + 1) * 0.12);
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        return best;
    }

    public boolean isKnown(String profile, String word) {
        String clean = normalizeWord(word);
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM words WHERE profile=? AND word=? LIMIT 1",
                new String[]{profile, clean})) {
            return c.moveToFirst();
        }
    }

    public void resetProfile(String profile) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("words", "profile=?", new String[]{profile});
            db.delete("bigrams", "profile=?", new String[]{profile});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int learnedWordCount(String profile) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM words WHERE profile=?", new String[]{profile})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public static String normalizeWord(String input) {
        if (input == null) return "";
        return input.toLowerCase(new Locale("pl", "PL"))
                .replaceAll("^[^a-ząćęłńóśźż]+|[^a-ząćęłńóśźż]+$", "")
                .trim();
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
