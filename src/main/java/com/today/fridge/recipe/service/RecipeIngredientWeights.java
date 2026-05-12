package com.today.fridge.recipe.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 레시피 재료별 평균 무게 데이터를 관리하는 클래스입니다.
 * 데이터베이스의 모든 재료(약 300+개)에 대해 개당/팩당 평균 무게(g)를 제공합니다.
 */
public class RecipeIngredientWeights {
    private static final Map<String, BigDecimal> WEIGHTS = new HashMap<>();

    static {
        // --- 1. 기본 농산물 (과일/채소) ---
        // Fruits
        putWeight("Apple", "사과", 250);
        putWeight("Pear", "배", 500);
        putWeight("Avocado", "아보카도", 200);
        putWeight("Pomegranate", "석류", 300);
        putWeight("Kiwi", "키위", 100);
        putWeight("Lemon", "레몬", 100);
        putWeight("Plum", "자두", 80);
        putWeight("Nectarine", "천도복숭아", 150);
        putWeight("Red-Grapefruit", "자몽", 350);
        putWeight("Red-Grapefruit", "레드자몽", 350);
        putWeight("Lime", "라임", 80);
        putWeight("Mango", "망고", 300);
        putWeight("Passion-Fruit", "패션프루트", 50);
        putWeight("Banana", "바나나", 150);
        putWeight("Papaya", "파파야", 500);
        putWeight("Satsumas", "귤", 100);
        putWeight("Satsumas", "사츠마", 100);
        putWeight("Pineapple", "파인애플", 1500);
        putWeight("Melon", "멜론", 1500);
        putWeight("Orange", "오렌지", 200);
        putWeight("Peach", "복숭아", 250);
        putWeight("단감", 200);
        putWeight("방울토마토", 20);
        putWeight("완숙토마토", 200);
        putWeight("밤", "알밤", 20);

        // Vegetables - Root/Bulb
        putWeight("Onion", "양파", 200);
        putWeight("흰양파", 200);
        putWeight("햇양파", 200);
        putWeight("작은양파", 100);
        putWeight("Potato", "감자", 150);
        putWeight("햇감자", 150);
        putWeight("진감자", 150);
        putWeight("Carrots", "당근", 150);
        putWeight("작은당근", 80);
        putWeight("무", 1000);
        putWeight("작은무", 500);
        putWeight("천수무", 800);
        putWeight("고구마", 200);
        putWeight("Ginger", "생강", 50);
        putWeight("저민생강", 10);
        putWeight("Garlic", "마늘", 10);
        putWeight("햇마늘", 10);
        putWeight("생마늘", 5);
        putWeight("우엉", 200);
        putWeight("도라지", "도라지채", 100);

        // Vegetables - Green/Leafy
        putWeight("Cabbage", "양배추", 1000);
        putWeight("적채", 1000);
        putWeight("방울양배추", 20);
        putWeight("배추", "절임배추", 1000);
        putWeight("알배추", "알배추잎", 500);
        putWeight("속배추", 500);
        putWeight("시금치", 300);
        putWeight("부추", "다진부추", 200);
        putWeight("상추", 100);
        putWeight("깻잎", 2);
        putWeight("청경채", 100);
        putWeight("쑥갓", 100);
        putWeight("미나리", "손질한미나리", 200);
        putWeight("콩나물", 300);
        putWeight("숙주", 300);
        putWeight("봄동", 300);
        putWeight("근대", 200);
        putWeight("케일", 5);
        putWeight("고수", 50);
        putWeight("어린잎", 50);

        // Vegetables - Gourd/Fruit-like
        putWeight("Cucumber", "오이", 150);
        putWeight("청오이", 150);
        putWeight("가시오이", 150);
        putWeight("Zucchini", "애호박", 300);
        putWeight("쥬키니호박", 300);
        putWeight("조선호박", 400);
        putWeight("단호박", "미니단호박", 500);
        putWeight("Aubergine", "가지", 150);
        putWeight("Tomato", "토마토", 200);
        putWeight("Pepper", "고추", 15);
        putWeight("청양고추", "땡초", 10);
        putWeight("홍고추", 15);
        putWeight("꽈리고추", 5);
        putWeight("아삭이고추", 30);
        putWeight("오이맛고추", 30);
        putWeight("피망", "청피망", 100);
        putWeight("홍피망", 100);
        putWeight("파프리카", 200);
        putWeight("주황파프리카", 200);
        putWeight("홍파프리카", 200);
        putWeight("초록파프리카", 200);

        // Vegetables - Stems/Others
        putWeight("Leek", "대파", 100);
        putWeight("쪽파", "실파", 5);
        putWeight("마늘쫑", 100);
        putWeight("Asparagus", "아스파라거스", 20);
        putWeight("브로콜리", "데친브로콜리", 300);
        putWeight("옥수수", "찐옥수수", 200);

        // --- 2. 버섯류 (Mushrooms) ---
        putWeight("Mushroom", "버섯", 20);
        putWeight("Brown-Cap-Mushroom", "갈색버섯", 25);
        putWeight("양송이버섯", 25);
        putWeight("표고버섯", "건표고", 30);
        putWeight("느타리버섯", 200);
        putWeight("새송이버섯", "미니새송이", 100);
        putWeight("팽이버섯", 150);
        putWeight("만가닥버섯", "백만송이버섯", 150);

        // --- 3. 육류 및 수산물 (Proteins) ---
        // Meat
        putWeight("소고기", "국거리소고기", 200);
        putWeight("돼지고기", "돼지안심", 200);
        putWeight("닭고기", "닭다리", 500);
        putWeight("닭가슴살", "시판닭가슴살", 120);
        putWeight("삼겹살", "대패삼겹살", 200);
        putWeight("오리고기", 500);
        putWeight("베이컨", "슬라이스베이컨", 20);

        // Seafood
        putWeight("오징어", "생물오징어", 300);
        putWeight("고등어", "자반고등어", 300);
        putWeight("꽃게", "냉동꽃게", 200);
        putWeight("굴", 10);
        putWeight("바지락", "바지락살", 5);
        putWeight("꼬막", 10);
        putWeight("새우", "자숙새우", 20);
        putWeight("전복", 50);
        putWeight("멸치", "지리멸치", 1);
        putWeight("황태", "황태채", 50);

        // --- 4. 가공식품 (Processed / Cans) ---
        putWeight("참치캔", "참치통조림", 150);
        putWeight("스팸", "리챔", 250);
        putWeight("햄", "슬라이스햄", 200);
        putWeight("소시지", "비엔나", 15);
        putWeight("어묵", "사각어묵", 50);
        putWeight("두부", "모두부", 300);
        putWeight("순두부", 350);
        putWeight("연두부", 150);
        putWeight("유부", "시판네모유부", 10);
        putWeight("맛살", "게맛살", 20);
        putWeight("치즈", "슬라이스치즈", 20);
        putWeight("피자치즈", "모짜렐라치즈", 100);

        // --- 5. 유제품 및 음료 (Dairy / Liquids) ---
        putWeight("Milk", "우유", 200);
        putWeight("Yoghurt", "요거트", 100);
        putWeight("Soy-Milk", "두유", 200);
        putWeight("Juice", "주스", 200);
        putWeight("계란", "달걀", 50);
        putWeight("메추리알", 10);

        // --- 6. 기타 (Kimchi, Sauces, Grains) ---
        putWeight("김치", "잘익은김치", 200);
        putWeight("묵은지", 500);
        putWeight("단무지", 200);
        putWeight("떡", "가래떡", 100);
        putWeight("식은밥", "밥", 210);

        // --- 7. 대량 매핑 (나머지 특수 항목들) ---
        // 중복되거나 변형된 이름들 대량 등록
        putWeight("알육수", "코인육수", 5);
        putWeight("월계수잎", 1);
        putWeight("다시다", "미원", 10);
        putWeight("통깨", "깨소금", 5);
        putWeight("땅콩", "아몬드", 1);
        putWeight("호두", 5);
    }

