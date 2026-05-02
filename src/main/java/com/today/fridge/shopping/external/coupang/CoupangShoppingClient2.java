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

@Slf4j
@Component
public class CoupangShoppingClient2 {

    private static final String BASE_URL = "https://api-gateway.coupang.com";
    private static final String SEARCH_PATH = "/v2/providers/affiliate_open_api/apis/openapi/products/search";
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final RestClient restClient;

    @Value("${coupang.partners.access-key:}")
    private String accessKey;

    @Value("${coupang.partners.secret-key:}")
    private String secretKey;

    public CoupangShoppingClient2(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ShoppingItemDto> search(String keyword) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            log.warn("[CoupangShoppingClient2] API 키 미설정, 건너뜀");
            return Collections.emptyList();
        }
        try {
            String datetime = ZonedDateTime.now(ZoneOffset.UTC).format(DATETIME_FMT);
            String queryString = "keyword=" + keyword + "&limit=10&subId=";
            String signature = buildHmac(datetime, queryString);
            String authorization = String.format(
                    "CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
                    accessKey, datetime, signature);

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
