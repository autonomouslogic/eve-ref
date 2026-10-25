"""
Tests for 08-publish.py's feed code: building everef-deep-archive.xml from a torrent's info
(build_feed, build_magnet, torrent_url), reading a torrent's fields back out of a real .torrent
(read_torrent), and the diff shown before publishing (print_diff). No network, no rclone: read_torrent's one rclone
call is replaced with a local file copy.

These close the loop end to end: build_feed's output is fed back into check_feed_item, the same function
08-publish uses after publishing to check what data.everef.net actually serves, so a mismatch between
building and checking the feed fails here instead of after publishing it live.

Needs the libtorrent Python bindings, like 08-publish.py itself; skipped (not failed) if they aren't installed.

Run from scripts/deep-archive/: python3 -m unittest discover -s tests (or make test-deep-archive).
"""

import contextlib
import datetime
import importlib.util
import io
import os
import shutil
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "lib"))
from workflow import render_comment  # noqa: E402

try:
    import libtorrent as lt
except ImportError:
    raise unittest.SkipTest("libtorrent Python bindings aren't installed")

SPEC = importlib.util.spec_from_file_location("publish", os.path.join(HERE, "..", "08-publish.py"))
publish = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(publish)  # also needs libtorrent, already checked above


def entry(id_, pub_date, v1_hash="a" * 40, v2_hash="b" * 64, files=2, size=1000):
    return {
        "id": id_,
        "torrent_bytes": 12345,
        "v1_info_hash": v1_hash,
        "v2_info_hash": v2_hash,
        "pub_date": pub_date,
        "files": files,
        "bytes": size,
        "comment": render_comment(id_, {2020, 2021}, {"a", "b"}),
        "trackers": ["udp://tracker.example.com:1337/announce"],
    }


def quietly(function, *args):
    with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
        return function(*args)


class BuildMagnetTest(unittest.TestCase):
    def test_v1_and_v2_hash(self):
        magnet = publish.build_magnet("id", "a" * 40, "b" * 64, ["udp://t1", "udp://t2"])
        self.assertTrue(magnet.startswith("magnet:?xt=urn:btih:" + "a" * 40))
        self.assertIn("xt=urn:btmh:1220" + "b" * 64, magnet)
        self.assertIn("dn=id", magnet)
        self.assertIn("tr=udp://t1", magnet)
        self.assertIn("tr=udp://t2", magnet)

    def test_no_v2_hash(self):
        magnet = publish.build_magnet("id", "a" * 40, None, [])
        self.assertNotIn("btmh", magnet)


class TorrentUrlTest(unittest.TestCase):
    def test_torrent_and_txt(self):
        self.assertEqual(
            publish.torrent_url("everef-eve-online-deep-archive-2024-01-01", "torrent"),
            "https://data.everef.net/deep-archive/everef-eve-online-deep-archive-2024-01-01.torrent",
        )
        self.assertEqual(
            publish.torrent_url("everef-eve-online-deep-archive-2024-01-01", "txt"),
            "https://data.everef.net/deep-archive/everef-eve-online-deep-archive-2024-01-01.txt",
        )


class BuildFeedTest(unittest.TestCase):
    def test_round_trips_through_check_feed_item(self):
        entries = [entry("id", datetime.datetime(2024, 1, 2, tzinfo=datetime.timezone.utc))]
        body = publish.build_feed(entries).encode("utf-8")
        problems = quietly(publish.check_feed_item, body, "id", entries[0]["v1_info_hash"])
        self.assertEqual(problems, [])

    def test_catches_a_missing_item(self):
        entries = [entry("id", datetime.datetime(2024, 1, 2, tzinfo=datetime.timezone.utc))]
        body = publish.build_feed(entries).encode("utf-8")
        problems = quietly(publish.check_feed_item, body, "other-id", "c" * 40)
        self.assertTrue(any("no <item>" in p for p in problems))

    def test_sorted_newest_first(self):
        entries = [
            entry("older", datetime.datetime(2024, 1, 1, tzinfo=datetime.timezone.utc)),
            entry("newer", datetime.datetime(2024, 6, 1, tzinfo=datetime.timezone.utc)),
        ]
        root = ET.fromstring(publish.build_feed(entries))
        titles = [item.findtext("title") for item in root.iter("item")]
        self.assertEqual(titles, ["newer", "older"])

    def test_adding_a_new_entry_keeps_the_existing_one(self):
        # build_feed always rebuilds the whole feed from every published torrent (never incrementally), so "adding
        # an entry" is: call it again with the new torrent's entry included, and check the existing one is still
        # there, unchanged, alongside the new one.
        existing = entry("everef-eve-online-deep-archive-2024-01-01", datetime.datetime(2024, 1, 2, tzinfo=datetime.timezone.utc))
        before = ET.fromstring(publish.build_feed([existing]))
        self.assertEqual([item.findtext("title") for item in before.iter("item")], [existing["id"]])

        new = entry(
            "everef-eve-online-deep-archive-2024-06-01",
            datetime.datetime(2024, 6, 2, tzinfo=datetime.timezone.utc),
            v1_hash="c" * 40,
            v2_hash="d" * 64,
        )
        after = ET.fromstring(publish.build_feed([existing, new]))
        items = {item.findtext("title"): item for item in after.iter("item")}
        self.assertEqual(set(items), {existing["id"], new["id"]})

        # The new item is first (newest first) and carries its own hash; the existing item is untouched.
        titles_in_order = [item.findtext("title") for item in after.iter("item")]
        self.assertEqual(titles_in_order, [new["id"], existing["id"]])
        self.assertEqual(items[new["id"]].findtext("guid"), new["v1_info_hash"])
        self.assertEqual(
            ET.tostring(items[existing["id"]], encoding="unicode"), ET.tostring(before.find("channel/item"), encoding="unicode")
        )

        # Both round-trip through the module's own feed check, as 08-publish uses it after publishing live.
        body = publish.build_feed([existing, new]).encode("utf-8")
        self.assertEqual(quietly(publish.check_feed_item, body, existing["id"], existing["v1_info_hash"]), [])
        self.assertEqual(quietly(publish.check_feed_item, body, new["id"], new["v1_info_hash"]), [])

    def test_is_valid_rss(self):
        entries = [entry("id", datetime.datetime(2024, 1, 2, tzinfo=datetime.timezone.utc))]
        root = ET.fromstring(publish.build_feed(entries))
        self.assertEqual(root.tag, "rss")
        items = list(root.iter("item"))
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0].findtext("title"), "id")

    def test_magnet_uri_is_well_formed_in_cdata(self):
        entries = [entry("id", datetime.datetime(2024, 1, 2, tzinfo=datetime.timezone.utc))]
        feed = publish.build_feed(entries)
        root = ET.fromstring(feed)
        magnet = root.find(".//item/{http://xmlns.ezrss.it/0.1/}magnetURI").text
        self.assertTrue(magnet.startswith("magnet:?xt=urn:btih:"))

    def test_empty_feed_is_still_valid_rss(self):
        root = ET.fromstring(publish.build_feed([]))
        self.assertEqual(list(root.iter("item")), [])


