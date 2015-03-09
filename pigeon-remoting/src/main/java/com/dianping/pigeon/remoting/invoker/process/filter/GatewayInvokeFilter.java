/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.invoker.process.filter;

import org.apache.log4j.Logger;

import com.dianping.dpsf.async.ServiceFutureFactory;
import com.dianping.pigeon.log.LoggerLoader;
import com.dianping.pigeon.remoting.common.domain.InvocationRequest;
import com.dianping.pigeon.remoting.common.domain.InvocationResponse;
import com.dianping.pigeon.remoting.common.process.ServiceInvocationHandler;
import com.dianping.pigeon.remoting.common.util.Constants;
import com.dianping.pigeon.remoting.invoker.config.InvokerConfig;
import com.dianping.pigeon.remoting.invoker.domain.InvokerContext;
import com.dianping.pigeon.remoting.invoker.process.statistics.InvokerStatisticsHolder;

/**
 * 
 * 
 */
public class GatewayInvokeFilter extends InvocationInvokeFilter {

	private static final Logger logger = LoggerLoader.getLogger(GatewayInvokeFilter.class);

	@Override
	public InvocationResponse invoke(ServiceInvocationHandler handler, InvokerContext invocationContext)
			throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug("invoke the GatewayInvokeFilter, invocationContext:" + invocationContext);
		}
		InvokerConfig<?> invokerConfig = invocationContext.getInvokerConfig();
		InvocationRequest request = invocationContext.getRequest();
		int maxRequests = invokerConfig.getMaxRequests();
		try {
			InvokerStatisticsHolder.flowIn(request);
			try {
				return handler.handle(invocationContext);
			} catch (Throwable e) {
				if (Constants.CALL_FUTURE.equalsIgnoreCase(invokerConfig.getCallType())) {
					ServiceFutureFactory.remove();
				}
				throw e;
			}
		} finally {
			InvokerStatisticsHolder.flowOut(request);
		}
	}

}
