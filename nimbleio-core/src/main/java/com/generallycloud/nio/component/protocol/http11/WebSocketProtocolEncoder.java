package com.generallycloud.nio.component.protocol.http11;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.generallycloud.nio.common.MathUtil;
import com.generallycloud.nio.component.BufferedOutputStream;
import com.generallycloud.nio.component.TCPEndPoint;
import com.generallycloud.nio.component.protocol.IOReadFuture;
import com.generallycloud.nio.component.protocol.IOWriteFuture;
import com.generallycloud.nio.component.protocol.ProtocolEncoder;
import com.generallycloud.nio.component.protocol.TextWriteFuture;
import com.generallycloud.nio.component.protocol.http11.future.WebSocketReadFuture;

public class WebSocketProtocolEncoder implements ProtocolEncoder {

	public IOWriteFuture encode(TCPEndPoint endPoint, IOReadFuture readFuture) throws IOException {
		
		WebSocketReadFuture future = (WebSocketReadFuture) readFuture;

		BufferedOutputStream o = future.getWriteBuffer();

		byte [] header;
		
		int size = o.size();
		
		//FIXME  后续支持更多type
		//FIXME  后续支持更多mask
		//-127 : 10000001
		if (size < 126) {
			header = new byte[2];
			header[0] = -127;
			header[1] = (byte)size;
		}else if(size < ((1 << 16) -1)){
			header = new byte[4];
			header[0] = -127;
			header[1] = 126;
			header[3] = (byte)(size & 0xff);
			header[2] = (byte)((size >> 8) & 0xff);
		}else{
			header = new byte[6];
			header[0] = -127;
			header[1] = 126;
			MathUtil.int2Byte(header, size, 2);
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(header.length + size);
		
		buffer.put(header);
		buffer.put(o.array(),0,size);
		
		buffer.flip();

		TextWriteFuture textWriteFuture = new TextWriteFuture(endPoint, readFuture, buffer);

		return textWriteFuture;
	}

}
