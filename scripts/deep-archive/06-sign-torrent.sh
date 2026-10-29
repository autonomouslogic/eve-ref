#!/usr/bin/env bash
# sign-torrent: a step of the EVE Ref Deep Archive workflow. Signs the <ID>.torrent file (05-make-torrent) with
# minisign, run through Docker, producing <ID>.torrent.minisig next to it in SCRATCH_DIR. This is the signature
# 09-publish uploads alongside the torrent, so someone can verify the .torrent file itself before loading it into a
# client, without needing the torrent's own piece hashes.
#
# DRAFT. Needs 05-make-torrent's completion file, and refuses to run twice (lib/sign.sh, the same rules as
# lib/workflow.py). Needs the minisign private key, which is never kept in SCRATCH_DIR or this repo, and its
# passphrase, typed in.
#
#     06-sign-torrent.sh
#
# Verifies the signature against the public key in this checkout before finishing, so a bad signature is caught
# here rather than by someone downloading the torrent. 09-publish verifies it again before uploading it.
#
# Writes:
#     SCRATCH_DIR/<ID>.torrent.minisig
#     SCRATCH_DIR/06-sign-torrent.done   completion file
#
# Environment (or torrents.env, see lib/workflow.py): SCRATCH_DIR, MINISIGN_KEY_PATH (lib/sign.sh).
#
# Needs Docker and python3.

# shellcheck source=lib/sign.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/sign.sh"

sign_step 05-make-torrent "<ID>.torrent"
