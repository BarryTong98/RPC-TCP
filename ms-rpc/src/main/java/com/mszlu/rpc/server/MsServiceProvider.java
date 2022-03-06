package com.mszlu.rpc.server;

import com.mszlu.rpc.annontation.MsService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MsServiceProvider {
    private final Map<String,Object> serviceMap;

    public MsServiceProvider(){
        serviceMap = new ConcurrentHashMap<>();
    }

    //我们要去发布对应的服务，服务提供方
    public void publishService(MsService msService, Object service) {
        registerService(msService, service);
        //启动nettyServer
    }

    private void registerService(MsService msService, Object service) {
        String version = msService.version();
        String canonicalName = service.getClass().getInterfaces()[0].getCanonicalName();
        //key: canonicalName+version
        //value: service
        serviceMap.put(canonicalName+version, service);
    }

    public Object getService(String serviceName){
        //通过serviceName获取service bean
        return serviceMap.get(serviceName);
    }
}
