package com.mszlu.rpc.netty;

//首先定义接口，抽象其行为，因为以后还有更多的行为
public interface MsServer {
    void run();

    void stop();
}
