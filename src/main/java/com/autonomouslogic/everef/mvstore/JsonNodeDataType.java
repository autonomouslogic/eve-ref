package com.autonomouslogic.everef.mvstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.ObjectDataType;

@Singleton
public class JsonNodeDataType extends ObjectDataType {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected JsonNodeDataType() {}

	@Override
	@SneakyThrows
	public void write(WriteBuffer buff, Object obj) {
		byte[] json = objectMapper.writeValueAsBytes(obj);
		buff.putInt(json.length);
		buff.put(json);
	}

	@Override
	@SneakyThrows
	public Object read(ByteBuffer buff) {
		int len = buff.getInt();
		byte[] json = new byte[len];
		buff.get(json);
		return objectMapper.readTree(json);
	}
}
