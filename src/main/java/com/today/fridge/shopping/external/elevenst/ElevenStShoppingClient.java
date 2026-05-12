package com.today.fridge.shopping.external.elevenst;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.external.coupang.CoupangShoppingClient;
import com.today.fridge.shopping.type.ShippingType;
import com.today.fridge.shopping.type.StockStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Primary
@Component
// Todo: 11번가가  클라이언트 상속 받고 있음 > 이후 shoppingClient 공통 인터페이스로 수정하기
// 11번가의 XML 응답을'EUC-KR'로 읽고 다시 리스트로 변환하는 역할
public class ElevenStShoppingClient extends CoupangShoppingClient {

    private static final String BASE_URL = "http://openapi.11st.co.kr/openapi/OpenApiService.tmall";

    private final RestClient restClient;

    @Value("${app.elevenst.openapi.key:}")
    private String apiKey;

    public ElevenStShoppingClient(RestClient restClient) {
        super(restClient);
        this.restClient = restClient;
    }

    @Override
    public List<ShoppingItemDto> search(String keyword) {

        log.info("[ElevenStShoppingClient] API 호출 시작 - 검색어: {}", keyword);

        if (apiKey.isBlank()) {
            log.warn("[ElevenStShoppingClient] API 키 미설정, 건너뜀");
            return Collections.emptyList();
        }
        try {
            String encodedKeyword = URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
            // option=Categories: 응답에 카테고리 정보 포함 → 식품 카테고리 여부 판별에 사용
            String url = BASE_URL + "?key=" + apiKey
                    + "&apiCode=ProductSearch&keyword=" + encodedKeyword + "&pageSize=20&option=Categories";
            byte[] body = restClient.get()
                    .uri(java.net.URI.create(url))
                    .retrieve()
                    .body(byte[].class);

            if (body == null || body.length == 0) {
                log.warn("[ElevenStShoppingClient] 응답 데이터가 비어있음");
                return Collections.emptyList();
            }

            List<ShoppingItemDto> results = parseXml(body, keyword);
            log.info("[ElevenStShoppingClient] 검색 성공 - 결과 수: {}", results.size());
            return results;
        } catch (Exception e) {
            log.warn("[ElevenStShoppingClient] 검색 실패 keyword={}: {}", keyword, e.getMessage());
            return Collections.emptyList();
        }
    }

    // 식품으로 판별하는 카테고리 키워드 목록
    private static final java.util.Set<String> FOOD_CATEGORY_KEYWORDS = java.util.Set.of(
            "식품", "식재료", "과일", "채소", "야채", "정육", "수산", "육류", "곡류", "쌀",
            "음료", "건강식품", "신선", "냉동", "유제품", "계란", "두부", "김치", "장류",
            "조미료", "오일", "간식", "스낵", "제과", "빵", "떡", "국수", "라면", "견과"
    );

    private List<ShoppingItemDto> parseXml(byte[] body) throws Exception {
        return parseXml(body, null);
    }

