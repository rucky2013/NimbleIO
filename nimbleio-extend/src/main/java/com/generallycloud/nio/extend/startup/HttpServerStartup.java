package com.generallycloud.nio.extend.startup;

import java.io.File;

import com.generallycloud.nio.acceptor.TCPAcceptor;
import com.generallycloud.nio.common.LifeCycleUtil;
import com.generallycloud.nio.common.Logger;
import com.generallycloud.nio.common.LoggerFactory;
import com.generallycloud.nio.common.SharedBundle;
import com.generallycloud.nio.component.DefaultNIOContext;
import com.generallycloud.nio.component.LoggerSEListener;
import com.generallycloud.nio.component.NIOContext;
import com.generallycloud.nio.component.SessionAliveSEListener;
import com.generallycloud.nio.component.protocol.http11.ServerHTTPProtocolFactory;
import com.generallycloud.nio.extend.ApplicationContext;
import com.generallycloud.nio.extend.FixedIOEventHandle;
import com.generallycloud.nio.extend.configuration.FileSystemACLoader;
import com.generallycloud.nio.extend.service.FutureAcceptorHttpFilter;

public class HttpServerStartup {
	
	private Logger logger = LoggerFactory.getLogger(HttpServerStartup.class);

	public void launch() throws Exception {

		ApplicationContext applicationContext = new ApplicationContext();

		NIOContext context = new DefaultNIOContext();

		TCPAcceptor acceptor = new TCPAcceptor();

		try {

			FileSystemACLoader fileSystemACLoader = new FileSystemACLoader();

			applicationContext
					.setLastServiceFilter(new FutureAcceptorHttpFilter(applicationContext.getClassLoader()));
			applicationContext.setConfigurationLoader(fileSystemACLoader);
			applicationContext.setContext(context);

			context.setIOEventHandleAdaptor(new FixedIOEventHandle(applicationContext));

			context.addSessionEventListener(new LoggerSEListener());
			
			context.addSessionEventListener(new SessionAliveSEListener());

			context.setProtocolFactory(new ServerHTTPProtocolFactory());
			
			acceptor.setContext(context);

			acceptor.bind();

		} catch (Throwable e) {

			logger.error(e.getMessage(), e);

			LifeCycleUtil.stop(applicationContext);

			acceptor.unbind();
		}
	}

	public static void main(String[] args) throws Exception {
		
		String classPath = SharedBundle.instance().getClassPath()  + "http/";
		
		File f = new File(classPath);
		
		if (f.exists()) {
			SharedBundle.instance().setClassPath(classPath);
		}
		
		HttpServerStartup launcher = new HttpServerStartup();

		launcher.launch();
	}

}
