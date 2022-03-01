package com.mszlu.rpc.consumer.rpc;

import com.mszlu.rpc.annontation.MsHttpClient;
import com.mszlu.rpc.annontation.MsMapping;
import com.mszlu.rpc.provider.service.model.Goods;

@MsHttpClient(value = "goodsHttpRpc")
//实现这个接口的实现类，并把它注入到spring容器当中
//value -》 goodsHttpRpc -》 bean的名称
public interface GoodsHttpRpc {
    //发起网络调用 调用provider服务 商品服务查询
    //通过这个方法，我们调用远程服务的时候，就喝使用本地的service一样方便了
    @MsMapping(url = "http://localhost:7777", api = "/provider/goods/{id}")
    Goods findGoods(Long id);
}
