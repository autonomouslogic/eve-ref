package com.autonomouslogic.everef.refdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

public class FieldRenamerTest {
	@Inject
	ObjectMapper objectMapper;

	@Inject
	TestDataUtil testDataUtil;

	@Inject
	FieldRenamer fieldRenamer;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/com/autonomouslogic/everef/refdata/ObjectMergerTest/sde-renames.csv")
	void shouldRenameFieldsFromSdeToRefData(String input, String expected) {
		assertEquals(expected, fieldRenamer.fieldRenameFromSde(input));
	}

	@Test
	@SneakyThrows
	void shouldRenameObjectFields() {
		var input = """
			{
				"var": 1,
				"anotherVar": 2
			}""";
		var expected = """
			{
				"var": 1,
				"another_var": 2
			}""";
		testDataUtil.assertJsonEquals(
				objectMapper.readTree(expected), fieldRenamer.fieldRenameFromSde(objectMapper.readTree(input)));
	}

	@Test
	@SneakyThrows
	void shouldRenameObjectsRecursively() {
		var input =
				"""
			{
				"var": 1,
				"anotherVar": 2,
				"childObj": {
					"var": 3,
					"anotherVar": 4
				}
			}""";
		var expected =
				"""
			{
				"var": 1,
				"another_var": 2,
				"child_obj": {
					"var": 3,
					"another_var": 4
				}
			}""";
		testDataUtil.assertJsonEquals(
				objectMapper.readTree(expected), fieldRenamer.fieldRenameFromSde(objectMapper.readTree(input)));
	}

	@Test
	@SneakyThrows
	void shouldRenameArrays() {
		var input = """
			[{
				"var": 1,
				"anotherVar": 2
			}, {
				"var": 3,
				"anotherVar": 4
			}]""";
		var expected = """
			[{
				"var": 1,
				"another_var": 2
			}, {
				"var": 3,
				"another_var": 4
			}]""";
		testDataUtil.assertJsonEquals(
				objectMapper.readTree(expected), fieldRenamer.fieldRenameFromSde(objectMapper.readTree(input)));
	}
}
