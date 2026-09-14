# Szybka Klawiatura

Własna klawiatura systemowa Android napisana od zera pod szybkie pisanie na telefonie.

## v0.1.0

- osobna aplikacja i osobny IME `Szybka Klawiatura`, bez AnySoftKeyboard,
- duże klawisze i uproszczony układ,
- X i V usunięte z głównego układu,
- osobne profile `PRACA` i `NORMAL`,
- trzy podpowiedzi z własnego słownika,
- korekta na podstawie własnego słownika,
- ekran dodawania słów do słownika profilu PRACA,
- możliwość wyczyszczenia słownika PRACA,
- duży przycisk `WKLEJ`, który wkleja aktualną zawartość schowka po dotknięciu,
- dane słownika przechowywane lokalnie na telefonie,
- automatyczny build APK w GitHub Actions.

## Prywatność

v0.1.0 nie zapisuje automatycznie całej wpisywanej treści i nie monitoruje automatycznie schowka. Słowa do profilu PRACA dodajesz świadomie z ekranu aplikacji, a `WKLEJ` odczytuje schowek dopiero po dotknięciu przycisku.

## Pobieranie

Wejdź w `Actions` -> `Build Szybka Klawiatura` -> najnowszy zielony przebieg -> `Artifacts` -> `SzybkaKlawiatura-v0.1.0-APK`.

Po rozpakowaniu ZIP otrzymasz `SzybkaKlawiatura-v0.1.0.apk`.

## Instalacja

1. Zainstaluj APK.
2. Uruchom aplikację `Szybka Klawiatura`.
3. Naciśnij `1. Włącz Szybką Klawiaturę` i zezwól na używanie klawiatury.
4. Wróć do aplikacji i naciśnij `2. Wybierz klawiaturę`.
5. Wybierz `Szybka Klawiatura`.
6. Dodaj często używane słowa do profilu PRACA i zacznij testy.

## Build lokalny

Projekt wymaga JDK 17, Android SDK 35 i Gradle 8.7. Build debug: `gradle :app:assembleDebug`.
