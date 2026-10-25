#!/usr/bin/env bash
# sign-torrent: a manual step of the EVE Ref Deep Archive workflow. Signs the <ID>.torrent file (05-make-torrent)
# with minisign, run through Docker, producing <ID>.torrent.minisig next to it in SCRATCH_DIR. This is the signature
# 09-publish uploads alongside the torrent, so someone can verify the .torrent file itself before loading it into a
# client, without needing the torrent's own piece hashes.
#
# DRAFT. Manual step: no completion file (lib/workflow.py's MANUAL_STEPS), run by hand after 05-make-torrent and
# before 07-seed. Needs the minisign private key, which is never kept in SCRATCH_DIR or this repo.
#
#     06-sign-torrent.sh
#
# 1. Finds the single *.torrent file 05-make-torrent wrote in SCRATCH_DIR.
# 2. Runs `jedisct1/minisign -S` on it through Docker, mounting SCRATCH_DIR and the key's folder read-only.
#    Always prompts for the key's passphrase interactively: it's never put in the environment or torrents.env.
# 3. If the public key is checked out at ui/public/.well-known/minisign.pub (the one published on the site), verifies
#    the signature against it before finishing, so a bad signature is caught here rather than by someone downloading
#    the torrent.
#
# Writes:
#     SCRATCH_DIR/<ID>.torrent.minisig
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

key_dir="$(cd "$(dirname "$MINISIGN_KEY_PATH")" && pwd)"
key_name="$(basename "$MINISIGN_KEY_PATH")"
PUBLIC_KEY="$SELF_DIR/../../ui/public/.well-known/minisign.pub"

sign_one() {
  local pattern="$1" label="$2" name sig_name sig_file
  shopt -s nullglob
  local matches=("$SCRATCH_DIR"/$pattern)
  if [[ ${#matches[@]} -ne 1 ]]; then
    echo "🔴 Expected exactly one $pattern file in $SCRATCH_DIR (from $label), found ${#matches[@]}" >&2
    exit 1
  fi
  name="$(basename "${matches[0]}")"
  sig_name="$name.minisig"
  sig_file="$SCRATCH_DIR/$sig_name"
  if [[ -f "$sig_file" ]]; then
    echo "🔴 Already signed: $sig_file (delete it by hand to redo)" >&2
    exit 1
  fi

  echo "🔵 Signing"
  echo "$SCRATCH_DIR/$name"
  docker run -it --rm \
    -v "$SCRATCH_DIR:/data" -v "$key_dir:/key:ro" \
    jedisct1/minisign -S -s "/key/$key_name" -m "/data/$name" -x "/data/$sig_name"
  [[ -f "$sig_file" ]] || { echo "🔴 minisign didn't write $sig_file" >&2; exit 1; }
  echo "Written: $sig_file"

  if [[ -f "$PUBLIC_KEY" ]]; then
    echo "🔵 Verifying against $PUBLIC_KEY"
    docker run --rm \
      -v "$SCRATCH_DIR:/data" -v "$(cd "$(dirname "$PUBLIC_KEY")" && pwd):/pub:ro" \
      jedisct1/minisign -V -p "/pub/$(basename "$PUBLIC_KEY")" -m "/data/$name" -x "/data/$sig_name"
  else
    echo "🔴 Public key not found at $PUBLIC_KEY: skipped verification, check it by hand" >&2
  fi
}

sign_one "*.torrent" "05-make-torrent"

echo "✅ Sign OK"
