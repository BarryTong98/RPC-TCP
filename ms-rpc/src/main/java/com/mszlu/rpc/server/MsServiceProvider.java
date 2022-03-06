package com.mszlu.rpc.server;

import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.netty.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MsServiceProvider {
    private final Map<String, Object> serviceMap;

    public MsServiceProvider() {
        serviceMap = new ConcurrentHashMap<>();
    }

    //我们要去发布对应的服务，服务提供方
    public void publishService(MsService msService, Object service) {
        registerService(msService, service);
        //启动nettyServer
        NettyServer nettyServer = new NettyServer()
    }

    private void registerService(MsService msService, Object service) {
        String version = msService.version();
        String canonicalName = service.getClass().getInterfaces()[0].getCanonicalName();
        log.info("发布了服务：{}", canonicalName);
        //key: canonicalName+version
        //value: service
        serviceMap.put(canonicalName + version, service);
    }

    public Object getService(String serviceName) {
        //通过serviceName获取service bean
        return serviceMap.get(serviceName);
    }
}
