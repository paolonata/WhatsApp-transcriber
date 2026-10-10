#!/usr/bin/env python3
"""
Minimal HTTP server wrapping whisper.cpp's whisper-cli for the "Trascrivi
WhatsApp" Android app. Deliberately dependency-free (stdlib only) to keep
the Docker image small and the build reliable on a Raspberry Pi.

API:
  GET  /health      -> 200 "ok", for a quick reachability check
  POST /transcribe   -> body: raw audio bytes (whatever format WhatsApp used)
                         headers: Authorization: Bearer <token> (required)
                                  X-Model-Tier: fast|precise (default: fast)
                         response: {"text": "..."} or {"error": "..."}

Everything happens on this one device, on your home network. Nothing here
ever talks to the internet except the one-time model download.
"""
import json
import os
import subprocess
import tempfile
import threading
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = 8765
MODELS_DIR = "/data/models"
WHISPER_BIN = "/opt/whisper.cpp/build/bin/whisper-cli"
OPTIONS_PATH = "/data/options.json"

transcribe_lock = threading.Lock()


def load_config():
    with open(OPTIONS_PATH, "r", encoding="utf-8") as f:
        options = json.load(f)
    return {
        "api_token": options["api_token"],
        "fast_model": options["fast_model"],
        "precise_model": options["precise_model"],
        "threads": str(options.get("threads", 4)),
    }


def model_path(model_name: str) -> str:
    return os.path.join(MODELS_DIR, f"ggml-{model_name}.bin")


def ensure_model_downloaded(model_name: str) -> None:
    dest = model_path(model_name)
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        return
    print(f"[whisper-transcriber] downloading model {model_name}...", flush=True)
    url = f"https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-{model_name}.bin"
    part = dest + ".part"
    subprocess.run(["curl", "-L", "--fail", "-o", part, url], check=True)
    os.rename(part, dest)
    print(f"[whisper-transcriber] model {model_name} ready", flush=True)


def transcribe(audio_bytes: bytes, model_name: str, threads: str) -> str:
    with tempfile.TemporaryDirectory() as tmp:
        input_path = os.path.join(tmp, "input.audio")
        wav_path = os.path.join(tmp, "input.wav")
        out_prefix = os.path.join(tmp, "output")

        with open(input_path, "wb") as f:
            f.write(audio_bytes)

        subprocess.run(
            ["ffmpeg", "-y", "-i", input_path, "-ar", "16000", "-ac", "1", "-c:a", "pcm_s16le", wav_path],
            check=True,
            capture_output=True,
        )

        with transcribe_lock:
            subprocess.run(
                [
                    WHISPER_BIN,
                    "-m", model_path(model_name),
                    "-f", wav_path,
                    "-l", "it",
                    "-t", threads,
                    "-nt",
                    "-otxt",
                    "-of", out_prefix,
                ],
                check=True,
                capture_output=True,
            )

        with open(out_prefix + ".txt", "r", encoding="utf-8") as f:
            return f.read().strip()


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _authorized(self, config: dict) -> bool:
        expected = f"Bearer {config['api_token']}"
        return self.headers.get("Authorization") == expected

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {"status": "ok"})
        else:
            self._send_json(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/transcribe":
            self._send_json(404, {"error": "not found"})
            return

        try:
            config = load_config()
        except Exception:
            self._send_json(500, {"error": "Configurazione del server non leggibile"})
            return

        if not self._authorized(config):
            self._send_json(401, {"error": "Token non valido"})
            return

        content_length = int(self.headers.get("Content-Length", 0))
        if content_length <= 0:
            self._send_json(400, {"error": "Corpo della richiesta vuoto"})
            return
        audio_bytes = self.rfile.read(content_length)

        tier = self.headers.get("X-Model-Tier", "fast")
        model_name = config["precise_model"] if tier == "precise" else config["fast_model"]

        try:
            ensure_model_downloaded(model_name)
            text = transcribe(audio_bytes, model_name, config["threads"])
            self._send_json(200, {"text": text})
        except Exception as e:
            traceback.print_exc()
            self._send_json(500, {"error": f"Trascrizione fallita: {e}"})

    def log_message(self, format, *args):
        print(f"[whisper-transcriber] {self.address_string()} - {format % args}", flush=True)


def main():
    os.makedirs(MODELS_DIR, exist_ok=True)
    config = load_config()
    ensure_model_downloaded(config["fast_model"])
    ensure_model_downloaded(config["precise_model"])

    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"[whisper-transcriber] listening on :{PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
