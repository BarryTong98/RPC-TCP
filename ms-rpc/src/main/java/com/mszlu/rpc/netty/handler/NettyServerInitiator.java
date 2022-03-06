package com.mszlu.rpc.netty.handler.server;

import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;


//这边继承了netty 管道的包
public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private EventExecutorGroup eventExecutors;

    public NettyServerInitiator(EventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    protected void initChannel(SocketChannel ch) throws Exception {
        //我们首先拿到这个管道， 在addLast添加一些我们想要的处理类

        //定义 TCP协议数据报文格式
        //解码器
        ch.pipeline().addLast("decoder", new MsRpcDecoder());
        //编码器
        ch.pipeline().addLast("encoder", new MsRpcEncoder());
        //消息处理器，线程池处理
        ch.pipeline().addLast(eventExecutors, "handler", new MsNettyServerHandler());
    }
}
