package com.mszlu.rpc.provider.service.impl;

import com.mszlu.rpc.annontation.MsService;
import com.mszlu.rpc.provider.service.GoodsService;
import com.mszlu.rpc.provider.service.model.Goods;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
//把GoodsService这个服务 发布，消费方就可以进行调用
@MsService(version = "1.0")
public class GoodsServiceImpl implements GoodsService {

    public Goods findGoods(Long id) {
        return new Goods(id,"Service provider's good", BigDecimal.valueOf(100));
    }
}
