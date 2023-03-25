package com.autonomouslogic.everef.util;

import java.net.ServerSocket;
import lombok.SneakyThrows;

public class NetUtil {
	@SneakyThrows
	public static int getFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
