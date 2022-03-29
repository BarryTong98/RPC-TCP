package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;


//这边继承了netty 管道的包
public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private EventExecutorGroup eventExecutors;

    public NettyServerInitiator(EventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    protected void initChannel(SocketChannel ch) throws Exception {
        //我们首先拿到这个管道， 在addLast添加一些我们想要的处理类
        //处理心跳，10秒钟 未收到 读请求 关闭客户端连接
        ch.pipeline().addLast(new IdleStateHandler(10, 0,0, TimeUnit.SECONDS));
        //定义 TCP协议数据报文格式
        //解码器
        ch.pipeline().addLast("decoder", new MsRpcDecoder());
        //编码器
        ch.pipeline().addLast("encoder", new MsRpcEncoder());
        //消息处理器，线程池处理
        ch.pipeline().addLast(eventExecutors, "handler", new MsNettyServerHandler());
    }
}
