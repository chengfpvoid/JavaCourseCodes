package io.github.kimmking.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 过滤器调用链实现类，链式调用各过滤器doFilter接口
 */
public final class HttpRequestFilterChain implements FilterChain {
    /**
     *     当前执行到的filter
     */
    private int pos = 0;
    /**
     * filter个数
     */
    private int n;

    private HttpRequestFilter[] filters = new HttpRequestFilter[]{};
    /**
     * 每次扩容的大小
     */
    private final static int INCREMENT = 10;




    @Override
    public void doFilter(FullHttpRequest request, ChannelHandlerContext ctx) {
        if(pos < n) {
            HttpRequestFilter filter = filters[pos++];
            filter.filter(request, ctx,this);
        }

    }

    public void addFilter(HttpRequestFilter filter) {
        if(filter == null) {
            return;
        }
        for(HttpRequestFilter requestFilter : filters) {
            if(requestFilter == filter) {
                return;
            }
        }
        if(filters.length == n) {
            HttpRequestFilter[] newFilters = new HttpRequestFilter[n + INCREMENT];
            System.arraycopy(filters,0,newFilters,0,n);
            filters = newFilters;
        }
        filters[n++] = filter;

    }
}
