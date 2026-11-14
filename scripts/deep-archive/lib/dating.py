"""
dating: how a file on data.everef.net is dated (NOTES.md "Dating"). 01-find selects by it and the purge scripts
(d01-find, d03-dryrun, d04-delete) decide by it, so both always agree on a file's date.
"""

import datetime
import re

NAME_DATE_RE = re.compile(r"(?<![0-9])([0-9]{4})-([0-9]{2})-([0-9]{2})(?![0-9])")
NAME_YEAR_RE = re.compile(r"(?<![0-9])([0-9]{4})(?![0-9])")
# Earlier numbers in a file name aren't years: EVE Online was released in 2003.
FIRST_YEAR = 2003


def file_name(path):
    return path.rsplit("/", 1)[-1]


def file_date(path, modified):
    """
    (date, dated_by): the earliest of the modification date ("modified"), any ISO date in the file name ("name date"),
    and 31 December of any year in the file name ("name year"). On a tie, the first of those.
    """
    name = file_name(path)
    dates = [(modified, "modified")]
    for match in NAME_DATE_RE.finditer(name):
        try:
            date = datetime.date(int(match[1]), int(match[2]), int(match[3]))
        except ValueError:
            continue
        if date.year >= FIRST_YEAR:
            dates.append((date, "name date"))
    for match in NAME_YEAR_RE.finditer(name):
        year = int(match[1])
        if year >= FIRST_YEAR:
            dates.append((datetime.date(year, 12, 31), "name year"))
    return min(dates, key=lambda d: d[0])
