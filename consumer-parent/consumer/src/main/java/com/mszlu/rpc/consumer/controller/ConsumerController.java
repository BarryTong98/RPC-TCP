package com.mszlu.rpc.consumer.controller;

import com.mszlu.rpc.annontation.MsReference;
import com.mszlu.rpc.consumer.rpc.GoodsHttpRpc;
import com.mszlu.rpc.provider.service.GoodsService;
import com.mszlu.rpc.provider.service.model.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("consumer")
public class ConsumerController {
    @Autowired
    private RestTemplate restTemplate;
    //The below way is extremely inconvenient, we will not use it in the current project
//    @GetMapping("/find/{id}")
//    public Goods find(@PathVariable Long id){
//        //Get access to the product query service by http
//        //http://localhost:7777/provider/goods/1
//        Goods goods = restTemplate.getForObject("http://localhost:7777/provider/goods/" + id, Goods.class);
//        //http://localhost:5555/consumer/find/1 -> get the real result
//        return goods;
//    }
//    @Autowired
//    private GoodsHttpRpc goodsHttpRpc;
//
//    @GetMapping("/find/{id}")
//    public Goods find(@PathVariable Long id){
//        //Get access to the product query service by http
//        return goodsHttpRpc.findGoods(id);
//    }

    //在微服务里面，服务提供方会有多个，那么这边只能连接一个
    @MsReference(version = "1.0")
    private GoodsService goodsService;

    @GetMapping("/find/{id}")
    public Goods find(@PathVariable Long id) {
        return goodsService.findGoods(id);
    }
}
