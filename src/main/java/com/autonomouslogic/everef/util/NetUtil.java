package com.autonomouslogic.everef.util;

import lombok.SneakyThrows;

import java.net.ServerSocket;

public class NetUtil {
	@SneakyThrows
	public static int getFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
