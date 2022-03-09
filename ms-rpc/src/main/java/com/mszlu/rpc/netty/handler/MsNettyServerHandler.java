package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MsNettyServerHandler extends ChannelInboundHandlerAdapter {
    private MsRequestHandler msRequestHandler;

    public MsNettyServerHandler() {
        //单例工厂实现
        msRequestHandler = SingletonFactory.getInstance(MsRequestHandler.class);
    }

    //接收客户端发来的数据,数据肯定包括了 要调用的服务提供者的 方法/接口
    //解析消息，拿到数据，找到对应的服务提供者，然后调用，得到调用结果，发消息给客户端
    //读对应的数据 -> MsMessage
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof MsMessage) {
                //拿到请求数据,调用我们对应的服务提供方方法 获取结果 给客户端返回
                MsMessage msMessage = (MsMessage) msg;
                byte messageType = msMessage.getMessageType();
                //在这里判断messagetype如果是REQUEST
                if (MessageTypeEnum.REQUEST.getCode() == messageType) {
                    //拿到REQUEST
                    MsRequest msRequest = (MsRequest) msMessage.getData();
                    //处理业务，使用反射找到方法 发起调用 获取结果
                    Object result = msRequestHandler.handler(msRequest);
                    msMessage.setMessageType(MessageTypeEnum.RESPONSE.getCode());
                    //要保证这channel是可用的和可写的，才能进行厦门的操作
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        MsResponse msResponse = MsResponse.success(result, msRequest.getRequestId());
                        msMessage.setData(msResponse);
                    } else {
                        msMessage.setData(MsResponse.fail("network error"));
                    }
                }
                //通过管道写回去并且加一个关闭监听,保证我们的数据要能写完
                ctx.writeAndFlush(msMessage).addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Exception e) {
            log.error("读取消息出错：", e);
        }finally {
            //释放 以防内存泄漏
            ReferenceCountUtil.release(msg);
        }

    }
}
