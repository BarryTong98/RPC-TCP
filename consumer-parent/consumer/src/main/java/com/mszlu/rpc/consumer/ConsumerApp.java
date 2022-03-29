package com.mszlu.rpc.consumer;

import com.mszlu.rpc.annontation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//通过EnableRpc的调用，我们就间接实现了@Import(MsRpcSpringBeanPostProcessor.class)
@EnableRpc(nacosGroup = "ms-rpc")
public class ConsumerApp {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApp.class,args);
    }
}
