package com.mszlu.rpc.netty.client.idle;

/**
 * 重连检测狗，当发现当前的链路不稳定关闭之后，进行12次重连
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

//用来说明ChannelHandler是否可以在多个channel直接共享使用
@Sharable
@Slf4j
//ChannelHandlerHolder客户端处理类
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder, CacheClearHandler {


    private final Bootstrap bootstrap;
    private final Timer timer;
    private final InetSocketAddress inetSocketAddress;

    private volatile boolean reconnect = true;
    private int attempts;

    private final CompletableFuture<Channel> completableFuture;

    /**
     * @param bootstrap         server端启动类
     * @param inetSocketAddress 地址
     * @param completableFuture
     * @param reconnect         是否要重连
     */
    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, InetSocketAddress inetSocketAddress, CompletableFuture<Channel> completableFuture, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.inetSocketAddress = inetSocketAddress;
        this.reconnect = reconnect;
        this.completableFuture = completableFuture;
    }

    /**
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("当前链路已经激活了，重连尝试次数重新置为0");
        //代表一个连接 连接上了
        attempts = 0;
        //现在链路已经激活 了
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        //代表未连接，此时应该发生重试策略
        log.info("链接关闭");
        //判断重连状态，触发对应的重连
        if (reconnect) {
            log.info("链接关闭，将进行重连");
            //如果重连次数小于12，那么重连+1
            if (attempts < 12) {
                attempts++;
                log.info("重连次数:{}", attempts);
            } else {
                //重连次数大于12，标识不需要重连了，
                //不在重连了
                reconnect = false;
                //连接失败 从 缓存中 去除
                clear(inetSocketAddress);
            }
            //重连的间隔时间会越来越长
            int timeout = 2 << attempts;
            //定时任务 重连
            timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
        }
        ctx.fireChannelInactive();
    }

    //触发重连 调用下方的方法 -》定时任务
    public void run(Timeout timeout) throws Exception {
        ChannelFuture future;
        //设定handler
        //bootstrap已经初始化好了，只需要将handler填入就可以了
        //同步代码块包起来，更加安全一些
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                //将handler放入到管道里面
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(inetSocketAddress);
        }
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()) {
                    //代表连接成功，将channel放入任务中
                    completableFuture.complete(f.channel());
                } else {
                    completableFuture.completeExceptionally(future.cause());
                    //尝试重连
                    f.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }

}