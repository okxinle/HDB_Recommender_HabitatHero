#!/usr/bin/env python3
"""Fetch Singapore POIs from Overpass (OSM) and load into HabitatHero backend."""

from __future__ import annotations

import argparse
import json
import sys
import time
from typing import Dict, List, Optional

import requests

OVERPASS_ENDPOINTS = [
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
]
DEFAULT_BACKEND_URL = "http://localhost:8080/api/admin/pois/load"
DEFAULT_AUTH_URL = "http://localhost:8080/api/auth/login"

OVERPASS_FILTERS = [
        '["amenity"="school"]',
        '["amenity"="food_court"]',
        '["amenity"="hawker_centre"]',
        '["shop"="supermarket"]',
        '["leisure"="park"]',
        '["amenity"="hospital"]',
        '["leisure"="playground"]',
]


def category_from_tags(tags: Dict[str, str]) -> Optional[str]:
    amenity = tags.get("amenity", "").strip().lower()
    shop = tags.get("shop", "").strip().lower()
    leisure = tags.get("leisure", "").strip().lower()

    if amenity == "school":
        return "SCHOOL"
    if amenity in {"food_court", "hawker_centre"}:
        return "HAWKER_CENTRE"
    if shop == "supermarket":
        return "SUPERMARKET"
    if leisure == "park":
        return "PARK"
    if amenity == "hospital":
        return "HOSPITAL"
    if leisure == "playground":
        return "PLAYGROUND"

    return None


def build_overpass_query(node_filter: str) -> str:
    return f"""
[out:json][timeout:180];
area["name"="Singapore"]->.searchArea;
node{node_filter}(area.searchArea);
out body;
""".strip()


def fetch_single_filter_with_retries(
    session: requests.Session,
    node_filter: str,
    max_attempts: int,
) -> Dict:
    query = build_overpass_query(node_filter)
    last_error: Optional[Exception] = None

    for attempt in range(1, max_attempts + 1):
        endpoint = OVERPASS_ENDPOINTS[(attempt - 1) % len(OVERPASS_ENDPOINTS)]
        try:
            response = session.post(
                endpoint,
                data={"data": query},
                timeout=240,
            )
            response.raise_for_status()
            return response.json()
        except requests.RequestException as exc:
            last_error = exc
            wait_seconds = min(20, 2 * attempt)
            print(
                f"Overpass attempt {attempt}/{max_attempts} failed for filter {node_filter} on {endpoint}: {exc}"
            )
            if attempt < max_attempts:
                print(f"Retrying in {wait_seconds}s...")
                time.sleep(wait_seconds)

    raise RuntimeError(f"Overpass failed for filter {node_filter}: {last_error}")


def fetch_overpass_data(session: requests.Session, max_attempts: int) -> Dict:
    merged_elements: List[Dict] = []
    for node_filter in OVERPASS_FILTERS:
        print(f"Fetching Overpass filter: node{node_filter}")
        part = fetch_single_filter_with_retries(session, node_filter, max_attempts)
        merged_elements.extend(part.get("elements", []))

    return {"elements": merged_elements}


def build_poi_payload(overpass_json: Dict) -> List[Dict]:
    elements = overpass_json.get("elements", [])
    payload: List[Dict] = []

    seen = set()
    for element in elements:
        if element.get("type") != "node":
            continue

        lat = element.get("lat")
        lon = element.get("lon")
        tags = element.get("tags", {})

        if lat is None or lon is None or not isinstance(tags, dict):
            continue

        category = category_from_tags(tags)
        if category is None:
            continue

        name = (tags.get("name") or "").strip()
        if not name:
            name = f"Unnamed {category}"

        # Keep de-duplication strict enough to avoid duplicate inserts from overlapping tags.
        dedupe_key = (round(float(lat), 7), round(float(lon), 7), category, name)
        if dedupe_key in seen:
            continue
        seen.add(dedupe_key)

        payload.append(
            {
                "name": name,
                "category": category,
                "latitude": float(lat),
                "longitude": float(lon),
            }
        )

    return payload


