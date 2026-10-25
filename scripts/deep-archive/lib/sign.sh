# sign.sh: shared by the EVE Ref Deep Archive signing steps (04-sign-sha256.sh, 06-sign-torrent.sh). Sourced, not
# run. Does for a bash step what lib/workflow.py's Workflow does for the Python ones: reads torrents.env, takes
# SCRATCH_DIR/log/.lock, refuses to start if the previous scripted step hasn't completed or this one already has,
# logs to SCRATCH_DIR/log/<NN>-<step>-<UTC time>.log, and writes SCRATCH_DIR/<NN>-<step>.done as the last thing it
# does.
#
#     sign_step <previous step> <file name>
#
# e.g. sign_step 03-load "<ID>.sha256", with <ID> filled in from 01-find.done. The step's own name comes from its
# script's file name; tests/test_workflow.py checks the previous step matches lib/workflow.py's STEPS.
#
# Signs SCRATCH_DIR/<file name> with `jedisct1/minisign -S` through Docker, mounting SCRATCH_DIR and the key's
# folder read-only, into SCRATCH_DIR/<file name>.minisig. Always prompts for the key's passphrase interactively: it's
# never put in the environment or torrents.env. Then verifies the signature against the public key in this
# checkout, ui/public/.well-known/minisign.pub (the one published on the site).
#
# Environment (or torrents.env, see lib/workflow.py):
#     SCRATCH_DIR
#     MINISIGN_KEY_PATH   path to the minisign private key file (e.g. everef.key), kept outside this repo

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SELF_DIR="$(dirname "$LIB_DIR")"
STEP_NAME="$(basename "$0" .sh)"
PUBLIC_KEY="$SELF_DIR/../../ui/public/.well-known/minisign.pub"
MINISIGN_IMAGE="jedisct1/minisign"

die() {
  echo "🔴 $1" >&2
  exit 1
}

# Environment already set wins over torrents.env (lib/workflow.py's load_env does the same).
load_settings() {
  local env_file="${TORRENTS_ENV:-$SELF_DIR/torrents.env}"
  local pre_scratch_dir="${SCRATCH_DIR-}" pre_minisign_key_path="${MINISIGN_KEY_PATH-}"
  if [[ -f "$env_file" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$env_file"
    set +a
  fi
  SCRATCH_DIR="${pre_scratch_dir:-${SCRATCH_DIR:-}}"
  MINISIGN_KEY_PATH="${pre_minisign_key_path:-${MINISIGN_KEY_PATH:-}}"
  [[ -n "$SCRATCH_DIR" ]] || die "SCRATCH_DIR is not set (environment or torrents.env)"
  [[ -n "$MINISIGN_KEY_PATH" ]] || die "MINISIGN_KEY_PATH is not set (environment or torrents.env): path to the minisign private key"
  [[ -d "$SCRATCH_DIR" ]] || die "SCRATCH_DIR does not exist: $SCRATCH_DIR"
  [[ -f "$MINISIGN_KEY_PATH" ]] || die "MINISIGN_KEY_PATH does not exist: $MINISIGN_KEY_PATH"
  SCRATCH_DIR="$(cd "$SCRATCH_DIR" && pwd -P)"
}

# The torrent ID 01-find chose, from its completion file.
read_torrent_id() {
  python3 -I -c 'import json, sys; print(json.load(open(sys.argv[1]))["torrent_id"])' "$SCRATCH_DIR/01-find.done"
}

# Same checks as Workflow.begin: one step at a time, never twice, never before the previous one.
begin_step() {
  local previous="$1"
  mkdir -p "$SCRATCH_DIR/log"
  exec 9>"$SCRATCH_DIR/log/.lock"
  flock -n 9 || die "Another step is running in this SCRATCH_DIR ($SCRATCH_DIR/log/.lock is locked)"
  DONE_FILE="$SCRATCH_DIR/$STEP_NAME.done"
  [[ ! -f "$DONE_FILE" ]] || die "$STEP_NAME already completed, refusing to rerun. To redo it on purpose, delete $DONE_FILE and every later step's by hand"
  [[ -f "$SCRATCH_DIR/$previous.done" ]] || die "Earlier step $previous has not completed yet ($SCRATCH_DIR/$previous.done missing)"
  TORRENT_ID="$(read_torrent_id)"
  # Keep the terminal on fds 3 and 4 for minisign's passphrase prompt, which never goes through tee or the log.
  exec 3>&1 4>&2
  exec > >(tee -a "$SCRATCH_DIR/log/$STEP_NAME-$(date -u +%Y%m%dT%H%M%SZ).log") 2>&1
  echo "🚀 $STEP_NAME: $TORRENT_ID"
}

# The completion file, in the same format as Workflow.complete.
complete_step() {
  local name="$1" sig_name="$2" tmp="$DONE_FILE.tmp"
  cat >"$tmp" <<EOF
{
  "completed_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "step": "${STEP_NAME#[0-9][0-9]-}",
  "summary": {
    "signature": "$sig_name",
    "signed": "$name"
  },
  "torrent_id": "$TORRENT_ID"
}
EOF
  mv "$tmp" "$DONE_FILE"
}

sign_step() {
  local previous="$1" name_template="$2" name sig_name file sig_file key_dir key_name
  load_settings
  begin_step "$previous"
  name="${name_template//<ID>/$TORRENT_ID}"
  sig_name="$name.minisig"
  file="$SCRATCH_DIR/$name"
  sig_file="$SCRATCH_DIR/$sig_name"
  key_dir="$(cd "$(dirname "$MINISIGN_KEY_PATH")" && pwd)"
  key_name="$(basename "$MINISIGN_KEY_PATH")"

  [[ -f "$file" ]] || die "Not found: $file (from $previous)"
  [[ ! -f "$sig_file" ]] || die "Already signed: $sig_file (delete it by hand to redo)"
  [[ -f "$PUBLIC_KEY" ]] || die "Public key not found: $PUBLIC_KEY"

  echo "🔵 Signing"
  echo "$file"
  docker run -it --rm \
    -v "$SCRATCH_DIR:/data" -v "$key_dir:/key:ro" \
    "$MINISIGN_IMAGE" -S -s "/key/$key_name" -m "/data/$name" -x "/data/$sig_name" >&3 2>&4
  [[ -f "$sig_file" ]] || die "minisign didn't write $sig_file"
  echo "Written: $sig_file"

  echo "🔵 Verifying against $PUBLIC_KEY"
  docker run --rm \
    -v "$SCRATCH_DIR:/data:ro" -v "$(cd "$(dirname "$PUBLIC_KEY")" && pwd):/pub:ro" \
    "$MINISIGN_IMAGE" -V -p "/pub/$(basename "$PUBLIC_KEY")" -m "/data/$name" -x "/data/$sig_name"

  complete_step "$name" "$sig_name"
  echo "✅ $STEP_NAME OK for $TORRENT_ID"
}
