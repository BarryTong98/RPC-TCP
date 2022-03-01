package com.mszlu.rpc.provider.service.impl;

import com.mszlu.rpc.provider.service.GoodsService;
import com.mszlu.rpc.provider.service.model.Goods;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GoodsServiceImpl implements GoodsService {

    public Goods findGoods(Long id) {
        return new Goods(id,"Service provider's good", BigDecimal.valueOf(100));
    }
}
