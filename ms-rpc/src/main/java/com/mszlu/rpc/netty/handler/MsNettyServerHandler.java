package com.mszlu.rpc.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MsNettyServerHandler extends ChannelInboundHandlerAdapter{
    //接收客户端发来的数据,数据肯定包括了 要调用的服务提供者的 方法/接口
    //解析消息，拿到数据，找到对应的服务提供者，然后调用，得到调用结果，发消息给客户端
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }
}
