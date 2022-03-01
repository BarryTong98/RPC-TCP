package com.mszlu.rpc.provider.service;

import com.mszlu.rpc.provider.service.model.Goods;

public interface GoodsService {

    /**
     * Query the Goods according to the good id
     * @param id
     * @return
     */
    Goods findGoods(Long id);
}