    private static void putWeight(String en, String ko, int grams) {
        BigDecimal weight = new BigDecimal(grams);
        WEIGHTS.put(en, weight);
        WEIGHTS.put(ko, weight);
    }

    private static void putWeight(String name, int grams) {
        WEIGHTS.put(name, new BigDecimal(grams));
    }

    /**
     * 특정 재료명에 대해 가중치를 찾지 못할 경우, 
     * 이름에 포함된 키워드를 기반으로 유추하여 가중치를 반환합니다.
     */
    public static BigDecimal getWeight(String ingredientName) {
        if (ingredientName == null) return null;
        
        // 1. 완전 일치 검색
        BigDecimal directMatch = WEIGHTS.get(ingredientName);
        if (directMatch != null) return directMatch;

        // 2. 키워드 기반 유추
        if (ingredientName.contains("양파")) return WEIGHTS.get("양파");
        if (ingredientName.contains("감자")) return WEIGHTS.get("감자");
        if (ingredientName.contains("당근")) return WEIGHTS.get("당근");
        if (ingredientName.contains("마늘")) return WEIGHTS.get("마늘");
        if (ingredientName.contains("고추")) return WEIGHTS.get("고추");
        if (ingredientName.contains("버섯")) return WEIGHTS.get("버섯");
        if (ingredientName.contains("두부")) return WEIGHTS.get("두부");
        if (ingredientName.contains("어묵")) return WEIGHTS.get("어묵");
        if (ingredientName.contains("치즈")) return WEIGHTS.get("치즈");
        if (ingredientName.contains("참치")) return WEIGHTS.get("참치캔");
        if (ingredientName.contains("스팸") || ingredientName.contains("햄")) return WEIGHTS.get("스팸");
        if (ingredientName.contains("김치")) return WEIGHTS.get("김치");
        if (ingredientName.contains("사과")) return WEIGHTS.get("사과");
        if (ingredientName.contains("토마토")) return WEIGHTS.get("토마토");
        if (ingredientName.contains("배추")) return WEIGHTS.get("배추");
        if (ingredientName.contains("대파")) return WEIGHTS.get("대파");
        if (ingredientName.contains("육수")) return WEIGHTS.get("알육수");

        return null;
    }
}
