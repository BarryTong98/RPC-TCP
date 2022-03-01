package com.mszlu.rpc.provider.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Goods {

    //Goods id
    private Long id;
    //Goods name
    private String goodsName;
    //Good price
    private BigDecimal price;
}
