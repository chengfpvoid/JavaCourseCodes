package io.github.kimmking.gateway.outbound.okhttp;

import io.github.kimmking.gateway.outbound.httpclient4.NamedThreadFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OkhttpOutboundHandler {

    private ExecutorService proxyService;

    private String backendUrl;

    private OkHttpClient okHttpClient;

    public OkhttpOutboundHandler(String backendUrl) {
        this.backendUrl = backendUrl.endsWith("/")?backendUrl.substring(0,backendUrl.length()-1):backendUrl;
        int coreSize = Runtime.getRuntime().availableProcessors() * 2;
        proxyService = new ThreadPoolExecutor(coreSize, coreSize,
                1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024),
                new NamedThreadFactory("proxyService"));
        okHttpClient = new OkHttpClient();


    }

    public void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {
        final String url = backendUrl + fullRequest.uri();
        proxyService.submit(() -> doGet(fullRequest,ctx,url));
        //doGet(fullRequest, ctx,url);

    }

    private void doGet(final FullHttpRequest fullRequest, ChannelHandlerContext ctx, String url) {
        HttpHeaders httpHeaders = fullRequest.headers();
        Request.Builder builder = new Request.Builder()
                .url(url).addHeader("Content-Type", "application/json");
        httpHeaders.forEach(entry-> builder.addHeader(entry.getKey(),entry.getValue()));
        Request request = builder.build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response)  {
                try {
                    handleResponse(fullRequest,ctx,response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleResponse(final FullHttpRequest fullRequest,final ChannelHandlerContext ctx,Response response) throws IOException {
        FullHttpResponse  httpResponse = null;
        try (ResponseBody responseBody = response.body()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            byte[] body = responseBody.bytes();
            httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
            httpResponse.headers().set("Content-Type", "application/json");
            httpResponse.headers().setInt("Content-Length", (int) responseBody.contentLength());
            Headers responseHeaders = response.headers();
            System.out.println("======response-headers============");
            for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
            }
         //   System.out.println(responseBody);


        }catch (Exception e){
            e.printStackTrace();
            httpResponse = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            ctx.close();

        }finally {
            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.write(httpResponse);
                }
            }
            ctx.flush();
        }
    }
}
