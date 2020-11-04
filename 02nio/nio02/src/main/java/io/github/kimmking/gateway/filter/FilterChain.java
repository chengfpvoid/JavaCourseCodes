package io.github.kimmking.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 过滤器调用链接口
 */
public interface FilterChain {

    void doFilter(FullHttpRequest request, ChannelHandlerContext ctx);
}
