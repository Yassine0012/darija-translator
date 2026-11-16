"""Command-line client for the Darija translator REST service (standard library only).

Usage:
    python translate_client.py "Good morning"      # one-shot
    python translate_client.py                     # interactive mode (empty line to quit)

Configuration through environment variables:
    API_URL       default http://localhost:8080/api/translate
    API_USER      default admin
    API_PASSWORD  required
"""
import base64
import json
import os
import sys
import urllib.error
import urllib.request

API_URL = os.environ.get("API_URL", "http://localhost:8080/api/translate")
API_USER = os.environ.get("API_USER", "admin")
API_PASSWORD = os.environ.get("API_PASSWORD", "")


def translate(text: str, source: str = "") -> str:
    token = base64.b64encode(f"{API_USER}:{API_PASSWORD}".encode("utf-8")).decode("ascii")
    request = urllib.request.Request(
        API_URL,
        data=json.dumps({"text": text, "source": source}).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": f"Basic {token}"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.loads(response.read().decode("utf-8"))["translation"]
    except urllib.error.HTTPError as e:
        if e.code == 401:
            raise SystemExit("Authentication failed: check API_USER and API_PASSWORD.")
        raise SystemExit(f"Server error: HTTP {e.code}")
    except urllib.error.URLError as e:
        raise SystemExit(f"Cannot reach the server at {API_URL}: {e.reason}")


def main() -> None:
    # Make Arabic output work on Windows consoles.
    sys.stdout.reconfigure(encoding="utf-8")

    if len(sys.argv) > 1:
        print(translate(" ".join(sys.argv[1:])))
        return

    print("Darija translator - type text and press Enter (empty line to quit).")
    while True:
        text = input("> ").strip()
        if not text:
            break
        print(translate(text))


if __name__ == "__main__":
    main()
