package com.mszlu.rpc.netty.client.handler;

import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MsNettyClientHandler extends ChannelInboundHandlerAdapter {
    //这里去读消息，也是对应的Message消息，然后进行对应的处理

    private UnprocessedRequests unprocessedRequests;
    public MsNettyClientHandler(){
        unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //一旦客户端发出消息，在这就得等待接收
        if(msg instanceof MsMessage){
            MsMessage msMessage = (MsMessage) msg;
            Object data = msMessage.getData();
            if(MessageTypeEnum.RESPONSE.getCode() == msMessage.getMessageType()){
                MsResponse msResponse = (MsResponse) data;
                unprocessedRequests.complete(msResponse);
            }
            //
        }



    }
}
