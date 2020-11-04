package io.github.kimmking.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * http request filter ,在filter中拿到请求头，添加key 是 nio value是chengfpvoid
 */
public class NioKeyRequestFilter implements HttpRequestFilter {
    @Override
    public void filter(FullHttpRequest fullRequest, ChannelHandlerContext ctx, HttpRequestFilterChain chain) {
        HttpHeaders httpHeaders = fullRequest.headers();
        httpHeaders.add("nio","chengfpvoid");
        chain.doFilter(fullRequest,ctx);
    }
}
