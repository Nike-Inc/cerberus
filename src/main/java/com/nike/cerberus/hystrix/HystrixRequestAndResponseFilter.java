package com.nike.cerberus.hystrix;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.filter.RequestAndResponseFilter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Riposte filter that sets up and shuts down a HystrixRequestContext
 */
public class HystrixRequestAndResponseFilter implements RequestAndResponseFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public <T> RequestInfo<T> filterRequestFirstChunkNoPayload(RequestInfo<T> currentRequestInfo,
                                                               ChannelHandlerContext ctx) {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        currentRequestInfo.addRequestAttribute("HystrixRequestContext", context);
        return currentRequestInfo;
    }

    @Override
    public <T> RequestInfo<T> filterRequestLastChunkWithFullPayload(RequestInfo<T> currentRequestInfo,
                                                                    ChannelHandlerContext ctx) {
        // Nothing to do - the other filterRequest method already handled Hystrix initialization.
        // Returning null just means use the passed-in response, which is what we want.
        return null;
    }

    @Override
    public <T> ResponseInfo<T> filterResponse(ResponseInfo<T> currentResponseInfo, RequestInfo<?> requestInfo,
                                              ChannelHandlerContext ctx) {
        try {
            ((HystrixRequestContext) requestInfo.getRequestAttributes().get("HystrixRequestContext")).shutdown();
        } catch (Throwable t) {
            logger.error("An unexpected error occurred trying to shutdown the HystrixRequestContext for this request.", t);
        }

        // Returning null just means use the passed-in response, which is what we want.
        return null;
    }
}