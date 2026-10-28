"""
torrent_size: estimates the size of a v1 .torrent, and its bdecode token count, from file paths and sizes, without
reading any data.

The .torrent's size doesn't depend on file order: it's a sum of a part per file, a part for the piece hashes, and a
fixed part, so files can be added one at a time and the estimate read at any point. The per-file part mirrors
libtorrent 2.0's create_torrent with the v1_only and modification_time flags (05-make-torrent.py), the format EVE
Ref's torrents use:
- an entry per file in the "files" list: its length, modification time (mtime, always present: every file here has a
  real, non-zero upload time) and path. v1 has no piece alignment: files are packed back to back, with no pad files
  (NOTES.md "Piece alignment"), so a file's own size doesn't change how many pieces it takes -- only the running
  total does.
- "pieces": 20 bytes (SHA-1) for every piece of the running total so far, computed from the total, not per file.
The fixed part (trackers, comment, creator, name, top-level keys, root files) is the FIXED_BYTES allowance, not
worked out. Token counts are within about 1% of libtorrent's.
"""

import math

# Allowance for the parts that don't depend on the selection: trackers, comment, creator, name, top-level keys, and
# the root files (README, checksum file, its signature; lib/workflow.py's root_file_names). GOOD_TRACKERS
# (lib/trackers.py) is a handful of trackers, each about 50 bytes and 2 tokens; the one-line comment, under 100
# bytes. The root files' own piece hashes are the only part that scales with the selection, through the checksum
# file growing with the file count -- at PIECE_SIZE (8 MiB), even a multi-megabyte checksum file only adds a piece
# or two, negligible against this allowance.
FIXED_BYTES = 4096
FIXED_TOKENS = 200


def _str(length):
    """Bencoded length of a string of this many bytes: <length>:<bytes>."""
    return len(str(length)) + 1 + length


def _int(n):
    """Bencoded length of an integer: i<n>e."""
    return len(str(n)) + 2


# Bencoded lengths of the keys.
_LENGTH = _str(len("length"))
_MTIME = _str(len("mtime"))
_PATH = _str(len("path"))


class TorrentSize:
    def __init__(self, piece_size):
        self.piece_size = piece_size
        self.files = 0
        self._total_size = 0
        self._entry_bytes = 0
        self._entry_tokens = 0

    def add(self, path, size, mtime=1):
        """Adds a file: path relative to the torrent's root folder, "/"-separated; mtime only has to be non-zero."""
        parts = [len(p.encode("utf-8")) for p in path.split("/")]
        self.files += 1
        self._total_size += size
        # file entry: d6:lengthi<size>e5:mtimei<mtime>e4:pathl<parts>ee
        self._entry_bytes += 2 + _LENGTH + _int(size) + _MTIME + _int(mtime) + _PATH + 2 + sum(_str(p) for p in parts)
        self._entry_tokens += 10 + len(parts)

    @property
    def pieces(self):
        """Pieces for the running total so far: v1 has no per-file alignment, so this isn't a sum of per-file
        piece counts."""
        return math.ceil(self._total_size / self.piece_size)

    @property
    def bytes(self):
        return FIXED_BYTES + self._entry_bytes + 20 * self.pieces

    @property
    def tokens(self):
        # +2: the top-level "pieces" key and its one (large) string value, counted once, not per file.
        return FIXED_TOKENS + self._entry_tokens + 2
