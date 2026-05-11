package com.today.fridge.shopping.external.naver;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * NaverShoppingClient 단위 테스트
 * - RestClient 체인을 Mock으로 대체하여 외부 API 호출 없이 JSON 파싱·필터링 로직 검증
 * - 가격 하한선(< 500원) 필터, 최저가 선택, API 키 미설정 처리 검증 포함
 */
@ExtendWith(MockitoExtension.class)
class NaverShoppingClientTest {

    @Mock
    private RestClient restClient;

    private NaverShoppingClient client;

    @BeforeEach
    void setUp() {
        client = new NaverShoppingClient(restClient);
        ReflectionTestUtils.setField(client, "clientId", "test-client-id");
        ReflectionTestUtils.setField(client, "clientSecret", "test-client-secret");
    }

    // ============================================================================================
    // RestClient 체인 Mock 헬퍼
    // ============================================================================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockRestClientResponse(NaverShoppingResponse response) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(anyString(), any(Object[].class))).willReturn(headersSpec);
        given(headersSpec.header(anyString(), anyString())).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(NaverShoppingResponse.class)).willReturn(response);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockRestClientThrows() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(anyString(), any(Object[].class))).willReturn(headersSpec);
        given(headersSpec.header(anyString(), anyString())).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(NaverShoppingResponse.class))
                .willThrow(new RestClientException("Connection refused"));
    }

    private NaverShoppingResponse buildResponse(NaverShoppingResponse.Item... items) {
        return new NaverShoppingResponse(items.length, 1, items.length, List.of(items));
    }

    private NaverShoppingResponse.Item item(String title, String lprice) {
        return new NaverShoppingResponse.Item(
                title, "http://link.example.com", null,
                lprice, "0", "네이버쇼핑", "p001",
                null, null, null, null, "0"
        );
    }

    // ============================================================================================
    // 테스트 케이스
    // ============================================================================================

    @Test
    @DisplayName("[단위] 정상 응답 — 사과 15000원 1건이 그대로 반환된다")
    void normal_response_returns_single_item() {
        mockRestClientResponse(buildResponse(item("사과 1kg", "15000")));

        List<ShoppingItemDto> result = client.search("사과");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualTo(15000);
        assertThat(result.get(0).getProductName()).isEqualTo("사과 1kg");
    }

    @Test
    @DisplayName("[단위] 가격 300원(< 500) 상품은 가격 하한선 필터로 제거되어 빈 리스트를 반환한다")
    void price_below_500_is_filtered() {
        mockRestClientResponse(buildResponse(item("사과 스티커", "300")));

        List<ShoppingItemDto> result = client.search("사과");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[단위] items가 빈 배열이면 빈 리스트를 반환한다")
    void empty_items_returns_emptyList() {
        mockRestClientResponse(new NaverShoppingResponse(0, 1, 0, List.of()));

        List<ShoppingItemDto> result = client.search("사과");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[단위] 여러 상품 중 최저가(3500원) 1건만 반환된다")
    void multiple_items_returns_only_cheapest() {
        mockRestClientResponse(buildResponse(
                item("사과 2kg", "5000"),
                item("사과 1kg", "3500"),
                item("사과 5kg", "7000")
        ));

        List<ShoppingItemDto> result = client.search("사과");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualTo(3500);
    }

    @Test
    @DisplayName("[단위] RestClientException 발생 시 예외를 전파하지 않고 빈 리스트를 반환한다")
    void restClientException_returns_emptyList() {
        mockRestClientThrows();

        List<ShoppingItemDto> result = client.search("사과");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[단위] API 키(clientId)가 빈 값이면 외부 호출 없이 빈 리스트를 반환한다")
    void blank_apiKey_returns_emptyList() {
        ReflectionTestUtils.setField(client, "clientId", "");
        ReflectionTestUtils.setField(client, "clientSecret", "");

        List<ShoppingItemDto> result = client.search("사과");

        assertThat(result).isEmpty();
    }
}
