package com.today.fridge.shopping.external.elevenst;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ElevenStShoppingClient 단위 테스트
 * - RestClient 체인을 Mock으로 대체하여 외부 API 호출 없이 XML 파싱·필터링 로직 검증
 * - 가격 하한선(< 1000원) 필터 및 비식품 키워드 필터(NON_FOOD_WORDS) 검증 포함
 */
@ExtendWith(MockitoExtension.class)
class ElevenStShoppingClientTest {

    @Mock
    private RestClient restClient;

    private ElevenStShoppingClient client;

    @BeforeEach
    void setUp() {
        client = new ElevenStShoppingClient(restClient);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
    }

    // ============================================================================================
    // RestClient 체인 Mock 헬퍼
    // ============================================================================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockRestClientResponse(byte[] body) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(any(URI.class))).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(byte[].class)).willReturn(body);
    }

    private byte[] buildXml(String... productEntries) {
        StringBuilder sb = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"EUC-KR\"?><ProductSearchResponse><Products>");
        for (String entry : productEntries) {
            sb.append(entry);
        }
        sb.append("</Products></ProductSearchResponse>");
        return sb.toString().getBytes(Charset.forName("EUC-KR"));
    }

    private String product(String name, int price) {
        return "<Product>" +
                "<ProductCode>P001</ProductCode>" +
                "<ProductName>" + name + "</ProductName>" +
                "<SalePrice>" + price + "</SalePrice>" +
                "<ProductImage>http://img.example.com/img.jpg</ProductImage>" +
                "<DetailPageUrl>http://detail.example.com</DetailPageUrl>" +
                "<Seller>테스트판매자</Seller>" +
                "<DeliveryFee>0</DeliveryFee>" +
                "<IsConditionDelivery>0</IsConditionDelivery>" +
                "</Product>";
    }

    // ============================================================================================
    // 테스트 케이스
    // ============================================================================================

    @Test
    @DisplayName("[단위] 가격 900원(< 1000) 상품은 가격 하한선 필터로 제거된다")
    void price_below_1000_is_filtered() {
        mockRestClientResponse(buildXml(product("수박 자르개", 900)));

        List<ShoppingItemDto> result = client.search("수박");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[단위] 비식품 키워드(커터) 포함 2500원 상품은 NON_FOOD_WORDS 필터로 제거된다")
    void nonFood_keyword_in_name_is_filtered() {
        mockRestClientResponse(buildXml(product("수박 껍질 커터", 2500)));

        List<ShoppingItemDto> result = client.search("수박");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[단위] 정상 식재료(수박 5kg 15000원)는 필터를 통과하여 1건 반환된다")
    void normal_food_product_passes_filter() {
        mockRestClientResponse(buildXml(product("수박 5kg 당일배송", 15000)));

        List<ShoppingItemDto> result = client.search("수박");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("수박 5kg 당일배송");
        assertThat(result.get(0).getPrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("[단위] 식품+비식품 혼합 시 식품(15000원)만 1건 반환된다")
    void mixed_products_returns_only_food() {
        mockRestClientResponse(buildXml(
                product("수박 5kg", 15000),
                product("수박 도구", 900)
        ));

        List<ShoppingItemDto> result = client.search("수박");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("[단위] 검색 키워드(수박)가 상품명(참외 1통)에 없으면 필터링된다")
    void product_without_keyword_is_filtered() {
        mockRestClientResponse(buildXml(product("참외 1통", 5000)));

        List<ShoppingItemDto> result = client.search("수박");

        assertThat(result).isEmpty();
    }
}
