"""
torrent_size: estimates the size of a hybrid v1+v2 .torrent, and its bdecode token count, from file paths and sizes,
without reading any data.

The .torrent's size doesn't depend on file order: it's a sum of a part per file, a part per directory and a fixed
part, so files can be added one at a time and the estimate read at any point. The per-file and per-directory parts
mirror libtorrent 2.0's create_torrent:
- v1 "files": an entry per file, plus a pad file entry (".pad/<n>") when the size isn't a multiple of the piece size;
- v1 "pieces": 20 bytes per piece. Hybrid torrents align every file to a piece, so each file has its own pieces;
- v2 "file tree": a leaf per file, with a 32-byte pieces root unless it's empty, and a node per directory;
- v2 "piece layers": 32 bytes per piece, for files bigger than one piece.
They matched libtorrent's output byte for byte in tests. The fixed part (trackers, comment, name, top-level keys,
root files) is the FIXED_BYTES allowance, not worked out. Token counts are within about 1% of libtorrent's.
"""

import math

# Allowance for the parts that don't depend on the selection: trackers, comment, creator, name, top-level keys, and the
# .sha256 and README files at the root of the torrent.
# About 1 KB with the 7 trackers in trackers.txt and the longest PLAN 2.4 comment; rounded up for more trackers.
FIXED_BYTES = 4096
FIXED_TOKENS = 100


def _str(length):
    """Bencoded length of a string of this many bytes: <length>:<bytes>."""
    return len(str(length)) + 1 + length


def _int(n):
    """Bencoded length of an integer: i<n>e."""
    return len(str(n)) + 2


# Bencoded lengths of the keys and fixed values.
_LENGTH = _str(len("length"))
_PATH = _str(len("path"))
_ATTR_P = _str(len("attr")) + _str(len("p"))
_PAD_DIR = _str(len(".pad"))
_EMPTY_KEY = _str(0)
_PIECES_ROOT = _str(len("pieces root")) + _str(32)
_LAYER_KEY = _str(32)


class TorrentSize:
    def __init__(self, piece_size):
        self.piece_size = piece_size
        self.files = 0
        self.pieces = 0
        self._bytes = 0
        self._tokens = 0
        self._dirs = set()

    def add(self, path, size):
        """Adds a file: path relative to the torrent's root folder, "/"-separated."""
        parts = [len(p.encode("utf-8")) for p in path.split("/")]
        pieces = math.ceil(size / self.piece_size)
        self.files += 1
        self.pieces += pieces
        # v1 file entry: d6:lengthi<size>e4:pathl<parts>ee
        self._bytes += 2 + _LENGTH + _int(size) + _PATH + 2 + sum(_str(p) for p in parts)
        self._tokens += 8 + len(parts)
        # v1 pad file entry: d4:attr1:p6:lengthi<pad>e4:pathl4:.pad<n>:<pad>ee
        pad = -size % self.piece_size
        if pad:
            self._bytes += 2 + _ATTR_P + _LENGTH + _int(pad) + _PATH + 2 + _PAD_DIR + _str(len(str(pad)))
            self._tokens += 10
        # v1 piece hashes
        self._bytes += 20 * pieces
        # v2 file tree leaf: <name>d0:d6:lengthi<size>e11:pieces root32:<root>ee
        self._bytes += _str(parts[-1]) + 2 + _EMPTY_KEY + 2 + _LENGTH + _int(size)
        self._tokens += 8
        if size:
            self._bytes += _PIECES_ROOT
            self._tokens += 2
        # v2 piece layer: 32:<root><n>:<hashes>
        if size > self.piece_size:
            self._bytes += _LAYER_KEY + _str(32 * pieces)
            self._tokens += 2
        # v2 file tree directory nodes: <name>d...e
        path_parts = path.split("/")
        for i in range(1, len(path_parts)):
            directory = "/".join(path_parts[:i])
            if directory not in self._dirs:
                self._dirs.add(directory)
                self._bytes += _str(parts[i - 1]) + 2
                self._tokens += 3

    @property
    def bytes(self):
        return FIXED_BYTES + self._bytes

    @property
    def tokens(self):
        return FIXED_TOKENS + self._tokens
