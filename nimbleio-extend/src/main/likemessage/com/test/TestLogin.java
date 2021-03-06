package com.test;

import java.io.IOException;

import com.generallycloud.nio.Encoding;
import com.generallycloud.nio.common.CloseUtil;
import com.generallycloud.nio.common.MD5Token;
import com.generallycloud.nio.common.ThreadUtil;
import com.generallycloud.nio.connector.TCPConnector;
import com.generallycloud.nio.extend.FixedSession;
import com.generallycloud.nio.extend.IOConnectorUtil;
import com.generallycloud.nio.extend.SimpleIOEventHandle;

public class TestLogin {

	public static void main(String[] args) throws IOException {

		String username = "wk";
		String password = "wk";

		SimpleIOEventHandle eventHandle = new SimpleIOEventHandle();

		TCPConnector connector = IOConnectorUtil.getTCPConnector(eventHandle);

		FixedSession session = eventHandle.getFixedSession();

		connector.connect();
		
		boolean b = session.login(username, password);

		System.out.println(MD5Token.getInstance().getLongToken("admin100", Encoding.DEFAULT));

		System.out.println(b);

		ThreadUtil.sleep(500);

		CloseUtil.close(connector);

	}
}
