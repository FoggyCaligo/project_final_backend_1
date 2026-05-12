package com.today.fridge.shopping.external.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.type.ShippingType;

@ExtendWith(MockitoExtension.class)
class ShoppingExplainClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ShoppingExplainClient client;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.connectTimeout(any())).thenReturn(builder);
        when(builder.readTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);

        client = new ShoppingExplainClient(builder, "http://localhost:9999");
    }

    private ShoppingItemDto item(String mall, int price) {
        return ShoppingItemDto.builder()
                .mallName(mall)
                .productName("테스트 상품")
                .price(price)
                .shippingType(ShippingType.FREE)
                .build();
    }

    @Test
    @DisplayName("FastAPI가 정상 응답하면 explanation 문자열을 반환한다")
    void explain_success() {
        // given
        List<ShoppingItemDto> items = List.of(item("네이버쇼핑", 1_980));
        String expected = "무료배송으로 저렴하게 구매할 수 있는 두부입니다.";

        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(ShoppingExplainClient.ExplainResponse.class)))
                .thenReturn(new ShoppingExplainClient.ExplainResponse(expected));

        // when
        String result = client.explain("두부", items);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("FastAPI 호출이 실패하면 null을 반환하여 메인 흐름을 차단하지 않는다")
    void explain_fastApiFailure_returnsNull() {
        // given
        List<ShoppingItemDto> items = List.of(item("네이버쇼핑", 1_980));

        when(restTemplate.postForObject(anyString(), any(), eq(ShoppingExplainClient.ExplainResponse.class)))
                .thenThrow(new RuntimeException("연결 거부"));

        // when
        String result = client.explain("두부", items);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("FastAPI가 null 응답을 반환하면 null을 반환한다")
    void explain_nullResponse_returnsNull() {
        // given
        List<ShoppingItemDto> items = List.of(item("11번가", 2_100));

        when(restTemplate.postForObject(anyString(), any(), eq(ShoppingExplainClient.ExplainResponse.class)))
                .thenReturn(null);

        // when
        String result = client.explain("계란", items);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 items 목록으로도 호출이 가능하고 결과를 반환한다")
    void explain_emptyItems_callsApiWithoutError() {
        // given
        String expected = "가격 정보를 바탕으로 추천합니다.";
        when(restTemplate.postForObject(anyString(), any(), eq(ShoppingExplainClient.ExplainResponse.class)))
                .thenReturn(new ShoppingExplainClient.ExplainResponse(expected));

        // when
        String result = client.explain("두부", List.of());

        // then
        assertThat(result).isEqualTo(expected);
    }
}
