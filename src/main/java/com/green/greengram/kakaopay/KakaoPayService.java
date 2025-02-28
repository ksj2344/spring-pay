package com.green.greengram.kakaopay;

import com.green.greengram.config.SessionUtils;
import com.green.greengram.config.constants.ConstKakaoPay;
import com.green.greengram.config.exception.CustomException;
import com.green.greengram.config.exception.PayErrorCode;
import com.green.greengram.config.security.AuthenticationFacade;
import com.green.greengram.entity.*;
import com.green.greengram.kakaopay.model.*;
import com.green.greengram.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {
    private final AuthenticationFacade authenticationFacade;
    private final ConstKakaoPay constKakaoPay;
    private final KakaoPayFeignClient kakaoPayFeignClient;
    private final OrderMasterRepository orderMasterRepository;

    private final ProductRepository productRepository;

    @Transactional
    public KakaoPayReadyRes postReady(KakaoPayReadyReq req) {
        if(req.getProductList().size() == 0) { throw new CustomException(PayErrorCode.NOT_EXISTED_PRODUCT_INFO);  }
        List<Long> productIds = req.getProductList().stream()
                                .mapToLong(item -> item.getProductId())
                                .boxed()
                                .toList();
        // PK값 가져와서 List<Long>으로 passing, 구매하려는 상품 PK값만 가져옴
        List<Product> productList = productRepository.findByProductIdIn(productIds); //구매하고자 하는 상품 리스트가 DB로 부터 넘어옴
        if(productList.size() != req.getProductList().size()) { throw new CustomException(PayErrorCode.NOT_EXISTED_PRODUCT_INFO); }

        //Product 목록을 Map으로 변환하여 빠르게 검색 가능하게 만듦 Function.identity()는 객체의 멤버필드가 아닌 객체 자신을 말한다.
        Map<Long, OrderProductDto> orderProductMap = req.getProductList().stream().collect(Collectors.toMap(OrderProductDto::getProductId, Function.identity()));


        //총 결제금액
        // 첫번째 인자: 0은 초기값,
        // 두번째 인자: BiFunction Implements 객체 주소값
        // BiFunction: 파라미터가 두개. 첫번째 파라미터는 이전 리턴값(최초는 초기값), 두번째는 스트림 자식(리턴값) 순차적으로 넘어옴
        // reduce: 두번째 인자에 구현된 메서드를 stream 객체마다 호출해줌. 스트림의 값을 하나로 만들도록 계산해주고있음.
        int totalAmount = productList.stream().reduce(0
                , (prev, item) ->  prev + (item.getProductPrice() * orderProductMap.get(item.getProductId()).getQuantity())
                , Integer::sum); //두번째 인자의 파라미터를 integer로 활성화 시키기 위해 넣은것

        /*
            함수형 interface
            Function<T,R> 파라미터 있고 리턴값 있고 (T타입으로 와서 R타입으로 리턴)
            Supplier 파라미터 없고 리턴값 있고
            Consumer 파라미터 있고 리턴값 없고
            Predicate 파라미터 있고 리턴값 boolean

            Function은 Map많이 쓰고 Predicate는 filter에서 많이 씀
         */

        //DB insert 작업 준비
        User signedUser = User.builder()
                .userId(authenticationFacade.getSignedUserId())
                .build();

        OrderMaster orderMaster = OrderMaster.builder()
                .user(signedUser)
                .totalAmount(totalAmount)
                .orderStatusCode(OrderStatusCode.READY)
                .build();

        for(Product item : productList) {
            OrderProductIds ids = OrderProductIds.builder()
                    .orderId(item.getProductId())
                    .build();
            OrderProduct orderProduct = OrderProduct.builder()
                    .ids(ids)
                    .product(item)
                    .quantity(orderProductMap.get(item.getProductId()).getQuantity())
                    .unitPrice(item.getProductPrice())
                    .build();

            orderMaster.addOrderProduct(orderProduct);
        }

        orderMasterRepository.save(orderMaster);

        //결제 준비단계 : 상품1 외 n개 표시
        String itemName = productList.get(0).getProductName();
        if(productList.size() > 1) {
            itemName += String.format(" 외 %d개", productList.size() - 1);
        }

        KakaoPayReadyFeignReq feignReq = KakaoPayReadyFeignReq.builder()
                .cid(constKakaoPay.getCid())
                .partnerOrderId(orderMaster.getOrderId().toString()) //OrderMaster에 Insert 된 orderId값
                .partnerUserId(String.valueOf(authenticationFacade.getSignedUserId())) //결제 userId
                .itemName(itemName)
                .quantity(productList.size())
                .totalAmount(totalAmount)
                .taxFreeAmount(0)
                .approvalUrl(constKakaoPay.getCompletedUrl())
                .failUrl(constKakaoPay.getFailUrl())
                .cancelUrl(constKakaoPay.getCancelUrl())
                .build();

        KakaoPayReadyRes res = kakaoPayFeignClient.postReady(feignReq); //결제 준비 단계 요청을 보내고 응답으로 tid를 얻을 수 있다.

        //세션에 결제 정보 저장 (결제 승인 때 결제 준비 단계에서 보낸 tid, partnerOrderId, partnerUserId가 같아야 한다. 그래서 세션에 저장함)
        KakaoPaySessionDto dto = KakaoPaySessionDto.builder()
                .tid(res.getTid())
                .partnerOrderId(feignReq.getPartnerOrderId())
                .partnerUserId(feignReq.getPartnerUserId())
                .build();

        SessionUtils.addAttribute(constKakaoPay.getKakaoPayInfoSessionName(), dto);
        log.info("tid: {}", res.getTid());
        return res;
    }

    //승인처리
    public String getApprove(KakaoPayApproveReq req) {
        //카카오페이 준비과정에서 세션에 저장한 고유번호(tid), partnerOrderId, partnerUserId 가져오기
        KakaoPaySessionDto dto = (KakaoPaySessionDto) SessionUtils.getAttribute(constKakaoPay.getKakaoPayInfoSessionName());
        log.info("결제승인 요청을 인증하는 토큰: {}", req.getPgToken());
        //log.info("결제 고유번호: {}", tid);

        KakaoPayApproveFeignReq feignReq = KakaoPayApproveFeignReq.builder()
                .cid(constKakaoPay.getCid())
                .tid(dto.getTid())
                .partnerOrderId(dto.getPartnerOrderId())
                .partnerUserId(dto.getPartnerUserId())
                .pgToken(req.getPgToken())
                .payload("테스트")
                .build();

        KakaoPayApproveRes res = kakaoPayFeignClient.postApprove(feignReq);
        log.info("res: {}", res);

        OrderMaster orderMaster = orderMasterRepository.findById(Long.parseLong(dto.getPartnerOrderId())).orElse(null);
        if(orderMaster != null) {
            orderMaster.setOrderStatusCode(OrderStatusCode.COMPLETED);
        }
        return constKakaoPay.getCompletedUrl();
    }
}
