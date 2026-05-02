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
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = BASE_URL + "?key=" + apiKey
                    + "&apiCode=ProductSearch&keyword=" + encodedKeyword + "&pageSize=10";

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
