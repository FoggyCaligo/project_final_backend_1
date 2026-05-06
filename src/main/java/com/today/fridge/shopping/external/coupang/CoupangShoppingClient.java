package com.today.fridge.shopping.external.coupang;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

//  Log를 남기는 역할
@Slf4j
@Component
// 쿠팡 API 라는 외부 서비스에 접속해서 데이터를 가져오는 심부름꾼
public class CoupangShoppingClient {

    private static final String BASE_URL = "https://api-gateway.coupang.com";
    private static final String SEARCH_PATH = "/v2/providers/affiliate_open_api/apis/openapi/products/search";
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final RestClient restClient;

    @Value("${coupang.partners.access-key:}")
    private String accessKey;

    @Value("${coupang.partners.secret-key:}")
    private String secretKey;

    public CoupangShoppingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ShoppingItemDto> search(String keyword) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            log.warn("[CoupangShoppingClient2] API 키 미설정, 건너뜀");
            return Collections.emptyList();
            // Collections.emptyList는 빈 객체를 재사용한다 
        }

        try {
            // ZonedDateTime 은 UTC기준으로 시간+날짜+시간대(TimeZone) 정보를 사용 , java.time 패키지 중 하나 
            String datetime = ZonedDateTime.now(ZoneOffset.UTC).format(DATETIME_FMT);
            String queryString = "keyword=" + keyword + "&limit=10&subId=";
            String signature = buildHmac(datetime, queryString);
            String authorization = String.format(
                    "CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
                    accessKey, datetime, signature);
//  authorization 은 assetKey를 확인하고 현재 시각(signed-date) 에 signature (인증)을 보낸다  , 매 요청마다 시간이 포함된 '서명'을 만들어서 사용. Replay Attack 방지

            CoupangSearchResponse response = restClient.get()
                    .uri(BASE_URL + SEARCH_PATH + "?" + queryString)
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(CoupangSearchResponse.class);

            if (response == null || response.data() == null || response.data().productData() == null) {
                return Collections.emptyList();
            }
            return response.data().productData().stream()
                    .filter(p -> p.unitPrice() != null && p.unitPrice() > 0)
                    .map(this::toDto)
                    .toList();
        } catch (RestClientException e) {
            log.warn("[CoupangShoppingClient2] 검색 실패 keyword={}: {}", keyword, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[CoupangShoppingClient2] HMAC 서명 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
//  HMAC은 비밀 키를 사용해 만든 암호화된 메시지
 //HexFormat은 암호화 결과물은 사람이 읽을 수 없는 복잡한 컴퓨터 숫자인데 이걸 우리가 아는 16진수 문자열로 바꿔준다

    private String buildHmac(String datetime, String queryString) throws Exception {
        String message = datetime + "\nGET\n" + SEARCH_PATH + "\n" + queryString;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private ShoppingItemDto toDto(CoupangSearchResponse.ProductData p) {
        BigDecimal discountRate = (p.discountRate() != null && p.discountRate() > 0)
                ? BigDecimal.valueOf(p.discountRate()) : null;
        ShippingType shippingType = Boolean.TRUE.equals(p.isRocket())
                ? ShippingType.EXPRESS : ShippingType.STANDARD;
            // 빌더패턴의 장단점 : 가독성이 좋아지고 유연성(객체에 따라 값 사용 마음대로), 불변성(객체를 만든 후 내용을 바꿀 수 없게 설계) , 단점은 코드양이 많아지고 성능이 진다는 부분   
                return ShoppingItemDto.builder()
                .mallName("쿠팡")
                .mallProductId(p.itemId() != null ? String.valueOf(p.itemId()) : null)
                .productName(p.productName())
                .price(p.unitPrice())
                .originalPrice(p.originalPrice())
                .discountRate(discountRate)
                .purchaseUrl(p.productUrl())
                .imageUrl(p.productImage())
                .shippingType(shippingType)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
    }
}
