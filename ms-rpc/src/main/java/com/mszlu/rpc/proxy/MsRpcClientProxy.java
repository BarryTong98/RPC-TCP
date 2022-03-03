package com.mszlu.rpc.proxy;

import com.mszlu.rpc.annontation.MsMapping;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
public class MsRpcClientProxy implements InvocationHandler {

    public MsRpcClientProxy(){

    }

    //当接口 实现调用的时候，实际上是代理类的invoke方法被调用了
    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("rpc服务消费方 发起了调用...invoke");
        return null;
    }

    /**
     * 通过接口 生成代理类
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> T getProxy(Class<T> interfaceClass) {
        return(T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),new Class<?>[]{interfaceClass},this);
    }
}
