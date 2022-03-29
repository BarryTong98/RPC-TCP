package com.mszlu.rpc.netty.client;

import com.mszlu.rpc.message.MsRequest;

import java.util.concurrent.ExecutionException;

public interface MsClient {

    /**
     * 发送请求，并接收数据
     * @param msRequest
     * @return
     */
    Object sendRequest(MsRequest msRequest);
}

