package com.autonomouslogic.everef.util;

import com.google.common.collect.AbstractIterator;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class JsonStreamUtil {
	/**
	 * Streams the top-level entries of a JSON object one at a time, without ever materializing the
	 * whole object as a tree. {@code in} is not closed by the returned stream; the parser wrapping it
	 * is closed when the stream is closed (e.g. via try-with-resources).
	 */
	@SneakyThrows
	public static Stream<Pair<String, JsonNode>> streamObjectEntries(JsonMapper mapper, InputStream in) {
		var parser =
				mapper.reader().without(StreamReadFeature.AUTO_CLOSE_SOURCE).createParser(in);
		var token = parser.nextToken();
		if (token != JsonToken.START_OBJECT) {
			parser.close();
			throw new IllegalStateException("Expected a JSON object, got: " + token);
		}
		var spliterator = Spliterators.spliteratorUnknownSize(new JsonEntryIterator(parser), Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, false).onClose(parser::close);
	}

	@RequiredArgsConstructor
	private static class JsonEntryIterator extends AbstractIterator<Pair<String, JsonNode>> {
		private final JsonParser parser;

		@Override
		@SneakyThrows
		protected Pair<String, JsonNode> computeNext() {
			var token = parser.nextToken();
			if (token != JsonToken.PROPERTY_NAME) {
				return endOfData();
			}
			var name = parser.currentName();
			parser.nextToken();
			JsonNode value = parser.readValueAsTree();
			return Pair.of(name, value);
		}
	}
}
