package com.generallycloud.nio.component.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.generallycloud.nio.common.Logger;
import com.generallycloud.nio.common.LoggerFactory;
import com.generallycloud.nio.component.IOEventHandle;
import com.generallycloud.nio.component.Session;
import com.generallycloud.nio.component.TCPEndPoint;
import com.generallycloud.nio.component.IOEventHandle.IOEventState;

public abstract class AbstractWriteFuture extends FutureImpl implements IOWriteFuture {

	private Session			session		;
	private ReadFuture			readFuture	;
	protected TCPEndPoint		endPoint		;
	protected ByteBuffer		textBuffer	;
	protected InputStream		inputStream	;
	private static final Logger	logger		= LoggerFactory.getLogger(AbstractWriteFuture.class);

	public AbstractWriteFuture(TCPEndPoint endPoint, ReadFuture readFuture, ByteBuffer textBuffer) {
		this.endPoint = endPoint;
		this.readFuture = readFuture;
		this.session = endPoint.getSession();
		this.textBuffer = textBuffer;
	}

	protected void updateNetworkState(int length) {

		endPoint.updateNetworkState(length);
	}
	
	public void onException(IOException e) {
		
		ReadFuture readFuture = this.getReadFuture();
		
		IOEventHandle handle = readFuture.getIOEventHandle();
		
		if (handle == null) {
			logger.error(e.getMessage(),e);
			return;
		}
		
		try {
			handle.exceptionCaught(session, readFuture, e,IOEventState.WRITE);
		} catch (Throwable e1) {
			logger.debug(e1);
		}
	}

	public void onSuccess() {

		ReadFuture readFuture = this.getReadFuture();
		
		IOEventHandle handle = readFuture.getIOEventHandle();
		
		if (handle == null) {
			return;
		}

		try {
			handle.futureSent(session, getReadFuture());
		} catch (Throwable e) {
			logger.debug(e);
		}
	}

	public TCPEndPoint getEndPoint() {
		return endPoint;
	}

	public ReadFuture getReadFuture() {
		return readFuture;
	}

	public void setReadFuture(ReadFuture readFuture) {
		this.readFuture = readFuture;
	}
	
	public String toString() {
		return this.textBuffer.toString();
	}
}
