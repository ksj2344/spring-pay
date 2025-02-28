package com.green.greengram.kakaopay;

import com.green.greengram.config.feignclient.FeignClientKakaoPayConfiguration;
import com.green.greengram.kakaopay.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
//Web Client의 일을 대신함. 좀더 간편하고 쉽게.

@FeignClient(name = "kakaoPayApi"
           , url = "${constants.kakao-pay.base-url}"
           , configuration = { FeignClientKakaoPayConfiguration.class })
public interface KakaoPayFeignClient {


    @PostMapping(value = "/ready")
    KakaoPayReadyRes postReady( KakaoPayReadyFeignReq req);
    // @RequestBody: 원래는 바디로 값을 보내도록 하였으나

    @PostMapping(value = "/approve")
    KakaoPayApproveRes postApprove(KakaoPayApproveFeignReq req);
}
