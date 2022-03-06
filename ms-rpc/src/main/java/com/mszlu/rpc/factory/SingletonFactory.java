package com.mszlu.rpc.factory;

import com.mszlu.rpc.server.MsServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 获取单例对象的工厂类
 *
 */
public final class SingletonFactory {
    // ConcurrentHashMap 线程安全的HashMap
    // 根据线程安全的配置，我们可以保证 它是唯一的
    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    //单例工程内部私有，private类导致外部不能引用
    private SingletonFactory() {
    }

    //获取实例，实例里面获取类型
    public static <T> T getInstance(Class<T> c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        String key = c.toString();
        //我们看Map里面有没有对应的实例，如果有直接返回
        if (OBJECT_MAP.containsKey(key)) {
            return c.cast(OBJECT_MAP.get(key));
        } else {
            //如果没有 new一个对应的实例
            return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }

    public static void main(String[] args) {
        //测试并发下 生成的单例是否唯一 开启线程池
        //运行代码之后我们可以看到打印的都是同一个类和地址
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        for (int i = 0 ; i< 1000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    MsServiceProvider instance = SingletonFactory.getInstance(MsServiceProvider.class);
                    System.out.println(instance);
                }
            });
        }
        while (true){}
    }
}

