/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.netty.invoker;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.dianping.pigeon.component.invocation.InvocationRequest;
import com.dianping.pigeon.component.invocation.InvocationResponse;
import com.dianping.pigeon.monitor.LoggerLoader;
import com.dianping.pigeon.remoting.common.util.Constants;
import com.dianping.pigeon.remoting.invoker.util.RpcEventUtils;
import com.dianping.pigeon.remoting.netty.codec.NettyCodecUtils;
import com.dianping.pigeon.threadpool.DefaultThreadPool;
import com.dianping.pigeon.threadpool.ThreadPool;

public class NettyClientHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerLoader.getLogger(NettyClientHandler.class);

	private static ThreadPool exceptionProcessThreadPool = new DefaultThreadPool("exception-process-threadpool", 2,
			100, new LinkedBlockingQueue<Runnable>(50), new CallerRunsPolicy());

	private NettyClient client;

	public NettyClientHandler(NettyClient client) {
		this.client = client;
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		super.handleUpstream(ctx, e);

	}

	@SuppressWarnings("unchecked")
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		List<InvocationResponse> messages = (List<InvocationResponse>) e.getMessage();
		for (final InvocationResponse response : messages) {
			client.processResponse(response);
		}
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		final ExceptionEvent e_ = e;
		final Object attachment = NettyCodecUtils.getAttachment(ctx, Constants.ATTACHMENT_RETRY);
		flowOutexceptionCaughtRequest(attachment);
		if (e.getCause() instanceof IOException) {
			e.getChannel().close();
			Runnable task = new Runnable() {
				public void run() {
					client.connectionException(attachment, e_.getCause());
				}
			};
			exceptionProcessThreadPool.execute(task);
		}
	}

	private void flowOutexceptionCaughtRequest(final Object attachment) {
		InvocationRequest request = getRequest(attachment);
		if (request != null) {
			RpcEventUtils.channelExceptionCaughtEvent(request, client.getAddress());
		}
	}

	private InvocationRequest getRequest(Object attachment) {
		if (attachment instanceof Object[]) {
			Object[] msg = (Object[]) attachment;
			for (Object ele : msg) {
				if (ele instanceof InvocationRequest) {
					return (InvocationRequest) ele;
				}
			}
		} else if (attachment instanceof InvocationRequest) {
			return (InvocationRequest) attachment;
		}
		return null;
	}

}
