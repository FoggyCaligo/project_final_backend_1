package com.today.fridge.shopping.external.elevenst;

import com.today.fridge.shopping.dto.ShoppingItemDto;
import com.today.fridge.shopping.external.coupang.CoupangShoppingClient2;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Primary
@Component
// Todo: 11번가가 쿠팡 클라이언트 상속 받고 있음 > 이후 shoppingClient 공통 인터페이스로 수정하기  
// 11번가의 XML 응답을'EUC-KR'로 읽고 다시 리스트로 변환하는 역할
public class ElevenStShoppingClient extends CoupangShoppingClient2 {

    private static final String BASE_URL =
            "http://openapi.11st.co.kr/openapi/OpenApiService.tmall";

    private final RestClient restClient;

    @Value("${elevenst.openapi.key:}")
    private String apiKey;

    public ElevenStShoppingClient(RestClient restClient) {
        super(restClient);
        this.restClient = restClient;
    }

    @Override
    public List<ShoppingItemDto> search(String keyword) {
        if (apiKey.isBlank()) {
            log.warn("[ElevenStShoppingClient] API 키 미설정, 건너뜀");
            return Collections.emptyList();
        }
        try {
            // encodedKeyword는 URLEncoder.encode를 사용해서 검색어에 띄어쓰기나 글자들을 컴퓨터용 언어로 바꿔주는 번역된 키워드 
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = BASE_URL + "?key=" + apiKey
                    + "&apiCode=ProductSearch&keyword=" + encodedKeyword + "&pageSize=10";
            //  데이터 가져오기 (byte배열) : EUC-KR 로 직접 해석하기 위해 가공되지 않은 '날것'의 데이터를 가져온다 
            byte[] body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);

            if (body == null || body.length == 0) {
                return Collections.emptyList();
            }
            return parseXml(body);
        } catch (Exception e) {
            log.warn("[ElevenStShoppingClient] 검색 실패 keyword={}: {}", keyword, e.getMessage());
            return Collections.emptyList();
        }
    }


    //  11번가 api는 xml을 사용, 따라서 euc-kr 을 사용한다 : Charset.forName('EUC-KR')
    //  DocumentBuilderFactory , getElementByTagName("Product") : XML에서 parse (XML은 큰 박스 범위에서 작은 상자들 중에 Product 태그를 찾아서 그 안에 있는 정보들을 ShoppingItemDto로 변환한다)
    private List<ShoppingItemDto> parseXml(byte[] body) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new InputStreamReader(
                        new ByteArrayInputStream(body), Charset.forName("EUC-KR"))));

        NodeList productsContainer = doc.getElementsByTagName("Products");
        if (productsContainer.getLength() == 0) {
            return Collections.emptyList();
        }

        NodeList productNodes = productsContainer.item(0).getChildNodes();
        List<ShoppingItemDto> result = new ArrayList<>();
        for (int i = 0; i < productNodes.getLength(); i++) {
            Node node = productNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "Product".equals(node.getNodeName())) {
                ShoppingItemDto dto = toDto((Element) node);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }
        return result;
    }


    //  가격에 콤마를 찍어서 보내줄때 콤마가 포함된 가격표시를 숫자로 바꿈 
    private ShoppingItemDto toDto(Element el) {
        String productCode = getTagText(el, "ProductCode");
        String productName = getTagText(el, "ProductName");
        String salePriceStr = getTagText(el, "SalePrice");
        String imageUrl = getTagText(el, "ProductImage");
        String detailUrl = getTagText(el, "DetailPageUrl");
        String seller = getTagText(el, "Seller");

        if (salePriceStr == null || salePriceStr.isBlank()) return null;
        int price;
        try {
            price = Integer.parseInt(salePriceStr.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (price <= 0) return null;

        return ShoppingItemDto.builder()
                .mallName("11번가")
                .mallProductId(productCode)
                .productName(productName)
                .price(price)
                .purchaseUrl(detailUrl)
                .imageUrl(imageUrl)
                .brand(seller)
                .shippingType(ShippingType.STANDARD)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
    }

    private String getTagText(Element el, String tagName) {
        NodeList nodes = el.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }
}
