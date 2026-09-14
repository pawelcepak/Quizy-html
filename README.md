# Szybka Klawiatura

Spersonalizowany build Androidowej klawiatury oparty na AnySoftKeyboard.

## v0.1

- zoptymalizowany polski układ QWERTY,
- X i V usunięte z głównego rzędu,
- X dostępne przez przytrzymanie C,
- V dostępne przez przytrzymanie B,
- polskie znaki z istniejących popupów układu polskiego,
- podpowiedzi, autokorekta i uczenie słownictwa zapewniane przez silnik AnySoftKeyboard,
- automatyczny build APK w GitHub Actions.

## Pobieranie

Wejdź w Actions -> Build Android Keyboard -> najnowszy zielony przebieg -> Artifacts -> `SzybkaKlawiatura-v0.1-APK`.

Po rozpakowaniu ZIP otrzymasz `SzybkaKlawiatura-v0.1-debug.apk`.

## Xubuntu

Do używania telefonu jako dodatkowego wejścia na laptopie zalecany jest KDE Connect i funkcja Remote Input. Fizyczna klawiatura laptopa może działać równolegle.

## Build

Workflow pobiera przypięty commit AnySoftKeyboard, nakłada `config/polish_speed.xml`, buduje wariant z dodatkami językowymi i publikuje APK jako artefakt Actions.
