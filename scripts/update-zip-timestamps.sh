#!/bin/bash -e

# This script updates the timestamps of supplied zip files based on the timestamp of the latest file insisde the zip.

for file in "$@"; do
	timestamp="$(unzip -l $file | egrep -oe "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}" | sort | tail -n 1)"
	echo $file: $timestamp
	touch -d "$timestamp" $file
done
