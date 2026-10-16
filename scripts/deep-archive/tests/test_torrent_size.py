"""
Tests for lib/torrent_size.py: TorrentSize, the .torrent size estimate 01-find uses to pick its cutoff without
reading any data (PLAN 2.1, 2.4).

TorrentSizeTest covers the class on its own: it never shrinks as files are added, and an empty torrent is just the
fixed allowance.

RealTorrentTest is the real check: it builds an actual hybrid v1+v2 .torrent with libtorrent (small files chosen to
exercise v1 pad files, v2 piece layers for multi-piece files, and nested directories), and compares its real bencoded
size against TorrentSize's estimate for the same files. The estimate only has to be in the right ballpark (it's a
limit check, not an exact prediction), so the tolerance here is generous -- but it must never undershoot the real
size, since 01-find relies on it to stay under MAX_TORRENT_BYTES.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive). The real-torrent
test is skipped if the libtorrent Python bindings aren't installed (they aren't required for the other tests, or for
most of the suite; see README's Requirements).

Needs libtorrent 2.x for RealTorrentTest; skipped otherwise.
"""

import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
from torrent_size import FIXED_BYTES, TorrentSize  # noqa: E402

try:
    import libtorrent as lt

    LIBTORRENT = True
except ImportError:
    LIBTORRENT = False

PIECE_SIZE = 16 * 1024  # small, so hashing a handful of KB of real files is instant


class TorrentSizeTest(unittest.TestCase):
    def test_empty_is_just_the_fixed_allowance(self):
        torrent = TorrentSize(PIECE_SIZE)
        self.assertEqual(torrent.bytes, FIXED_BYTES)
        self.assertEqual(torrent.pieces, 0)
        self.assertEqual(torrent.files, 0)

    def test_adding_a_file_never_shrinks_the_estimate(self):
        torrent = TorrentSize(PIECE_SIZE)
        before = torrent.bytes
        torrent.add("a/b.csv", 12345)
        self.assertGreater(torrent.bytes, before)

    def test_a_bigger_file_needs_more_pieces(self):
        small = TorrentSize(PIECE_SIZE)
        small.add("a.bin", PIECE_SIZE)
        big = TorrentSize(PIECE_SIZE)
        big.add("a.bin", PIECE_SIZE * 10)
        self.assertGreater(big.pieces, small.pieces)
        self.assertGreater(big.bytes, small.bytes)

    def test_a_longer_path_costs_more_than_a_shorter_one(self):
        short = TorrentSize(PIECE_SIZE)
        short.add("a.bin", 100)
        long_path = TorrentSize(PIECE_SIZE)
        long_path.add("a/much/deeper/directory/structure/a.bin", 100)
        self.assertGreater(long_path.bytes, short.bytes)

    def test_a_shared_directory_is_only_counted_once(self):
        separate_dirs = TorrentSize(PIECE_SIZE)
        separate_dirs.add("a/one.bin", 100)
        separate_dirs.add("b/two.bin", 100)
        shared_dir = TorrentSize(PIECE_SIZE)
        shared_dir.add("a/one.bin", 100)
        shared_dir.add("a/two.bin", 100)
        # The v2 file tree node for "a" is paid for once, not twice, so sharing a directory costs less.
        self.assertLess(shared_dir.bytes, separate_dirs.bytes)

    def test_files_can_be_added_in_any_order(self):
        forward = TorrentSize(PIECE_SIZE)
        for path, size in [("a.bin", 100), ("b/c.bin", 200), ("b/d.bin", 300)]:
            forward.add(path, size)
        backward = TorrentSize(PIECE_SIZE)
        for path, size in reversed([("a.bin", 100), ("b/c.bin", 200), ("b/d.bin", 300)]):
            backward.add(path, size)
        self.assertEqual(forward.bytes, backward.bytes)
        self.assertEqual(forward.pieces, backward.pieces)