class ReadTorrentTest(unittest.TestCase):
    """read_torrent with its one rclone call replaced by a local file copy."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.addCleanup(lambda: shutil.rmtree(self.tmp, ignore_errors=True))
        self.store = os.path.join(self.tmp, "store")  # stands in for the bucket's deep-archive/
        os.makedirs(self.store)
        self._real_rclone = publish.rclone
        # read_torrent's one rclone call ("copyto", "<remote>/<name>", <local>): the remote path is never a real
        # path here, so copy by basename from self.store instead of actually running rclone.
        publish.rclone = lambda *args: shutil.copyfile(os.path.join(self.store, args[1].rsplit("/", 1)[-1]), args[2])
        self.addCleanup(self._restore)

    def _restore(self):
        publish.rclone = self._real_rclone

    def _make_torrent(self, torrent_id, years=(2020, 2021), datasets=("a", "b")):
        content_dir = os.path.join(self.tmp, "content", torrent_id)
        os.makedirs(content_dir)
        data_path = os.path.join(content_dir, "a", "file.bin")
        os.makedirs(os.path.dirname(data_path))
        with open(data_path, "wb") as f:
            f.write(b"x" * 100)
        readme_path = os.path.join(content_dir, f"{torrent_id}-README.md")
        with open(readme_path, "w", encoding="utf-8") as f:
            f.write("readme")

        fs = lt.file_storage()
        fs.add_file(f"{torrent_id}/a/file.bin", 100)
        fs.add_file(f"{torrent_id}/{torrent_id}-README.md", len(b"readme"))
        comment = render_comment(torrent_id, set(years), set(datasets))
        ct = lt.create_torrent(fs, 16 * 1024)
        ct.add_tracker("udp://tracker.example.com:1337/announce", 0)
        ct.set_comment(comment)
        ct.set_creator("EVE Ref")
        lt.set_piece_hashes(ct, os.path.join(self.tmp, "content"))
        torrent_path = os.path.join(self.store, f"{torrent_id}.torrent")
        with open(torrent_path, "wb") as f:
            f.write(lt.bencode(ct.generate()))
        return torrent_path, lt.torrent_info(torrent_path)

    def test_fields_extracted_from_a_real_torrent(self):
        torrent_id = "everef-eve-online-deep-archive-2024-01-01"
        torrent_path, ti = self._make_torrent(torrent_id)

        result = quietly(publish.read_torrent, "deep_archive_remote", self.tmp, f"{torrent_id}.torrent")

        self.assertEqual(result["id"], torrent_id)
        self.assertEqual(result["v1_info_hash"], str(ti.info_hashes().v1))
        self.assertEqual(result["files"], 1)  # the README isn't counted
        self.assertEqual(result["bytes"], 100)
        self.assertEqual(result["trackers"], ["udp://tracker.example.com:1337/announce"])

    def test_rejects_a_name_that_is_not_a_torrent_id(self):
        torrent_path, _ = self._make_torrent("not-a-valid-id")
        with self.assertRaises(SystemExit):
            quietly(publish.read_torrent, "deep_archive_remote", self.tmp, "not-a-valid-id.torrent")


class PrintDiffTest(unittest.TestCase):
    def test_no_change(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            publish.print_diff("same\n", "same\n", "everef-deep-archive.xml")
        self.assertIn("No change to everef-deep-archive.xml", out.getvalue())

    def test_shows_a_diff(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            publish.print_diff("old\n", "new\n", "everef-deep-archive.xml")
        self.assertIn("-old", out.getvalue())
        self.assertIn("+new", out.getvalue())

    def test_first_publish_has_no_old_text(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            publish.print_diff(None, "new\n", "everef-deep-archive.xml")
        self.assertIn("+new", out.getvalue())


if __name__ == "__main__":
    unittest.main()
