package com.mszlu.rpc.sping;

import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.proxy.MsRpcClientProxy;
import com.mszlu.rpc.server.MsServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * 在spring 的bean 初始化前后进行调用，一般代码都写到 初始化之后
 */
public class MsRpcSpringBeanPostProcessor implements BeanPostProcessor {
    private MsServiceProvider msServiceProvider;

    public MsRpcSpringBeanPostProcessor(){
        //这里采取new的方式对应生成
        //我们希望在构造方法里面，在这个地方调用的时候，我们希望用一个单例工厂生成这个对象，保证线程安全
        //单例工厂的好处
        //1、即使我们调用多次，我们也能保证工厂实例是唯一的，防止线程安全问题
        //2、而且我们MsServiceProvider 在别的类也需要使用，如果在这个地方创建，无法引用，
        // 但是如果用单例工厂我们可以便于其他类使用，因为始终是同一个对象
        msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class );
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //知道MsService注解，以及MsReference注解
        //Object bean代表spring所有能扫描的bean
        //在这边尝试获取注解
        MsService msService = bean.getClass().getAnnotation(MsService.class);
        if(bean.getClass().isAnnotationPresent(MsService.class)){
            MsReference msReference = bean.getClass().getAnnotation(MsReference.class);
            //加了MsService的bean就被找到了，就把其中的方法 都发布为服务
            //把对应的注解MsService和bean，注解里面又可能有对应的参数，我们把bean船进去
            msServiceProvider.publishService(msService, bean);
        }
        Field[] declaredFields = bean.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields){
            MsReference msReference = declaredField.getAnnotation(MsReference.class);
            if(msReference != null){
                //找到了 加了MsReference 的字段，就要生成代理类， 当我们的接口方法调用的时候，实际上就是访问的代理类中的invoke方法
                //在invoke方法中实现对应的调用
                MsRpcClientProxy msRpcClientProxy = new MsRpcClientProxy();
                //这里是根据declarcedField的类型，生成对应的代理类
                Object proxy = msRpcClientProxy.getProxy(declaredField.getType());
                //当isAccessible()的结果是false时不允许通过反射访问该字段, isAccessible()默认是false
                declaredField.setAccessible(true);
                //
                try {
                    declaredField.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
