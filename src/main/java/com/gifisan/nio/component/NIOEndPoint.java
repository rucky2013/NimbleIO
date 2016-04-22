package com.gifisan.nio.component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import com.gifisan.nio.Attachment;
import com.gifisan.nio.server.NIOContext;
import com.gifisan.nio.server.session.Session;
import com.gifisan.nio.service.WriteFuture;

public class NIOEndPoint implements EndPoint {

	private SlowlyNetworkReader	accept			= null;
	private Attachment			attachment		= null;
	private boolean			attempts0			= false;
	private boolean			attempts1			= false;
	private SocketChannel		channel			= null;
	private NIOContext			context			= null;
	private Session			currentSession		= null;
	private boolean			endConnect		= false;
	private InetSocketAddress	local			= null;
	private int				readed			= 0;
	private InetSocketAddress	remote			= null;
	private SelectionKey		selectionKey		= null;
	private Session[]			sessions			= new Session[4];
	private int				sessionSize		= 0;
	private Socket				socket			= null;
	private int				streamAvailable	= 0;
	private List<WriteFuture>	writers			= new ArrayList<WriteFuture>();
	private byte				writingSessionID	= -1;
	private SessionFactory		sessionFactory		= null;
	private IOReadFuture readFuture = null;

	public NIOEndPoint(SelectionKey selectionKey) throws SocketException {
		this.selectionKey = selectionKey;
		this.channel = (SocketChannel) selectionKey.channel();
		this.sessionFactory = context.getSessionFactory();
		this.socket = channel.socket();
		if (socket == null) {
			throw new SocketException("socket is empty");
		}
	}

	public void addWriter(WriteFuture writer) {
		if (isNetworkWeak()) {
			writers.add(writer);
		} else {
			context.getEndPointWriter().offer(writer);
		}
	}

	public void attach(Attachment attachment) {
		this.attachment = attachment;
	}

	public Attachment attachment() {
		return attachment;
	}

	public void attackNetwork(int length) {
		if (attempts0) {
			attempts1 = length == 0;
			return;
		}

		attempts0 = length == 0;
	}

	public boolean canWrite(byte sessionID) {
		return writingSessionID == -1 ? false : writingSessionID != sessionID;
	}

	public void close() throws IOException {
		this.selectionKey.attach(null);

		for (Session session : sessions) {
			if (session == null) {
				continue;
			}
			session.destroyImmediately();
		}

		this.channel.close();
	}

	public void endConnect() {
		this.endConnect = true;
	}

	public boolean flushServerOutputStream(ByteBuffer buffer) throws IOException {
		Session session = this.getCurrentSession();

		OutputStream outputStream = session.getServerOutputStream();

		if (outputStream == null) {
			throw new IOException("why did you not close this endpoint and did not handle it when a stream in.");
		}

		buffer.clear();

		int length = read(buffer);

		outputStream.write(buffer.array(), 0, length);

		readed += length;

		return readed == streamAvailable;
	}

	public Session getCurrentSession() {
		return currentSession;
	}

	public String getLocalAddr() {
		if (local == null) {
			local = (InetSocketAddress) socket.getLocalSocketAddress();
		}
		return local.getAddress().getCanonicalHostName();
	}

	public String getLocalHost() {
		return local.getHostName();
	}

	public int getLocalPort() {
		return local.getPort();
	}

	public int getMaxIdleTime() throws SocketException {
		return socket.getSoTimeout();
	}

	public String getRemoteAddr() {
		if (remote == null) {
			remote = (InetSocketAddress) socket.getRemoteSocketAddress();
		}
		return remote.getAddress().getCanonicalHostName();
	}

	public String getRemoteHost() {
		if (remote == null) {
			remote = (InetSocketAddress) socket.getRemoteSocketAddress();
		}
		return remote.getAddress().getHostName();
	}

	public int getRemotePort() {
		if (remote == null) {
			remote = (InetSocketAddress) socket.getRemoteSocketAddress();
		}
		return remote.getPort();
	}

	public SlowlyNetworkReader getSchedule() {
		return accept;
	}

	public Session getSession(byte sessionID) {

		Session session = sessions[sessionID];

		if (session == null) {
			session = sessionFactory.getSession(this, sessionID);
			sessions[sessionID] = session;
			sessionSize = sessionID;
		}

		return session;
	}

	public List<WriteFuture> getWriter() {
		this.attempts0 = false;
		this.attempts1 = false;
		return writers;
	}

	public boolean inStream() {
		return readed < streamAvailable;
	}

	public void interestWrite() {
		selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
	}

	public boolean isBlocking() {
		return channel.isBlocking();
	}

	public boolean isEndConnect() {
		return endConnect;
	}

	public boolean isNetworkWeak() {
		return attempts1;
	}

	public boolean isOpened() {
		return this.channel.isOpen();
	}

	public int read(ByteBuffer buffer) throws IOException {
		return this.channel.read(buffer);
	}

	public ByteBuffer read(int limit) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(limit);
		this.read(buffer);
		if (buffer.position() < limit) {
			throw new IOException("poor network ");
		}
		return buffer;
	}

	public void removeSession(byte sessionID) {
		Session session = sessions[sessionID];

		sessions[sessionID] = null;
		if (session != null) {
			session.destroyImmediately();
		}
	}

	public void resetServerOutputStream() {
		this.readed = 0;
		this.streamAvailable = 0;
	}

	public int sessionSize() {
		return sessionSize;
	}

	public void setCurrentSession(Session session) {
		this.currentSession = session;
	}

	public void setSchedule(SlowlyNetworkReader accept) {
		this.accept = accept;
	}

	public void setStreamAvailable(int streamAvailable) {
		this.streamAvailable = streamAvailable;

	}

	public void setWriting(byte sessionID) {
		this.writingSessionID = sessionID;
	}

	public int write(ByteBuffer buffer) throws IOException {
		return channel.write(buffer);
	}

	public NIOContext getContext() {
		return context;
	}

	public IOReadFuture getReadFuture() {
		return readFuture;
	}

	public void setReadFuture(IOReadFuture readFuture) {
		this.readFuture = readFuture;
	}
	
	

}