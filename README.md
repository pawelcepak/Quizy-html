# Szybka Klawiatura

Własna klawiatura systemowa Android napisana od zera pod szybkie pisanie na telefonie.

## v0.2.0

- osobna aplikacja i osobny IME `Szybka Klawiatura`, bez AnySoftKeyboard,
- duże klawisze i uproszczony układ,
- X i V usunięte z głównego układu,
- osobne profile `PRACA` i `NORMAL`,
- trzy podpowiedzi z własnego słownika,
- korekta na podstawie własnego słownika,
- duży przycisk `WKLEJ`, który wkleja aktualną zawartość schowka po dotknięciu,
- dane słownika przechowywane lokalnie na telefonie,
- baza danych ma migracje addytywne: aktualizacja wersji nie kasuje tabel `words` ani `bigrams`,
- eksport całego profilu PRACA do pliku JSON,
- import profilu PRACA z pliku JSON,
- licznik wyuczonych słów w ekranie aplikacji,
- możliwość wyzerowania profilu PRACA,
- automatyczny build APK w GitHub Actions.

## Zachowanie danych między wersjami

Słownik jest zapisany w prywatnej bazie aplikacji. Zwykła aktualizacja APK zachowuje dane. Migracje bazy są projektowane tak, aby nie usuwać wcześniej wyuczonych słów ani statystyk kolejności słów.

Przed odinstalowaniem aplikacji, wyczyszczeniem jej danych albo zmianą telefonu użyj `Eksportuj profil PRACA do pliku`. Plik można później wczytać przyciskiem `Importuj profil PRACA z pliku`.

Format kopii jest wersjonowany, aby przyszłe wydania mogły zachować zgodność z wcześniejszymi kopiami.

## Prywatność

Klawiatura nie monitoruje automatycznie schowka. `WKLEJ` odczytuje bieżącą zawartość schowka dopiero po świadomym dotknięciu przycisku.

## Pobieranie

Wejdź w `Actions` -> `Build Szybka Klawiatura` -> najnowszy zielony przebieg -> `Artifacts` -> `SzybkaKlawiatura-v0.2.0-APK`.

Po rozpakowaniu ZIP otrzymasz `SzybkaKlawiatura-v0.2.0.apk`.

## Instalacja

1. Zainstaluj APK.
2. Uruchom aplikację `Szybka Klawiatura`.
3. Naciśnij `1. Włącz Szybką Klawiaturę` i zezwól na używanie klawiatury.
4. Wróć do aplikacji i naciśnij `2. Wybierz klawiaturę`.
5. Wybierz `Szybka Klawiatura`.
6. Przed reinstalacją lub zmianą telefonu wykonaj eksport profilu PRACA.

## Build lokalny

Projekt wymaga JDK 17, Android SDK 35 i Gradle 8.7. Build debug: `gradle :app:assembleDebug`.
