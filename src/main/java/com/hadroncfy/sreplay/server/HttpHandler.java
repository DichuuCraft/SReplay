package com.hadroncfy.sreplay.server;

import java.io.File;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random random = new Random();
    private final Server server;
    public HttpHandler(Server server){
        this.server = server;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        final String path = msg.uri();
        LOGGER.info("Got request");
        server.removeExpiredFiles();
        final FileEntry fileEntry = server.urls.get(path);
        if (fileEntry == null || !fileEntry.getFile().exists()){
            // No no no, no 404
            ctx.close();
            return;
        }
        final File file = fileEntry.getFile();
        server.urls.remove(path);
        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(200));
        response.headers().set("Content-Type", "application/zip");
        HttpUtil.setContentLength(response, file.length());
        ctx.write(response);
        ctx.write(new DefaultFileRegion(file, 0, file.length()));
        ChannelFuture ch = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        ch.addListener(future -> {
            if (!future.isSuccess()){
                LOGGER.error("Channel error: " + future.cause().getMessage());
            }
            ctx.close();
        });
    }
}