package com.autonomouslogic.everef.util;

import com.google.common.collect.AbstractIterator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

@RequiredArgsConstructor
class ArchiveEntryIterator extends AbstractIterator<Pair<ArchiveEntry, byte[]>> {
	private final ArchiveInputStream stream;

	@Override
	@SneakyThrows
	protected Pair<ArchiveEntry, byte[]> computeNext() {
		var entry = stream.getNextEntry();
		if (entry == null) {
			return endOfData();
		}
		return Pair.of(entry, IOUtils.toByteArray(stream));
	}
}
