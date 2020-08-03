package com.hadroncfy.sreplay.server;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
    // Stolen from ServerNetworkIo
    public static final Lazy<NioEventLoopGroup> DEFAULT_CHANNEL = new Lazy<>(() -> {
        return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
    });
    public static final Lazy<EpollEventLoopGroup> EPOLL_CHANNEL = new Lazy<>(() -> {
        return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
    });

    private final Map<String, FileEntry> urls = new HashMap<>();
    private final Map<File, FileEntry> filesByFile = new HashMap<>();
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
        if (channel != null){
            ChannelFuture ch = channel.channel().close();
            ch.addListener(future -> {
                if (future.isSuccess()){
                    this.channel = null;
                }
            });
            return ch;
        }
        else {
            return null;
        }
    }

    void removeExpiredFiles(){
        for (String path: new HashSet<>(urls.keySet())){
            final FileEntry f = urls.get(path);
            if (f.isExpired()){
                removeFile(f);
            }
        }
    }

    FileEntry getFile(String path){
        return urls.get(path);
    }

    void removeFile(FileEntry f){
        urls.remove(f.getPath());
        filesByFile.remove(f.getFile());
    }

    public String addFile(File file, long last){
        removeExpiredFiles();
        FileEntry fe = filesByFile.get(file);
        if (fe == null){
            fe = new FileEntry(file, last);
            urls.put(fe.getPath(), fe);
            filesByFile.put(file, fe);
        }
        else {
            fe.touch(last);
        }
        return fe.getPath();
    }
}