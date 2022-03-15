package com.mszlu.rpc.netty.client;

import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.netty.client.handler.MsNettyClientHandler;
import com.mszlu.rpc.netty.client.handler.UnprocessedRequests;
import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class NettyClient implements MsClient{
    //发请求之前我们需要把netty客户端创建出来
    //启动类
    private final Bootstrap bootstrap;
    //线程池
    private final EventLoopGroup eventLoopGroup;

    private final UnprocessedRequests unprocessedRequests;

    public NettyClient(){
        //做一个缓存
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline ().addLast ( "decoder",new MsRpcDecoder() );
                        ch.pipeline ().addLast ( "encoder",new MsRpcEncoder());
                        ch.pipeline ().addLast ( "handler",new MsNettyClientHandler() );

                    }
                });
    }

    @Override
    public Object sendRequest(MsRequest msRequest, String host, int port) {
        //1. 先连接netty服务 拿到Channel 建立连接
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        //用启动类进行连接 并添加对应的监听器 来判断连接是不是一个完成的状态
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();
        //下面这个函数是异步操作，不会阻塞的
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //连接是否完成
                //连接netty服务成功
                if(future.isSuccess()){
                    //连接成功将结果放进去

                    channelCompletableFuture.complete(future.channel());
                }else {
                    //连接netty服务失败
                    log.info("连接netty服务失败");
                }
            }
        });
        //这里是下面数据发送到MsNettyClientHandler里面之后，接收他返回的数据
        CompletableFuture<MsResponse<Object>> resultCompletableFuture = new CompletableFuture<>();

        unprocessedRequests.put(msRequest.getRequestId(),resultCompletableFuture );

        //在这个地方放进去之后 channelCompletableFuture.complete(future.channel()); ->然后才能通过get拿到数据
        //通过get()函数获取数据 这个过程是阻塞的
        //所以一旦走到来这一步
        Channel channel = null;
        try {
            channel = channelCompletableFuture.get();
            //下面requestId省略的原因是没有用到
            MsMessage msMessage = MsMessage.builder()
                    .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(MessageTypeEnum.REQUEST.getCode())
                    .data(msRequest)
                    .build();
            //这个发送之后在MsNettyClientHandle里面进行处理
            channel.writeAndFlush(msMessage).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()){
                        log.info("请求完成");
                    }
                    else{

                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return resultCompletableFuture;

        //写对应的数据
    }
}
