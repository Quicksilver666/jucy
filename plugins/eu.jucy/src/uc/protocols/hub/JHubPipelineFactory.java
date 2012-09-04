package uc.protocols.hub;

import static org.jboss.netty.channel.Channels.*;

import java.io.IOException;
import java.net.ProtocolException;

import javax.net.ssl.SSLEngine;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.ssl.SslHandler;

import uc.ICryptoManager;
import uc.protocols.DCProtocol;

public class JHubPipelineFactory implements ChannelPipelineFactory {
	private static final Logger logger = LoggerFactory.make();
	
	private final boolean encrypted,adc;
	private final ICryptoManager cryptoManager;
	private final Hub hub;
	
	public JHubPipelineFactory(Hub hub,ICryptoManager cryptoManager,boolean encrypted,boolean adc) {
		this.hub = hub;
		this.encrypted = encrypted;
		this.adc = adc;
		this.cryptoManager = cryptoManager;
	}
	
	
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = pipeline();
		
		if (encrypted) {
			SSLEngine engine = cryptoManager.createSSLEngine(); 
			pipeline.addLast("ssl", new SslHandler(engine));
		}
	
 // On top of the SSL handler, add the text line codec.
		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192*4,  
				ChannelBuffers.wrappedBuffer(new byte[] { adc? (byte)'\n':(byte)'|' })));

		pipeline.addLast("decoder", new StringDecoder(adc? DCProtocol.NMDC_CHARSET:DCProtocol.ADC_CHARSET));
		pipeline.addLast("encoder", new StringEncoder(adc? DCProtocol.ADC_CHARSET:DCProtocol.NMDC_CHARSET));

		// and then business logic.
		pipeline.addLast("handler", new HubHandler(hub));

		return null;
	}

	
	public static class HubHandler extends SimpleChannelUpstreamHandler {
		
		private final Hub hub; 
		
		
		public HubHandler(Hub hub) {
			super();
			this.hub = hub;
		}

		@Override
		public void handleUpstream(
				ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			if (e instanceof ChannelStateEvent) {
				logger.info(e.toString());
			}
			super.handleUpstream(ctx, e);
		}

		@Override
		public void channelConnected(
				ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			// Get the SslHandler from the pipeline
			// which were added in SecureChatPipelineFactory.
			SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);

			// Begin handshake.
			sslHandler.handshake();
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			try {
				hub.receivedCommand((String)e.getMessage());
			} catch (ProtocolException e1) {
				e.getChannel().close();
			} catch (IOException e1) {
				e.getChannel().close();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			logger.warn("Unexpected exception from downstream.",e.getCause());
			e.getChannel().close();
		}
		
		
		
	}
}
