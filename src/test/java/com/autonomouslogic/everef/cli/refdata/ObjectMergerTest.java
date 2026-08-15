package com.autonomouslogic.everef.cli.refdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class ObjectMergerTest {
	@Inject
	JsonMapper jsonMapper;

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
			(ObjectNode) jsonMapper.readTree("""
				{
					"var1": 1
				}"""),
			(ObjectNode) jsonMapper.readTree("""
				{
					"var2": 2,
					"var3": 2
				}"""),
			(ObjectNode) jsonMapper.readTree("""
				{
					"var2": 3,
					"var3": 3
				}""")
		};
		var expected = jsonMapper.readTree("""
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
		var input = new ObjectNode[] {(ObjectNode) jsonMapper.readTree("""
				{
					"var1": 1,
					"child": {
						"var2": 1,
						"var3": 1
					}
				}"""), (ObjectNode) jsonMapper.readTree("""
				{
					"child": {
						"var1": 2,
						"var2": 2
					}
				}""")};
		var expected = jsonMapper.readTree("""
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
		var input = new ObjectNode[] {(ObjectNode) jsonMapper.readTree("""
				{
					"var1": [1, 2, 3]
				}"""), (ObjectNode) jsonMapper.readTree("""
				{
					"var1": [4, 5, 6]
				}""")};
		var expected = jsonMapper.readTree("""
			{
				"var1": [1, 2, 3, 4, 5, 6]
			}""");
		var actual = objectMerger.merge(input);
		assertEquals(expected, actual);
	}

	@Test
	@SneakyThrows
	void shouldNotReplaceWithNullValues() {
		var input = new ObjectNode[] {(ObjectNode) jsonMapper.readTree("""
				{
					"var1": "Text"
				}"""), (ObjectNode) jsonMapper.readTree("""
				{
					"var1": null
				}""")};
		var expected = jsonMapper.readTree("""
			{
				"var1": "Text"
			}""");
		var actual = objectMerger.merge(input);
		assertEquals(expected, actual);
	}

	@Test
	@SneakyThrows
	void shouldNotReplaceWithEmptyStrings() {
		var input = new ObjectNode[] {(ObjectNode) jsonMapper.readTree("""
				{
					"var1": "Text"
				}"""), (ObjectNode) jsonMapper.readTree("""
				{
					"var1": ""
				}""")};
		var expected = jsonMapper.readTree("""
			{
				"var1": "Text"
			}""");
		var actual = objectMerger.merge(input);
		assertEquals(expected, actual);
	}
}
