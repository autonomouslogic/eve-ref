package com.autonomouslogic.everef.mvstore;

import java.nio.ByteBuffer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.ObjectDataType;
import tools.jackson.databind.json.JsonMapper;

@Singleton
public class JsonNodeDataType extends ObjectDataType {
	@Inject
	protected JsonMapper jsonMapper;

	@Inject
	protected JsonNodeDataType() {}

	@Override
	@SneakyThrows
	public void write(WriteBuffer buff, Object obj) {
		byte[] json = jsonMapper.writeValueAsBytes(obj);
		buff.putInt(json.length);
		buff.put(json);
	}

	@Override
	@SneakyThrows
	public Object read(ByteBuffer buff) {
		int len = buff.getInt();
		byte[] json = new byte[len];
		buff.get(json);
		return jsonMapper.readTree(json);
	}
}