    private List<ShoppingItemDto> parseXml(byte[] body, String filterKeyword) throws Exception {
        String rawXml = new String(body, Charset.forName("EUC-KR"));
        log.info("[ElevenStShoppingClient] XML 응답 (앞 400자): {}",
                rawXml.length() > 400 ? rawXml.substring(0, 400) + "..." : rawXml);

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new InputStreamReader(
                        new ByteArrayInputStream(body), Charset.forName("EUC-KR"))));

        // 응답의 카테고리 명칭에 식품 키워드가 포함되면 식품 카테고리로 판별
        boolean isFoodContext = hasFoodCategory(doc);
        log.info("[ElevenStShoppingClient] 식품 카테고리 감지: {}", isFoodContext);

        NodeList productNodes = doc.getElementsByTagName("Product");
        if (productNodes.getLength() == 0) {
            log.warn("[ElevenStShoppingClient] <Product> 태그 없음 — 응답 XML 확인 필요");
            return Collections.emptyList();
        }

        List<ShoppingItemDto> result = new ArrayList<>();
        for (int i = 0; i < productNodes.getLength(); i++) {
            Node node = productNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                ShoppingItemDto dto = toDto((Element) node);
                if (dto != null) {
                    // 식품 카테고리가 감지된 경우: 상품명에 키워드만 포함되면 통과
                    // 식품 카테고리 미감지: 기존 비식품 단어 제외 필터 적용
                    if (isFoodContext) {
                        if (filterKeyword == null || dto.getProductName() == null
                                || dto.getProductName().contains(filterKeyword)) {
                            result.add(dto);
                        }
                    } else {
                        if (filterKeyword == null || dto.getProductName() == null
                                || isFoodProductName(dto.getProductName(), filterKeyword)) {
                            result.add(dto);
                        }
                    }
                }
            }
        }
        return result;
    }

    // option=Categories 응답의 <CategoryName> 태그에서 식품 카테고리 키워드 탐색
    private boolean hasFoodCategory(Document doc) {
        NodeList categoryNames = doc.getElementsByTagName("CategoryName");
        for (int i = 0; i < categoryNames.getLength(); i++) {
            String name = categoryNames.item(i).getTextContent();
            if (name != null) {
                for (String foodKeyword : FOOD_CATEGORY_KEYWORDS) {
                    if (name.contains(foodKeyword)) {
                        log.info("[ElevenStShoppingClient] 식품 카테고리 발견: {}", name);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 가격에 콤마를 찍어서 보내줄때 콤마가 포함된 가격표시를 숫자로 바꿈
    private ShoppingItemDto toDto(Element el) {
        String productCode = getTagText(el, "ProductCode");
        String productName = getTagText(el, "ProductName");
        String salePriceStr = getTagText(el, "SalePrice");
        String imageUrl = getTagText(el, "ProductImage");
        String detailUrl = getTagText(el, "DetailPageUrl");
        String seller = getTagText(el, "Seller");
        String deliveryFeeStr = getTagText(el, "DeliveryFee");         // 배송비 (0=무료)
        String isConditionDelivery = getTagText(el, "IsConditionDelivery"); // 조건부 무료배송

        if (salePriceStr == null || salePriceStr.isBlank())
            return null;
        int price;
        try {
            price = Integer.parseInt(salePriceStr.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (price < 1000)
            return null;

        ShippingType shippingType = resolveShippingType(deliveryFeeStr, isConditionDelivery);

        return ShoppingItemDto.builder()
                .mallName("11번가")
                .mallProductId(productCode)
                .productName(productName)
                .price(price)
                .purchaseUrl(detailUrl)
                .imageUrl(imageUrl)
                .brand(seller)
                .shippingType(shippingType)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
    }

    private static final java.util.Set<String> NON_FOOD_WORDS = java.util.Set.of(
            "도구", "칼세트", "커터", "절단기", "자르개", "나이프", "써는기",
            "조각기", "케이스", "스티커", "모형", "미니어처", "장난감", "인형", "소품", "인테리어"
    );

    private boolean isFoodProductName(String productName, String keyword) {
        if (!productName.contains(keyword)) return false;
        String lower = productName.toLowerCase();
        for (String nonFood : NON_FOOD_WORDS) {
            if (lower.contains(nonFood)) return false;
        }
        return true;
    }

    private String getTagText(Element el, String tagName) {
        NodeList nodes = el.getElementsByTagName(tagName);
        if (nodes.getLength() == 0)
            return null;
        return nodes.item(0).getTextContent();
    }

    /**
     * 11번가 배송비/조건부 무료배송으로 ShippingType 결정
     * DeliveryFee=0 또는 IsConditionDelivery=1 → FREE, 그 외 → STANDARD
     */
    private ShippingType resolveShippingType(String deliveryFeeStr, String isConditionDelivery) {
        try {
            if (deliveryFeeStr != null && Integer.parseInt(deliveryFeeStr.trim()) == 0) {
                return ShippingType.FREE;
            }
        } catch (NumberFormatException ignored) { }
        if ("1".equals(isConditionDelivery)) {
            return ShippingType.FREE;
        }
        return ShippingType.STANDARD;
    }
}
