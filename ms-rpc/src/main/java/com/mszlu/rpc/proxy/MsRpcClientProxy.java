package com.mszlu.rpc.proxy;

import com.mszlu.rpc.annontation.MsMapping;
import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.message.MsResponse;
import com.mszlu.rpc.netty.client.NettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
public class MsRpcClientProxy implements InvocationHandler {

    private MsReference msReference;
    private NettyClient nettyClient;

    public MsRpcClientProxy(MsReference msReference, NettyClient nettyClient) {
        this.msReference = msReference;
        this.nettyClient = nettyClient;
    }

    //当接口 实现调用的时候，实际上是代理类的invoke方法被调用了

    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("rpc服务消费方 发起了调用...invoke");
        //对invoke进行实现
        //1. 构建请求数据MsRequest
        //2. 创建Netty客户端
        //3. 通过客户端向服务端发送请求
        //4. 接收数据
        String version = msReference.version();
        //这边UUID就可以，我们主要是为了保证他的唯一性
        MsRequest msRequest = MsRequest.builder()
                .group("ms-rpc")
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .version(version)
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .build();
        Object sendRequest = nettyClient.sendRequest(msRequest);
        CompletableFuture<MsResponse<Object>> resultCompletableFuture = (CompletableFuture<MsResponse<Object>>) sendRequest;
        //拿到对应的结果
        MsResponse<Object> msResponse = resultCompletableFuture.get();
        //如果msResponse为空，那么服务器调用失败
        if (msResponse == null) {
            throw new MsRpcException("服务器调用失败 ");
        }
        //如果requestid不一致，那么响应结果和请求不一致
        if (!msRequest.getRequestId().equals(msResponse.getRequestId())) {
            throw new MsRpcException("响应结果和请求不一致");
        }
        return msResponse.getData();
    }

    /**
     * 通过接口 生成代理类
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> T getProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, this);
    }
}
