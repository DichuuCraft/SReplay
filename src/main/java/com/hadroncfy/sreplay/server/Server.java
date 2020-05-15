package com.hadroncfy.sreplay.server;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.minecraft.util.Lazy;

public class Server {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random random = new Random();
    // Stolen from ServerNetworkIo
    public static final Lazy<NioEventLoopGroup> DEFAULT_CHANNEL = new Lazy<>(() -> {
        return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
    });
    public static final Lazy<EpollEventLoopGroup> EPOLL_CHANNEL = new Lazy<>(() -> {
        return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
    });

    final Map<String, FileEntry> urls = new HashMap<>();
    private ChannelFuture channel;

    public synchronized ChannelFuture bind(InetAddress address, int port){
        Class<? extends ServerSocketChannel> clazz;
        Lazy<? extends MultithreadEventLoopGroup> lazy;
        if (Epoll.isAvailable()){
            clazz = EpollServerSocketChannel.class;
            lazy = EPOLL_CHANNEL;
        }
        else {
            clazz = NioServerSocketChannel.class;
            lazy = DEFAULT_CHANNEL;
        }
        final ChannelFuture channel = new ServerBootstrap().group(lazy.get()).channel(clazz).localAddress(address, port).childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline()
                .addLast(new HttpRequestDecoder())
                .addLast(new HttpResponseEncoder())
                .addLast(new HttpObjectAggregator(1048576))
                .addLast(new ChunkedWriteHandler())
                .addLast(new HttpHandler(Server.this));
            }
        }).bind();
        channel.addListener(future -> {
            if (future.isSuccess()){
                this.channel = channel;
            }
        });
        return channel;
    }

    public synchronized ChannelFuture stop(){
        return channel.channel().closeFuture();
    }

    void removeExpiredFiles(){
        for (String path: urls.keySet()){
            final FileEntry f = urls.get(path);
            if (f.isExpired()){
                urls.remove(path);
            }
        }
    }

    private static String randomString(int len){
        final StringBuilder sb = new StringBuilder();
        while (len --> 0){
            int i = random.nextInt(16);
            if (i >= 10){
                sb.append((char)(i - 10 + 'a'));
            }
            else {
                sb.append((char)('0' + i));
            }
        }
        return sb.toString();
    }

    public String addFile(File file, long last){
        final String path = '/' + randomString(32) + '/' + file.getName();
        removeExpiredFiles();
        urls.put(path, new FileEntry(file, last));
        return path;
    }
}