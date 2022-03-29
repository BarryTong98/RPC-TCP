package com.mszlu.rpc.spring;

import com.mszlu.rpc.annontation.EnableRpc;
import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.client.NettyClient;
import com.mszlu.rpc.proxy.MsRpcClientProxy;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.mszlu.rpc.server.MsServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 在spring 的bean 初始化前后进行调用，一般代码都写到 初始化之后
 */
@Slf4j
public class MsRpcSpringBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor {
    private MsServiceProvider msServiceProvider;

    private MsRpcConfig msRpcConfig;

    private NettyClient nettyClient;

    private NacosTemplate nacosTemplate;

    public MsRpcSpringBeanPostProcessor() {
        //这里采取new的方式对应生成
        //我们希望在构造方法里面，在这个地方调用的时候，我们希望用一个单例工厂生成这个对象，保证线程安全
        //单例工厂的好处
        //1、即使我们调用多次，我们也能保证工厂实例是唯一的，防止线程安全问题
        //2、而且我们MsServiceProvider 在别的类也需要使用，如果在这个地方创建，无法引用，
        // 但是如果用单例工厂我们可以便于其他类使用，因为始终是同一个对象
        msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class);
        nettyClient = SingletonFactory.getInstance(NettyClient.class);
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        if(enableRpc != null){
            if(msRpcConfig == null){
                //如果为空，我们在这里初始化并且添加配置
                log.info("EnablePrc 会先于所有的Bean实例化之前执行");
                msRpcConfig = new MsRpcConfig();
                msRpcConfig.setProviderPort(enableRpc.serverPort());
                msRpcConfig.setNacosPort(enableRpc.nacosPort());
                msRpcConfig.setNacosHost(enableRpc.nacosHost());
                msRpcConfig.setNacosGroup(enableRpc.nacosGroup());
                nettyClient.setMsRpcConfig(msRpcConfig);
                msServiceProvider.setMsRpcConfig(msRpcConfig);
                nacosTemplate.init(msRpcConfig.getNacosHost(), msRpcConfig.getNacosPort());
            }


        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //知道MsService注解，以及MsReference注解
        //Object bean代表spring所有能扫描的bean
        //在这边尝试获取注解
        MsService msService = bean.getClass().getAnnotation(MsService.class);
        if (bean.getClass().isAnnotationPresent(MsService.class)) {
            MsReference msReference = bean.getClass().getAnnotation(MsReference.class);
            //加了MsService的bean就被找到了，就把其中的方法 都发布为服务
            //把对应的注解MsService和bean，注解里面又可能有对应的参数，我们把bean传进去
            msServiceProvider.publishService(msService, bean);
        }
        Field[] declaredFields = bean.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            MsReference msReference = declaredField.getAnnotation(MsReference.class);
            if (msReference != null) {
                //找到了 加了MsReference 的字段，就要生成代理类， 当我们的接口方法调用的时候，实际上就是访问的代理类中的invoke方法
                //在invoke方法中实现对应的调用
                MsRpcClientProxy msRpcClientProxy = new MsRpcClientProxy(msReference, nettyClient);
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

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner做了一个扫描器
                Class<?> scannerClass = ClassUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        MsRpcSpringBeanPostProcessor.class.getClassLoader());
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class})
                        .newInstance(new Object[]{(BeanDefinitionRegistry) beanFactory, true});
                // add filter 过滤器 EnableRpc.class
                Class<?> filterClass = ClassUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter",
                        MsRpcSpringBeanPostProcessor.class.getClassLoader());
                //在所有bean加载之前，扫描这个注解
                //将EnableRpc实例化
                Object filter = filterClass.getConstructor(Class.class).newInstance(EnableRpc.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter",
                        ClassUtils.forName("org.springframework.core.type.filter.TypeFilter", MsRpcSpringBeanPostProcessor.class.getClassLoader()));
                addIncludeFilter.invoke(scanner, filter);
                // scan packages
                //在所有bean实例化之前，我们扫描
                //
                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
                scan.invoke(scanner, new Object[]{"com.mszlu.rpc.annontation"});
            } catch (Throwable e) {
                // spring 2.0
            }
        }




    }
}
