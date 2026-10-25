#!/usr/bin/env bash
# sign: step 04 of the EVE Ref Deep Archive workflow (manual). Signs the <ID>.sha256 checksum file 03-load wrote
# with minisign, run through Docker, producing <ID>.sha256.minisig next to it.
#
# DRAFT. Manual step: no completion file (lib/workflow.py's MANUAL_STEPS), run by hand after 03-load and before
# 05-make-torrent. Needs the minisign private key, which is never kept in SCRATCH_DIR or this repo.
#
#     04-sign.sh
#
# 1. Finds the single *.sha256 file 03-load wrote in SCRATCH_DIR.
# 2. Runs `jedisct1/minisign -S` on it through Docker, mounting SCRATCH_DIR and the key's folder read-only.
#    Always prompts for the key's passphrase interactively: it's never put in the environment or torrents.env.
# 3. If the public key is checked out at ui/public/.well-known/minisign.pub (the one published on the site), verifies
#    the signature against it before finishing, so a bad signature is caught here rather than by someone downloading
#    the torrent.
#
# Writes:
#     SCRATCH_DIR/<ID>.sha256.minisig
#
# Environment (or torrents.env, see lib/workflow.py):
#     SCRATCH_DIR
#     MINISIGN_KEY_PATH   path to the minisign private key file (e.g. everef.key), kept outside this repo
#
# Needs Docker.

set -euo pipefail

SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${TORRENTS_ENV:-$SELF_DIR/torrents.env}"

# Environment already set wins over torrents.env (lib/workflow.py's load_env does the same).
PRE_SCRATCH_DIR="${SCRATCH_DIR-}"
PRE_MINISIGN_KEY_PATH="${MINISIGN_KEY_PATH-}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi
SCRATCH_DIR="${PRE_SCRATCH_DIR:-${SCRATCH_DIR:-}}"
MINISIGN_KEY_PATH="${PRE_MINISIGN_KEY_PATH:-${MINISIGN_KEY_PATH:-}}"

: "${SCRATCH_DIR:?SCRATCH_DIR is not set (environment or torrents.env)}"
: "${MINISIGN_KEY_PATH:?MINISIGN_KEY_PATH is not set (environment or torrents.env): path to the minisign private key}"
[[ -d "$SCRATCH_DIR" ]] || { echo "🔴 SCRATCH_DIR does not exist: $SCRATCH_DIR" >&2; exit 1; }
[[ -f "$MINISIGN_KEY_PATH" ]] || { echo "🔴 MINISIGN_KEY_PATH does not exist: $MINISIGN_KEY_PATH" >&2; exit 1; }

shopt -s nullglob
sha256_files=("$SCRATCH_DIR"/*.sha256)
if [[ ${#sha256_files[@]} -ne 1 ]]; then
  echo "🔴 Expected exactly one *.sha256 file in $SCRATCH_DIR (from 03-load), found ${#sha256_files[@]}" >&2
  exit 1
fi
sha256_file="${sha256_files[0]}"
name="$(basename "$sha256_file")"
sig_name="$name.minisig"
sig_file="$SCRATCH_DIR/$sig_name"
if [[ -f "$sig_file" ]]; then
  echo "🔴 Already signed: $sig_file (delete it by hand to redo)" >&2
  exit 1
fi

key_dir="$(cd "$(dirname "$MINISIGN_KEY_PATH")" && pwd)"
key_name="$(basename "$MINISIGN_KEY_PATH")"

echo "🔵 Signing"
echo "$sha256_file"
docker run -it --rm \
  -v "$SCRATCH_DIR:/data" -v "$key_dir:/key:ro" \
  jedisct1/minisign -S -s "/key/$key_name" -m "/data/$name" -x "/data/$sig_name"
[[ -f "$sig_file" ]] || { echo "🔴 minisign didn't write $sig_file" >&2; exit 1; }
echo "Written: $sig_file"

PUBLIC_KEY="$SELF_DIR/../../ui/public/.well-known/minisign.pub"
if [[ -f "$PUBLIC_KEY" ]]; then
  echo "🔵 Verifying against $PUBLIC_KEY"
  docker run --rm \
    -v "$SCRATCH_DIR:/data" -v "$(cd "$(dirname "$PUBLIC_KEY")" && pwd):/pub:ro" \
    jedisct1/minisign -V -p "/pub/$(basename "$PUBLIC_KEY")" -m "/data/$name" -x "/data/$sig_name"
else
  echo "🔴 Public key not found at $PUBLIC_KEY: skipped verification, check it by hand" >&2
fi

echo "✅ Sign OK: $sig_file"
