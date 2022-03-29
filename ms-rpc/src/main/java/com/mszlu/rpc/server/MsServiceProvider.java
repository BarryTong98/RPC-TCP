package com.mszlu.rpc.server;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.exception.MsRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.NettyServer;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MsServiceProvider {
    private MsRpcConfig msRpcConfig;
    private final Map<String, Object> serviceMap;
    private NacosTemplate nacosTemplate;

    public MsServiceProvider() {
        serviceMap = new ConcurrentHashMap<>();
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    //我们要去发布对应的服务，服务提供方
    public void publishService(MsService msService, Object service) {
        registerService(msService, service);
        //启动nettyServer
        NettyServer nettyServer = new NettyServer();
    }

    private void registerService(MsService msService, Object service) {
        String version = msService.version();
        String interfaceName = service.getClass().getInterfaces()[0].getCanonicalName();
        log.info("发布了服务：{}", interfaceName);
        //key: canonicalName+version
        //value: service
        serviceMap.put(interfaceName + version, service);
        //同步注册到Nacos中 -------> 注册
        //group分组名称 只有同一个组内 调用关系才能成立，不同的组之间是隔离的

        //这里面需要判断一下
        if(msRpcConfig == null){
            throw new MsRpcException("必须开启EnableRPC");
        }
        try {
            Instance instance = new Instance();
            //得到本机的地址
            instance.setIp(InetAddress.getLocalHost().getHostAddress());
            instance.setPort(msRpcConfig.getNacosPort());
            instance.setClusterName("ms-rpc");
            //setServiceName -> interface name + version(1.0)
            instance.setServiceName(interfaceName + version);
            nacosTemplate.registerServer(msRpcConfig.getNacosGroup(), instance);
        } catch (Exception e) {
            log.error("nacos注册失败", e);
        }


    }

    public Object getService(String serviceName) {
        //通过serviceName获取service bean
        return serviceMap.get(serviceName);
    }

    public MsRpcConfig getMsRpcConfig() {
        return msRpcConfig;
    }

    public void setMsRpcConfig(MsRpcConfig msRpcConfig) {
        this.msRpcConfig = msRpcConfig;
    }
}
