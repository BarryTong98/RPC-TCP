package com.mszlu.rpc.beans;

import com.mszlu.rpc.proxy.MsHttpClientProxy;
import org.springframework.beans.factory.FactoryBean;

//FactoryBean是一个工厂Bean，可以生成某一个类型Bean的实例，它最大的一个作用是：可以让我们自定义Bean的创建过程
public class MsHttpClientFactoryBean<T> implements FactoryBean {

    private Class<T> interfaceClass;

    @Override
    public T getObject() throws Exception {
        //返回一个代理实现类
        return new MsHttpClientProxy().getProxy(interfaceClass);
    }

    @Override
    public Class<?> getObjectType() {
        //类型 是接口
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        //是否单例
        //true是单例，false是非单例 在Spring5.0中此方法利用了JDK1.8的新特性变成了default方法，返回true
        return FactoryBean.super.isSingleton();
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }
}
