package com.autonomouslogic.everef.refdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObjectMergerTest {
	@Inject
	ObjectMapper objectMapper;

	@Inject
	ObjectMerger objectMerger;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@Test
	@SneakyThrows
	void shouldMergeObjects() {
		var input = new ObjectNode[] {
			(ObjectNode) objectMapper.readTree("""
				{
					"var1": 1
				}"""),
			(ObjectNode) objectMapper.readTree("""
				{
					"var2": 2,
					"var3": 2
				}"""),
			(ObjectNode) objectMapper.readTree("""
				{
					"var2": 3,
					"var3": 3
				}""")
		};
		var expected = objectMapper.readTree("""
			{
				"var1": 1,
				"var2": 3,
				"var3": 3
			}""");
		var actual = objectMerger.merge(input);
		assertEquals(expected, actual);
	}

	@Test
	@SneakyThrows
	void shouldMergeObjectsRecursively() {
		var input = new ObjectNode[] {
			(ObjectNode) objectMapper.readTree(
					"""
				{
					"var1": 1,
					"child": {
						"var2": 1,
						"var3": 1
					}
				}"""),
			(ObjectNode)
					objectMapper.readTree("""
				{
					"child": {
						"var1": 2,
						"var2": 2
					}
				}""")
		};
		var expected = objectMapper.readTree(
				"""
			{
				"var1": 1,
				"child": {
					"var1": 2,
					"var2": 2,
					"var3": 1
				}
			}""");
		var actual = objectMerger.merge(input);
		assertEquals(expected, actual);
	}

	@Test
	@SneakyThrows
	void shouldMergeArrays() {
		var input = new ObjectNode[] {
			(ObjectNode) objectMapper.readTree("""
				{
					"var1": [1, 2, 3]
				}"""),
			(ObjectNode) objectMapper.readTree("""
				{
					"var1": [4, 5, 6]
				}""")
		};
		var expected = objectMapper.readTree("""
			{
				"var1": [1, 2, 3, 4, 5, 6]
			}""");
		var actual = objectMerger.merge(input);
		assertEquals(expected, actual);
	}
}
