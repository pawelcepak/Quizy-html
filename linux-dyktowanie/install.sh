#!/usr/bin/env bash
set -euo pipefail

BASE="$HOME/.local/share/szybkie-dyktowanie"
VENV="$BASE/venv"
MODEL="$BASE/vosk-model-small-pl-0.22"
URL="https://alphacephei.com/vosk/models/vosk-model-small-pl-0.22.zip"
ROOT="$(cd "$(dirname "$0")" && pwd)"

sudo apt update
sudo apt install -y python3 python3-venv python3-pip portaudio19-dev xdotool unzip curl

mkdir -p "$BASE"
python3 -m venv "$VENV"
"$VENV/bin/pip" install --upgrade pip
"$VENV/bin/pip" install -r "$ROOT/requirements.txt"

if [ ! -d "$MODEL" ]; then
  TMP="$BASE/model.zip"
  echo "Pobieram polski model Vosk (~50 MB)…"
  curl -L --fail --retry 3 "$URL" -o "$TMP"
  unzip -q "$TMP" -d "$BASE"
  rm -f "$TMP"
fi

cat > "$BASE/start.sh" <<EOF
#!/usr/bin/env bash
exec "$VENV/bin/python" "$ROOT/dyktowanie.py"
EOF
chmod +x "$BASE/start.sh"

mkdir -p "$HOME/.local/bin"
cat > "$HOME/.local/bin/szybkie-dyktowanie" <<EOF
#!/usr/bin/env bash
exec "$BASE/start.sh" "\$@"
EOF
chmod +x "$HOME/.local/bin/szybkie-dyktowanie"

echo
echo "Gotowe. Uruchom: szybkie-dyktowanie"
echo "F8 = start/stop dyktowania, Esc = wyjście."
echo "Model działa lokalnie; audio nie jest wysyłane do serwera."
