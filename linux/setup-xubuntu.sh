#!/usr/bin/env bash
set -euo pipefail

sudo apt update
sudo apt install -y kdeconnect

echo
echo "KDE Connect zainstalowany."
echo "1. Zainstaluj KDE Connect na telefonie."
echo "2. Telefon i laptop musza byc w tej samej sieci Wi-Fi."
echo "3. Sparuj urzadzenia."
echo "4. W telefonie otworz Remote Input / Zdalne wprowadzanie."
echo "5. Kliknij pole tekstowe w przegladarce na Xubuntu i pisz z telefonu lub klawiatury laptopa."
