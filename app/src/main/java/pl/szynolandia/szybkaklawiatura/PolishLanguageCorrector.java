package pl.szynolandia.szybkaklawiatura;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import morfologik.stemming.PolishStemmer;

/** Offline Polish validation based on Morfologik. No network/API key required. */
public final class PolishLanguageCorrector {
    private final PolishStemmer stemmer = new PolishStemmer();

    public String correctDiacritics(String input) {
        if (input == null || input.length() < 2) return input == null ? "" : input;
        String word = input.toLowerCase(new Locale("pl", "PL"));
        if (isPolishWord(word)) return word;

        List<String> candidates = new ArrayList<>();
        buildCandidates(word.toCharArray(), 0, candidates, 256);
        String best = word;
        int bestChanges = Integer.MAX_VALUE;
        for (String candidate : new LinkedHashSet<>(candidates)) {
            if (candidate.equals(word) || !isPolishWord(candidate)) continue;
            int changes = changedCharacters(word, candidate);
            if (changes < bestChanges) {
                best = candidate;
                bestChanges = changes;
            }
        }
        return best;
    }

    public boolean isPolishWord(String word) {
        try {
            return word != null && !word.isEmpty() && !stemmer.lookup(word).isEmpty();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void buildCandidates(char[] chars, int index, List<String> out, int limit) {
        if (out.size() >= limit) return;
        if (index >= chars.length) {
            out.add(new String(chars));
            return;
        }
        char original = chars[index];
        buildCandidates(chars, index + 1, out, limit);
        for (char replacement : alternatives(original)) {
            if (out.size() >= limit) break;
            chars[index] = replacement;
            buildCandidates(chars, index + 1, out, limit);
        }
        chars[index] = original;
    }

    private char[] alternatives(char c) {
        switch (c) {
            case 'a': return new char[]{'ą'};
            case 'c': return new char[]{'ć'};
            case 'e': return new char[]{'ę'};
            case 'l': return new char[]{'ł'};
            case 'n': return new char[]{'ń'};
            case 'o': return new char[]{'ó'};
            case 's': return new char[]{'ś'};
            case 'z': return new char[]{'ż','ź'};
            default: return new char[0];
        }
    }

    private int changedCharacters(String a, String b) {
        int count = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) if (a.charAt(i) != b.charAt(i)) count++;
        return count + Math.abs(a.length() - b.length());
    }
}
