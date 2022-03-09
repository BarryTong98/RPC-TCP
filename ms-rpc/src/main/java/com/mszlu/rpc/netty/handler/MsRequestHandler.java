package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.MsRequest;
import com.mszlu.rpc.server.MsServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class MsRequestHandler {
    private MsServiceProvider msServiceProvider;

    public MsRequestHandler() {
        msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class);
    }

    //处理这个数据
    public Object handler(MsRequest msRequest) {
        String interfaceName = msRequest.getInterfaceName();
        String version = msRequest.getVersion();
        Object service = msServiceProvider.getService(interfaceName + version);
        if(service == null){
            throw new MsRpcException("没有找到可用服务提供方");
        }
        try {
            Method method = service.getClass().getMethod(msRequest.getMethodName(), msRequest.getParamTypes());
            Object invoke = method.invoke(service, msRequest.getParameters());
            return invoke;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.info("服务提供方 方法调用 出现问题：", e);
        }
        return null;
    }
}