def post_payload(
    session: requests.Session,
    backend_url: str,
    payload: List[Dict],
    chunk_size: int,
    pause_seconds: float,
    token: str,
) -> None:
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}",
    }

    if chunk_size <= 0:
        # Single full payload POST
        response = session.post(backend_url, json=payload, headers=headers, timeout=600)
        response.raise_for_status()
        print("Backend response:", response.text)
        return

    total = len(payload)
    uploaded = 0
    batch_num = 0

    for start in range(0, total, chunk_size):
        batch_num += 1
        chunk = payload[start : start + chunk_size]
        response = session.post(backend_url, json=chunk, headers=headers, timeout=600)
        response.raise_for_status()
        uploaded += len(chunk)
        print(f"Uploaded batch {batch_num}: {len(chunk)} records (total {uploaded}/{total})")

        if pause_seconds > 0 and uploaded < total:
            time.sleep(pause_seconds)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch all Singapore POIs from Overpass and load into HabitatHero backend."
    )
    parser.add_argument(
        "--backend-url",
        default=DEFAULT_BACKEND_URL,
        help=f"POI loader endpoint (default: {DEFAULT_BACKEND_URL})",
    )
    parser.add_argument(
        "--auth-url",
        default=DEFAULT_AUTH_URL,
        help=f"Auth login endpoint for obtaining JWT (default: {DEFAULT_AUTH_URL})",
    )
    parser.add_argument(
        "--token",
        default="",
        help="Existing JWT token. If provided, login step is skipped.",
    )
    parser.add_argument(
        "--email",
        default="",
        help="Login email for JWT fetch from /api/auth/login.",
    )
    parser.add_argument(
        "--password",
        default="",
        help="Login password for JWT fetch from /api/auth/login.",
    )
    parser.add_argument(
        "--out",
        default="",
        help="Optional path to save fetched payload as JSON before posting.",
    )
    parser.add_argument(
        "--chunk-size",
        type=int,
        default=0,
        help="Set >0 to POST in chunks (e.g. 2000). Default 0 sends one full payload POST.",
    )
    parser.add_argument(
        "--pause-seconds",
        type=float,
        default=0.0,
        help="Pause between chunk uploads when --chunk-size > 0.",
    )
    parser.add_argument(
        "--overpass-attempts",
        type=int,
        default=6,
        help="Retry attempts per Overpass filter query (default: 6).",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    try:
        with requests.Session() as session:
            token = args.token.strip()
            if not token:
                if not args.email or not args.password:
                    print(
                        "Authentication required: provide --token OR both --email and --password."
                    )
                    return 1

                login_response = session.post(
                    args.auth_url,
                    json={"email": args.email, "password": args.password},
                    timeout=60,
                )
                login_response.raise_for_status()
                login_data = login_response.json()
                token = str(login_data.get("token", "")).strip()
                if not token:
                    print("Login succeeded but no token was returned.")
                    return 1

                print("Authenticated successfully via /api/auth/login")
            elif token.count(".") != 2:
                print(
                    "Provided --token does not look like a JWT. "
                    "Use a real auth token from /api/auth/login, not the SECRET_KEY."
                )
                return 1

            print("Fetching POIs from Overpass for Singapore...")
            overpass_json = fetch_overpass_data(session, max_attempts=max(1, args.overpass_attempts))

            print("Transforming Overpass response to PointOfInterest payload...")
            payload = build_poi_payload(overpass_json)
            print(f"Built payload with {len(payload)} POIs")

            if args.out:
                with open(args.out, "w", encoding="utf-8") as f:
                    json.dump(payload, f, ensure_ascii=False, indent=2)
                print(f"Saved payload to: {args.out}")

            print(f"Posting payload to {args.backend_url}...")
            post_payload(
                session=session,
                backend_url=args.backend_url,
                payload=payload,
                chunk_size=args.chunk_size,
                pause_seconds=args.pause_seconds,
                token=token,
            )

            print("Done.")
            return 0

    except requests.HTTPError as exc:
        print(f"HTTP error: {exc}")
        if exc.response is not None:
            print("Response body:")
            print(exc.response.text)
        return 1
    except requests.RequestException as exc:
        print(f"Network/request error: {exc}")
        return 1
    except Exception as exc:  # pragma: no cover
        print(f"Unexpected error: {exc}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
