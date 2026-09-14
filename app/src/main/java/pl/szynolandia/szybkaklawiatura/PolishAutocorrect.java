package pl.szynolandia.szybkaklawiatura;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mały, lokalny słownik korekty brakujących polskich znaków.
 * Nie zapisuje tekstu użytkownika i działa także przy pustym profilu PRACA.
 */
public final class PolishAutocorrect {
    private static final Map<String, String> WORDS;

    static {
        Map<String, String> m = new HashMap<>();
        add(m,
                "lubie", "lubię",
                "szkola", "szkoła",
                "szkole", "szkole",
                "szkoly", "szkoły",
                "sie", "się",
                "moze", "może",
                "mozesz", "możesz",
                "mozesz", "możesz",
                "można", "można",
                "mozna", "można",
                "bede", "będę",
                "bedziesz", "będziesz",
                "bedziemy", "będziemy",
                "chce", "chcę",
                "chcesz", "chcesz",
                "prosze", "proszę",
                "dziekuje", "dziękuję",
                "dzieki", "dzięki",
                "czesc", "cześć",
                "milosc", "miłość",
                "kocham", "kocham",
                "ktory", "który",
                "ktora", "która",
                "ktore", "które",
                "ktorym", "którym",
                "ktorego", "którego",
                "cos", "coś",
                "jakis", "jakiś",
                "jakas", "jakaś",
                "jakies", "jakieś",
                "kiedys", "kiedyś",
                "dzis", "dziś",
                "jutro", "jutro",
                "wczesniej", "wcześniej",
                "pozniej", "później",
                "wlasnie", "właśnie",
                "naprawde", "naprawdę",
                "troche", "trochę",
                "duzo", "dużo",
                "malo", "mało",
                "dobrze", "dobrze",
                "zle", "źle",
                "rowniez", "również",
                "rownie", "równie",
                "piekny", "piękny",
                "piekna", "piękna",
                "piekne", "piękne",
                "slodki", "słodki",
                "slodka", "słodka",
                "slodkie", "słodkie",
                "malenka", "maleńka",
                "malenki", "maleńki",
                "skarbie", "skarbie",
                "slonce", "słońce",
                "slonko", "słonko",
                "tesknie", "tęsknię",
                "tesknisz", "tęsknisz",
                "mysle", "myślę",
                "myslisz", "myślisz",
                "spokojnie", "spokojnie",
                "pewnie", "pewnie",
                "pytanie", "pytanie",
                "dlaczego", "dlaczego",
                "gdzie", "gdzie",
                "juz", "już",
                "tez", "też",
                "jeszcze", "jeszcze",
                "wiecej", "więcej",
                "mniej", "mniej",
                "ma", "ma",
                "mam", "mam",
                "masz", "masz",
                "mamy", "mamy",
                "sa", "są",
                "jestes", "jesteś",
                "jestem", "jestem",
                "bys", "byś",
                "bym", "bym",
                "byl", "był",
                "byla", "była",
                "bylo", "było",
                "byc", "być",
                "miec", "mieć",
                "zrobic", "zrobić",
                "powiedziec", "powiedzieć",
                "napisac", "napisać",
                "spotkac", "spotkać",
                "zobaczyc", "zobaczyć",
                "wrocic", "wrócić",
                "isc", "iść",
                "przyjsc", "przyjść",
                "wyjsc", "wyjść",
                "wziac", "wziąć",
                "dac", "dać",
                "wiedziec", "wiedzieć",
                "pamietam", "pamiętam",
                "pamietasz", "pamiętasz",
                "rozumiem", "rozumiem",
                "rozumiesz", "rozumiesz",
                "myslalem", "myślałem",
                "myslalam", "myślałam",
                "moge", "mogę",
                "mozesz", "możesz",
                "musze", "muszę",
                "musisz", "musisz",
                "powinnam", "powinnam",
                "powinienem", "powinienem",
                "cie", "cię",
                "tobie", "tobie",
                "twoj", "twój",
                "twoja", "twoja",
                "twoje", "twoje",
                "swoj", "swój",
                "swoja", "swoją",
                "swoje", "swoje",
                "moja", "moja",
                "moj", "mój",
                "moje", "moje",
                "mna", "mną",
                "toba", "tobą",
                "blisko", "blisko",
                "daleko", "daleko",
                "wtedy", "wtedy",
                "ktos", "ktoś",
                "nikt", "nikt",
                "wszyscy", "wszyscy",
                "kazdy", "każdy",
                "kazda", "każda",
                "kazde", "każde",
                "zadna", "żadna",
                "zadny", "żaden",
                "zadnego", "żadnego",
                "napisz", "napisz",
                "powiedz", "powiedz",
                "zrob", "zrób",
                "chodz", "chodź",
                "wróc", "wróć",
                "wroc", "wróć"
        );
        WORDS = Collections.unmodifiableMap(m);
    }

    private PolishAutocorrect() {}

    private static void add(Map<String, String> map, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
    }

    public static String correct(String input) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;
        String lower = input.toLowerCase(new Locale("pl", "PL"));
        String corrected = WORDS.get(lower);
        return corrected != null ? corrected : input;
    }
}
