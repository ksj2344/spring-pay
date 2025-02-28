package com.green.greengram.kakaopay.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
public class KakaoPayReadyReq {
    private List<OrderProductDto> productList;
}

/*
    {
        {productId: 100, quantity : 10},
        {productId: 200, quantity : 20},
        {productId: 300, quantity : 30}
    }

    키값으로 찾을 수 있게 함
    {
       100: {productId: 100, quantity : 10},
       200: {productId: 200, quantity : 20},
       300: {productId: 300, quantity : 30}
    }
 */