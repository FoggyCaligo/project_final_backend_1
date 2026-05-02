package com.today.fridge.shopping.external.naver;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class NaverShoppingClient {

    private static final String BASE_URL = "https://openapi.naver.com";
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private final RestClient restClient;

    @Value("${naver.shopping.client-id:}")
    private String clientId;

    @Value("${naver.shopping.client-secret:}")
    private String clientSecret;

    public NaverShoppingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ShoppingItemDto> search(String keyword) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("[NaverShoppingClient] API 키 미설정, 건너뜀");
            return Collections.emptyList();
        }
        try {
            NaverShoppingResponse response = restClient.get()
                    .uri(BASE_URL + "/v1/search/shop.json?query={q}&display=10&sort=asc", keyword)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(NaverShoppingResponse.class);

            if (response == null || response.items() == null) {
                return Collections.emptyList();
            }
            return response.items().stream()
                    .filter(item -> item.lprice() != null && !item.lprice().isBlank())
                    .map(this::toDto)
                    .toList();
        } catch (RestClientException e) {
            log.warn("[NaverShoppingClient] 검색 실패 keyword={}: {}", keyword, e.getMessage());
            return Collections.emptyList();
        }
    }

    private ShoppingItemDto toDto(NaverShoppingResponse.Item item) {
        String name = HTML_TAG.matcher(item.title()).replaceAll("").trim();
        int price = parsePrice(item.lprice());
        int originalPrice = parsePrice(item.hprice());
        BigDecimal discountRate = null;
        if (originalPrice > 0 && price < originalPrice) {
            double rate = (1.0 - (double) price / originalPrice) * 100;
            discountRate = BigDecimal.valueOf(Math.round(rate * 10) / 10.0);
        }
        return ShoppingItemDto.builder()
                .mallName(item.mallName() != null ? item.mallName() : "네이버쇼핑")
                .mallProductId(item.productId())
                .productName(name)
                .brand(item.brand())
                .price(price)
                .originalPrice(originalPrice > 0 ? originalPrice : null)
                .discountRate(discountRate)
                .purchaseUrl(item.link())
                .imageUrl(item.image())
                .shippingType(ShippingType.STANDARD)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
    }

    private int parsePrice(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
