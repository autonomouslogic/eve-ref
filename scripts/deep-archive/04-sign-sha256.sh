#!/usr/bin/env bash
# sign-sha256: a step of the EVE Ref Deep Archive workflow. Signs the <ID>.sha256 checksum file 03-load wrote with
# minisign, run through Docker, producing <ID>.sha256.minisig next to it in SCRATCH_DIR. Runs before
# 05-make-torrent so both end up inside the torrent, next to the README -- they're never published standalone.
#
# DRAFT. Needs 03-load's completion file, and refuses to run twice (lib/sign.sh, the same rules as lib/workflow.py).
# Needs the minisign private key, which is never kept in SCRATCH_DIR or this repo, and its passphrase, typed in.
#
#     04-sign-sha256.sh
#
# Verifies the signature against the public key in this checkout before finishing, so a bad signature is caught
# here rather than inside the torrent. 05-make-torrent verifies it again before copying it in.
#
# Writes:
#     SCRATCH_DIR/<ID>.sha256.minisig
#     SCRATCH_DIR/04-sign-sha256.done   completion file
#
# Environment (or torrents.env, see lib/workflow.py): SCRATCH_DIR, MINISIGN_KEY_PATH (lib/sign.sh).
#
# Needs Docker and python3.

# shellcheck source=lib/sign.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/sign.sh"

sign_step 03-load "<ID>.sha256"
