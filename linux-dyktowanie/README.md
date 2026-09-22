# Szybkie Dyktowanie Xubuntu

Mały, lokalny program do dyktowania po polsku w aktywnym polu tekstowym. Działa w Firefoxie, Zenie i innych aplikacjach X11.

## Jak działa

- `F8` — włącza/wyłącza dyktowanie.
- Mówisz do mikrofonu, a rozpoznany tekst pojawia się **na bieżąco**, a nie dopiero po zatrzymaniu nagrania.
- `Esc` — zamyka program.
- Rozpoznawanie mowy odbywa się lokalnie przez Vosk.
- Program nie jest rozszerzeniem Firefoxa i nie modyfikuje DOM strony.
- Do wprowadzenia tekstu używa systemowego wejścia klawiatury X11 (`xdotool`).

## Instalacja na Xubuntu

W terminalu:

```bash
cd ~/Pobrane/Quizy-html/linux-dyktowanie
bash install.sh
```

Jeśli repo masz w innym miejscu, przejdź do odpowiedniego katalogu.

Instalator doinstaluje `xdotool`, obsługę mikrofonu, środowisko Pythona oraz mały polski model Vosk (~50 MB). Model jest przeznaczony do pracy strumieniowej i offline. Vosk podaje, że małe modele są odpowiednie również dla desktopów i zwykle wymagają około 300 MB RAM podczas pracy.

Po instalacji:

```bash
szybkie-dyktowanie
```

Potem kliknij pole tekstowe w Firefoxie/Zenie i naciśnij `F8`.

## Ważne

To jest pierwsza wersja prototypowa. Vosk daje wyniki strumieniowe, więc program może korygować ostatni fragment tekstu, kiedy rozpoznanie się doprecyzuje. Nie stosujemy sztucznych opóźnień ani mechanizmów mających ukrywać automatyzację. Celem jest normalne, lokalne dyktowanie jako metoda wprowadzania tekstu.

Jeżeli Firefox/Zen działa w Waylandzie zamiast X11, `xdotool` może nie działać prawidłowo. Xubuntu zwykle używa sesji X11; w razie problemu sprawdź:

```bash
echo $XDG_SESSION_TYPE
```

Powinno zwrócić `x11`.

## Źródło modelu

Używany jest oficjalnie publikowany model `vosk-model-small-pl-0.22`. Vosk opisuje małe modele jako przeznaczone do pracy strumieniowej i lokalnej, a model polski `vosk-model-small-pl-0.22` ma rozmiar około 50 MB. 