@unittest.skipUnless(LIBTORRENT, "libtorrent Python bindings aren't installed")
class RealTorrentTest(unittest.TestCase):
    """Builds a real torrent and compares its actual size against TorrentSize's estimate for the same files."""

    def _build(self, files, readme_bytes=b"EVE Ref Deep Archive test README\n"):
        """
        files: {relative path: size in bytes}. Returns (actual .torrent bytes, TorrentSize estimate for just the
        data files -- the same split 01-find/04-make-torrent use, where the README only counts via FIXED_BYTES).
        """
        name = "everef-deep-archive-2024-01-01"
        with tempfile.TemporaryDirectory() as parent:
            content_dir = os.path.join(parent, name)
            os.makedirs(content_dir)
            for path, size in files.items():
                full = os.path.join(content_dir, path)
                os.makedirs(os.path.dirname(full), exist_ok=True)
                with open(full, "wb") as f:
                    # Repeating, not random, content: piece hashing doesn't care, and it keeps the test fast and
                    # deterministic.
                    f.write((b"EVE Ref deep archive test data. " * (size // 32 + 1))[:size])
            readme_path = os.path.join(content_dir, f"{name}-README.txt")
            with open(readme_path, "wb") as f:
                f.write(readme_bytes)

            fs = lt.file_storage()
            for path in sorted(files) + [f"{name}-README.txt"]:
                full = os.path.join(content_dir, path)
                fs.add_file(f"{name}/{path}", os.path.getsize(full))
            ct = lt.create_torrent(fs, PIECE_SIZE)
            for tier, tracker in enumerate(
                ["udp://tracker1.example.com:1337/announce", "udp://tracker2.example.com:1337/announce"]
            ):
                ct.add_tracker(tracker, tier)
            ct.set_comment(
                "EVE Ref Deep Archive: everef-deep-archive-2024-01-01\n"
                "Data from 2020–2024. Datasets: a, b, c, e.\n"
                "What this is, how to use it and how to help seed: https://docs.everef.net/datasets/deep-archive.html\n"
                "File list: https://data.everef.net/deep-archive/everef-deep-archive-2024-01-01.txt"
            )
            ct.set_creator("EVE Ref")
            lt.set_piece_hashes(ct, parent)
            actual_bytes = len(lt.bencode(ct.generate()))

        estimate = TorrentSize(PIECE_SIZE)
        for path, size in files.items():
            estimate.add(path, size)
        return actual_bytes, estimate

    def test_estimate_is_close_to_a_real_torrent(self):
        files = {
            "a/empty.bin": 0,
            "a/one-byte.bin": 1,
            "b/exactly-one-piece.bin": PIECE_SIZE,
            "b/one-byte-under-a-piece.bin": PIECE_SIZE - 1,
            "b/one-byte-over-a-piece.bin": PIECE_SIZE + 1,  # needs a v1 pad file
            "c/d/several-pieces.bin": PIECE_SIZE * 5,  # multi-piece: exercises the v2 piece layer
            "e/unicode-éè.bin": 10,  # non-ASCII byte length in the path matters to the estimate
        }
        actual_bytes, estimate = self._build(files)

        # The whole point of the estimate: it must never undershoot the real .torrent, or 01-find could pick a
        # cutoff that goes over MAX_TORRENT_BYTES without noticing.
        self.assertGreaterEqual(
            estimate.bytes,
            actual_bytes,
            f"estimate {estimate.bytes} undershoots the real .torrent ({actual_bytes} bytes)",
        )
        # It's a limit check, not an exact prediction (module docstring): roughly in the right ballpark is enough.
        # FIXED_BYTES (the allowance for trackers/comment/creator/name/README) is the whole margin budget; this
        # torrent's actual trackers+comment+README are well under it, so the overshoot should be too.
        self.assertLess(
            estimate.bytes - actual_bytes,
            FIXED_BYTES,
            f"estimate {estimate.bytes} is more than FIXED_BYTES ({FIXED_BYTES}) over the real size ({actual_bytes})",
        )

    def test_estimate_scales_with_many_small_files(self):
        # A more realistic shape: lots of small dated files, as a real selection mostly is (README, "Datasets").
        files = {f"market-orders/history/2022/{month:02d}/{day:02d}/orders.csv.bz2": 5000 for month in range(1, 4) for day in range(1, 29)}
        actual_bytes, estimate = self._build(files)
        self.assertGreaterEqual(estimate.bytes, actual_bytes)
        self.assertLess(estimate.bytes - actual_bytes, FIXED_BYTES)


if __name__ == "__main__":
    unittest.main()
