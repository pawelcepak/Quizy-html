#!/usr/bin/env python3
import argparse
import json
import os
import queue
import re
import subprocess
import sys
import threading
import time
import wave

import sounddevice as sd
from vosk import Model, KaldiRecognizer
from pynput import keyboard

SAMPLE_RATE = 16000
AUDIO_BLOCK = 8000

class Dictation:
    def __init__(self, model_path, hotkey):
        self.model_path = model_path
        self.hotkey = hotkey
        self.audio_q = queue.Queue()
        self.running = False
        self.stop_event = threading.Event()
        self.current_text = ""
        self.committed = ""
        self.lock = threading.Lock()
        self.model = None
        self.recognizer = None
        self.listener = None

    def audio_callback(self, indata, frames, time_info, status):
        if status:
            print(f"[audio] {status}", file=sys.stderr)
        if self.running:
            self.audio_q.put(bytes(indata))

    def type_text(self, text):
        if not text:
            return
        # Normal desktop keyboard input, not browser scripting or DOM manipulation.
        subprocess.run(["xdotool", "type", "--clearmodifiers", "--delay", "0", text], check=False)

    def erase_chars(self, count):
        if count <= 0:
            return
        subprocess.run(["xdotool", "key", "--clearmodifiers", "--repeat", str(count), "BackSpace"], check=False)

    def normalize(self, text):
        text = re.sub(r"\s+", " ", text.strip())
        if not text:
            return ""
        return text

    def update_live(self, partial):
        partial = self.normalize(partial)
        if not partial:
            return
        with self.lock:
            old = self.current_text
            common = 0
            max_common = min(len(old), len(partial))
            while common < max_common and old[common] == partial[common]:
                common += 1
            if len(old) > common:
                self.erase_chars(len(old) - common)
            suffix = partial[common:]
            if suffix:
                self.type_text(suffix)
            self.current_text = partial

    def final_result(self, text):
        text = self.normalize(text)
        if not text:
            return
        with self.lock:
            # Vosk's final result becomes the committed phrase. Keep it in the field
            # and reset the recognizer so the next phrase starts cleanly.
            self.current_text = text
        self.type_text(" ")
        self.current_text = ""

    def recognition_loop(self):
        while not self.stop_event.is_set():
            try:
                data = self.audio_q.get(timeout=0.2)
            except queue.Empty:
                continue
            if not self.running or self.recognizer is None:
                continue
            if self.recognizer.AcceptWaveform(data):
                try:
                    result = json.loads(self.recognizer.Result()).get("text", "")
                except json.JSONDecodeError:
                    result = ""
                if result:
                    self.final_result(result)
            else:
                try:
                    partial = json.loads(self.recognizer.PartialResult()).get("partial", "")
                except json.JSONDecodeError:
                    partial = ""
                if partial:
                    self.update_live(partial)

    def start(self):
        if self.running:
            return
        if self.model is None:
            print("Ładowanie polskiego modelu Vosk…")
            self.model = Model(self.model_path)
        self.recognizer = KaldiRecognizer(self.model, SAMPLE_RATE)
        self.running = True
        self.current_text = ""
        while not self.audio_q.empty():
            try: self.audio_q.get_nowait()
            except queue.Empty: break
        print("🎤 DYKTOWANIE: ON — mów. F8 wyłącza.")

    def stop(self):
        if not self.running:
            return
        self.running = False
        if self.recognizer:
            try:
                result = json.loads(self.recognizer.FinalResult()).get("text", "")
            except json.JSONDecodeError:
                result = ""
            if result:
                self.final_result(result)
        self.recognizer = None
        self.current_text = ""
        print("🎤 DYKTOWANIE: OFF")

    def toggle(self):
        if self.running:
            self.stop()
        else:
            self.start()

    def run(self):
        print("Szybkie Dyktowanie Xubuntu")
        print("F8 = start/stop | Esc = wyjście")
        print("Rozpoznawanie działa lokalnie na komputerze.")
        threading.Thread(target=self.recognition_loop, daemon=True).start()
        try:
            with sd.RawInputStream(samplerate=SAMPLE_RATE, blocksize=AUDIO_BLOCK,
                                   dtype="int16", channels=1, callback=self.audio_callback):
                self.listener = keyboard.GlobalHotKeys({
                    "<f8>": self.toggle,
                    "<esc>": self.request_exit,
                })
                self.listener.start()
                self.stop_event.wait()
        except Exception as exc:
            print(f"Błąd mikrofonu/klawisza: {exc}", file=sys.stderr)
            return 1
        return 0

    def request_exit(self):
        self.stop_event.set()
        self.running = False
        if self.listener:
            self.listener.stop()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default=os.path.expanduser("~/.local/share/szybkie-dyktowanie/vosk-model-small-pl-0.22"))
    args = parser.parse_args()
    if not os.path.isdir(args.model):
        print("Brak polskiego modelu Vosk. Uruchom ./install.sh", file=sys.stderr)
        return 2
    if subprocess.run(["which", "xdotool"], capture_output=True).returncode != 0:
        print("Brak xdotool. Uruchom ./install.sh", file=sys.stderr)
        return 2
    return Dictation(args.model, "f8").run()

if __name__ == "__main__":
    raise SystemExit(main())
