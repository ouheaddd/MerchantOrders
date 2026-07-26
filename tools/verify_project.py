#!/usr/bin/env python3
"""Offline structural validation for the Merchant Orders source archive."""
from __future__ import annotations

import json
import struct
import sys
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def check_required() -> None:
    required = [
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties",
        "src/main/templates/META-INF/neoforge.mods.toml",
        "src/main/resources/merchant_orders.mixins.json",
        "src/main/java/com/overyourhead/merchant_orders/MerchantOrdersMod.java",
        "src/main/resources/assets/merchant_orders/textures/gui/order_terminal.png",
        "src/main/resources/assets/merchant_orders/sounds/ui/order_add.ogg",
    ]
    for rel in required:
        if not (ROOT / rel).is_file():
            fail(f"missing required file: {rel}")


def check_json() -> None:
    for path in ROOT.rglob("*.json"):
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001
            fail(f"invalid JSON {path.relative_to(ROOT)}: {exc}")


def check_png() -> None:
    signature = b"\x89PNG\r\n\x1a\n"
    for path in ROOT.rglob("*.png"):
        data = path.read_bytes()
        if len(data) < 24 or data[:8] != signature:
            fail(f"invalid PNG signature: {path.relative_to(ROOT)}")
            continue
        width, height = struct.unpack(">II", data[16:24])
        if width <= 0 or height <= 0:
            fail(f"invalid PNG dimensions: {path.relative_to(ROOT)}")


def check_ogg() -> None:
    for path in ROOT.rglob("*.ogg"):
        if not path.read_bytes().startswith(b"OggS"):
            fail(f"invalid OGG signature: {path.relative_to(ROOT)}")


def check_wrapper() -> None:
    path = ROOT / "gradle/wrapper/gradle-wrapper.jar"
    try:
        with ZipFile(path) as archive:
            if "org/gradle/wrapper/GradleWrapperMain.class" not in archive.namelist():
                fail("wrapper JAR lacks GradleWrapperMain")
    except Exception as exc:  # noqa: BLE001
        fail(f"invalid wrapper JAR: {exc}")


def check_text_markers() -> None:
    combined = "\n".join(
        path.read_text(encoding="utf-8", errors="replace")
        for path in ROOT.rglob("*.java")
    )
    for marker in ("com.overyourhead.merchant_orders", "merchant_orders", "overyourhead"):
        if marker not in combined:
            fail(f"missing source marker: {marker}")


def main() -> int:
    check_required()
    check_json()
    check_png()
    check_ogg()
    check_wrapper()
    check_text_markers()
    if ERRORS:
        print("Offline validation FAILED")
        for error in ERRORS:
            print(f" - {error}")
        return 1
    java_count = sum(1 for _ in ROOT.rglob("*.java"))
    json_count = sum(1 for _ in ROOT.rglob("*.json"))
    png_count = sum(1 for _ in ROOT.rglob("*.png"))
    ogg_count = sum(1 for _ in ROOT.rglob("*.ogg"))
    print("Offline validation passed")
    print(f"Java: {java_count}; JSON: {json_count}; PNG: {png_count}; OGG: {ogg_count}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
