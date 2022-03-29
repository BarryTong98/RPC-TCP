package com.mszlu.rpc.netty.client.handler;

import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.constant.MsRpcConstants;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static org.reflections.Reflections.log;
@Slf4j
public class MsNettyClientHandler extends ChannelInboundHandlerAdapter {
    //这里去读消息，也是对应的Message消息，然后进行对应的处理

    private UnprocessedRequests unprocessedRequests;
    private MsRpcConstants msRpcConstants;

    public MsNettyClientHandler() {
        unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            //一旦客户端发出消息，在这就得等待接收
            if (msg instanceof MsMessage) {
                MsMessage msMessage = (MsMessage) msg;
                Object data = msMessage.getData();
                if (MessageTypeEnum.RESPONSE.getCode() == msMessage.getMessageType()) {
                    MsResponse msResponse = (MsResponse) data;
                    unprocessedRequests.complete(msResponse);
                }
                //
            }

        }catch (Exception e){
            log.error("客户端 读取消息出错: ",e);
        }finally {
            //释放 以防内存泄漏
            ReferenceCountUtil.release(msg);
        }


    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                //进行心跳检测，发送一个心跳包去服务端
                log.info("3s未收到写请求，发起心跳,地址：{}", ctx.channel().remoteAddress());
                MsMessage rpcMessage = new MsMessage();
                rpcMessage.setCodec(SerializationTypeEnum.PROTO_STUFF.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(MessageTypeEnum.HEARTBEAT_PING.getCode());
                rpcMessage.setData(MsRpcConstants.HEART_PING);
                //写对应的数据
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //代表通道已连接
        //表示channel活着
        log.info("客户端链接上了...链接正常");
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //代表服务端连接关闭了
        log.info("服务端连接关闭:{}", ctx.channel().remoteAddress());
        //需要将缓存清除掉

        //标识channel不活着
        ctx.fireChannelInactive();
    }
}
