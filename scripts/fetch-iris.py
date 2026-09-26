#!/usr/bin/env python3
"""Downloads the Iris and Sodium jars used by the shader gametest into bench-mods/<mc>/iris/.

Usage: scripts/fetch-iris.py <minecraft version>

The versions are pinned; each file is checked against the SHA-512 that Modrinth reports.
"""
import hashlib
import json
import pathlib
import sys
import time
import urllib.parse
import urllib.request

PINNED = {
    "26.1.2": {"iris": "1.11.4+26.1-fabric", "sodium": "mc26.1.2-0.9.2-fabric"},
    "26.2": {"iris": "1.11.4+26.2-fabric", "sodium": "mc26.2-0.9.2-fabric"},
    "26.3": {"iris": "1.11.6+26.3-fabric", "sodium": "mc26.3-0.9.2-fabric"},
}
HEADERS = {"User-Agent": "radishsprouts/LightLite (gametest)"}


def get(url):
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=60) as response:
                return response.read()
        except OSError:
            if attempt == 3:
                raise
            time.sleep(2 ** (attempt + 1))


def main():
    mc = sys.argv[1]
    target = pathlib.Path(__file__).resolve().parent.parent / "bench-mods" / mc / "iris"
    target.mkdir(parents=True, exist_ok=True)
    for project, version_number in PINNED[mc].items():
        query = urllib.parse.urlencode({"loaders": '["fabric"]', "game_versions": f'["{mc}"]'})
        versions = json.loads(get(f"https://api.modrinth.com/v2/project/{project}/version?{query}"))
        version = next(v for v in versions if v["version_number"] == version_number)
        file = next((f for f in version["files"] if f["primary"]), version["files"][0])
        data = get(file["url"])
        if hashlib.sha512(data).hexdigest() != file["hashes"]["sha512"]:
            sys.exit(f"SHA-512 mismatch for {file['filename']}")
        (target / file["filename"]).write_bytes(data)
        print(f"{mc}: {file['filename']} (SHA-512 verified)")


if __name__ == "__main__":
    main()
