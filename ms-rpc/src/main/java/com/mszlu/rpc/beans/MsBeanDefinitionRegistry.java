package com.mszlu.rpc.beans;

import com.mszlu.rpc.annontation.EnableHttpClient;
import com.mszlu.rpc.annontation.MsHttpClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

/**
 * 1. ImportBeanDefinitionRegistrar类只能通过其他类@Import的方式来加载，通常是启动类或配置类。
 * 2. 使用@Import，如果括号中的类是ImportBeanDefinitionRegistrar的实现类，则会调用接口方法，将其中要注册的类注册成bean
 * 3. 实现该接口的类拥有注册bean的能力
 */
public class MsBeanDefinitionRegistry implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, EnvironmentAware {

    private Environment environment;

    private ResourceLoader resourceLoader;

    public MsBeanDefinitionRegistry(){}

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerMsHttpClient(metadata,registry);
    }

    private void registerMsHttpClient(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(EnableHttpClient.class.getCanonicalName());
        //找到Enable注解，获取其中的basePackage属性，此属性标明了@MsHttpClient所在的包
        Object basePackage = annotationAttributes.get("basePackage");
        if (basePackage != null){
            String base = basePackage.toString();
            //ClassPathScanningCandidateComponentProvider是Spring提供的工具，可以按自定义的类型，查找classpath下符合要求的class文件
            ClassPathScanningCandidateComponentProvider scanner = getScanner();
            //创建资源加载器
            scanner.setResourceLoader(resourceLoader);
            //配置过滤器，扫描只扫描过滤器里面标识的注解
            AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(MsHttpClient.class);
            //找打MsHttpClient 扫描器 就会进行扫描
            scanner.addIncludeFilter(annotationTypeFilter);
            //上方定义了要找@MsHttpClient注解标识的类，这里进行对应包的扫描,扫描后就找到了所有被@MsHttpClient注解标识的类
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(base);
            //BeanDefinition
            for (BeanDefinition candidateComponent : candidateComponents) {
                //判断一下它是不是AnnotatedBeanDefinition 的定义
                if (candidateComponent instanceof  AnnotatedBeanDefinition){
                    //这就是被@MsHttpClient注解标识的类
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    //拿到类里面的信息
                    AnnotationMetadata beanDefinitionMetadata = annotatedBeanDefinition.getMetadata();
                    //判断它是不是一个接口，加了一个断言
                    Assert.isTrue(beanDefinitionMetadata.isInterface(),"@MsHttpClient 必须定义在接口上");
                    //通过上面的方式我们就找到对应的接口了
                    //获取此注解的属性。拿到MsHttpClient.class里面的属性，里面包含所有的属性
                    Map<String, Object> clientAnnotationAttributes = beanDefinitionMetadata.getAnnotationAttributes(MsHttpClient.class.getCanonicalName());
                    //这里判断是否value设置了值，value为此Bean的名称，定义bean的时候要用
                    String beanName = getClientName(clientAnnotationAttributes);


                    //这里我们获得的是一个接口，接口无法实例化，所有我们要生成代理实现类
                    //Bean的定义，通过建造者Builder模式来实现,需要一个参数，FactoryBean的实现类-> MsHttpClientFactoryBean -> implements FactoryBean
                    //FactoryBean是一个工厂Bean，可以生成某一个类型Bean实例，它最大的一个作用是：可以让我们自定义Bean的创建过程。
                    //这样子我们久获得了一个Bean的builder
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MsHttpClientFactoryBean.class);

                    //设置FactoryBean实现类中自定义的属性,这里我们设置@MsHttpClient标识的类,用于生成代理实现类
                    //这里的interface class对应的就是 MsHttpClientFactoryBean里面的interfaceClass
                    beanDefinitionBuilder.addPropertyValue("interfaceClass",beanDefinitionMetadata.getClassName());
                    assert beanName != null;
                    //定义Bean
                    registry.registerBeanDefinition(beanName,beanDefinitionBuilder.getBeanDefinition());
                }
            }
        }
    }

    //这个方法是为了获取bean name的值也就是@MsHttpClient 里面的String Value
    private String getClientName(Map<String, Object> clientAnnotationAttributes) {
        //首先判断值是否为空
        if (clientAnnotationAttributes == null){
            throw new RuntimeException("value必须有值");
        }
        Object value = clientAnnotationAttributes.get("value");
        if (value != null && !value.toString().equals("")){
            return value.toString();
        }
        return null;
    }
    //这个方法是从Feign组件中 源码找的
    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }
}

