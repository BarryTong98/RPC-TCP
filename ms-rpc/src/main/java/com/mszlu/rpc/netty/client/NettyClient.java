package com.mszlu.rpc.netty.client;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.RandomUtils;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.constant.CompressTypeEnum;
import com.mszlu.rpc.constant.MessageTypeEnum;
import com.mszlu.rpc.constant.SerializationTypeEnum;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsMessage;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.netty.client.handler.MsNettyClientHandler;
import com.mszlu.rpc.netty.client.handler.UnprocessedRequests;
import com.mszlu.rpc.netty.codec.MsRpcDecoder;
import com.mszlu.rpc.netty.codec.MsRpcEncoder;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

@Slf4j
public class NettyClient implements MsClient {
    private MsRpcConfig msRpcConfig;
    //发请求之前我们需要把netty客户端创建出来
    //启动类
    private final Bootstrap bootstrap;
    //线程池
    private final EventLoopGroup eventLoopGroup;

    private final UnprocessedRequests unprocessedRequests;

    private final NacosTemplate nacosTemplate;
    //ip,port
    private final static Set<String> SERVICES = new CopyOnWriteArraySet<>();

    public NettyClient() {
        //做一个缓存
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("decoder", new MsRpcDecoder());
                        ch.pipeline().addLast("encoder", new MsRpcEncoder());
                        ch.pipeline().addLast("handler", new MsNettyClientHandler());

                    }
                });
    }

    @Override
    public Object sendRequest(MsRequest msRequest) {
        if (msRpcConfig == null) {
            throw new MsRpcException("必须开启EnableRpc");
        }
        //这里是下面数据发送到MsNettyClientHandler里面之后，接收他返回的数据
        CompletableFuture<MsResponse<Object>> resultCompletableFuture = new CompletableFuture<>();
        //1. 先连接netty服务 拿到Channel 建立连接
        InetSocketAddress inetSocketAddress = null;
        String ipPort = null;
        //判断SERVICE里面有没有数据
        if (!SERVICES.isEmpty()) {
            //如果不为空
            int size = SERVICES.size();
            //随机的负载均衡算法
            int nextInt = RandomUtils.nextInt(0, size - 1);
            Optional<String> optional = SERVICES.stream().skip(nextInt).findFirst();
            //防止出现空指针
            if (optional.isPresent()) {
                //得到ip和port
                ipPort = optional.get();
                new InetSocketAddress(ipPort.split(",")[0], Integer.parseInt(ipPort.split(",")[1]));
                log.info("走了缓存,省入了链接nacos的开销");
            }
        }
        //dubbo rpc：注册中心挂掉之后，我们的服务调用还能否正常
        //回答：正常，因为第一次调用之后，就会缓存我们的服务提供方的地址，直接发起调用，不再需要走对应的注册中心了
        //nacos注册之后 --》需要从nacos中获取 服务提供方的ip和port
        Instance oneHealthyInstance = null;
        try {
            oneHealthyInstance = nacosTemplate.getOneHealthyInstance(msRpcConfig.getNacosGroup(), "ms-rpc");
            inetSocketAddress = new InetSocketAddress(oneHealthyInstance.getIp(), oneHealthyInstance.getPort());
            ipPort = oneHealthyInstance.getIp() + "," + oneHealthyInstance.getPort();
            SERVICES.add(ipPort);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取nacos实例 出错:", e);
            resultCompletableFuture.completeExceptionally(e);
            return resultCompletableFuture;
        }


        //用启动类进行连接 并添加对应的监听器 来判断连接是不是一个完成的状态
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();
        //下面这个函数是异步操作，不会阻塞的
        String finalIpPort = ipPort;
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //连接是否完成
                //连接netty服务成功
                if (future.isSuccess()) {
                    //连接成功将结果放进去

                    channelCompletableFuture.complete(future.channel());
                } else {
                    //连接netty服务失败
                    //从缓存中进行剔除
                    SERVICES.remove(finalIpPort);
                    //将异常放入
                    channelCompletableFuture.completeExceptionally(future.cause());
                    log.info("连接netty服务失败");
                }
            }
        });


        unprocessedRequests.put(msRequest.getRequestId(), resultCompletableFuture);

        //在这个地方放进去之后 channelCompletableFuture.complete(future.channel()); ->然后才能通过get拿到数据
        //通过get()函数获取数据 这个过程是阻塞的
        //所以一旦走到来这一步
        Channel channel = null;
        try {
            channel = channelCompletableFuture.get();
            //判断chnnel是不是活的
            if (!channel.isActive()) {
                throw new MsRpcException("连接异常");
            }
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
                    if (future.isSuccess()) {
                        log.info("请求完成");
                    } else {
                        log.info("发送数据请求数据失败");
                        //将future channel关闭
                        future.channel().close();
                        //标识失败
                        resultCompletableFuture.completeExceptionally(future.cause());
                    }
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            //标识对应的异常
            resultCompletableFuture.completeExceptionally(e);
            log.error("channel获取失败:", e);
        }
        return resultCompletableFuture;

        //写对应的数据
    }

    public MsRpcConfig getMsRpcConfig() {
        return msRpcConfig;
    }

    public void setMsRpcConfig(MsRpcConfig msRpcConfig) {
        this.msRpcConfig = msRpcConfig;
    }
}
