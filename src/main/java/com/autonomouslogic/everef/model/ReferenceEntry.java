package com.autonomouslogic.everef.model;

import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
public class ReferenceEntry {
	String type;

	Long id;

	@NonNull
	String path;

	@NonNull
	@ToString.Exclude
	byte[] content;

	@NonNull
	String md5B64;

	@NonNull
	String md5Hex;
}
