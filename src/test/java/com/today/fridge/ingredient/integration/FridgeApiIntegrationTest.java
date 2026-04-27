package com.today.fridge.ingredient.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 냉장고 CRUD API 작업 시작 문서 v1.0 기준 통합 테스트 (H2, 전체 Spring 컨텍스트).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FridgeApiIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId1;
    private long userId2;
    @BeforeEach
    void seedUsersAndCategory() {
        jdbcTemplate.update("DELETE FROM user_ingredient");
        jdbcTemplate.update("DELETE FROM ingredient_master");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("INSERT INTO users (login_id, password_hash) VALUES (?,?)", "it-u1", "hash");
        userId1 = jdbcTemplate.queryForObject("SELECT user_id FROM users WHERE login_id = ?", Long.class, "it-u1");
        jdbcTemplate.update("INSERT INTO users (login_id, password_hash) VALUES (?,?)", "it-u2", "hash");
        userId2 = jdbcTemplate.queryForObject("SELECT user_id FROM users WHERE login_id = ?", Long.class, "it-u2");
    }

    @Test
    void list_withoutUserId_401_unauthorizedMessage() throws Exception {
        mockMvc.perform(get("/api/v1/fridge/ingredients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void create_then_list_returnsItem_withPageInfo() throws Exception {
        LocalDate exp = LocalDate.now(KST).plusDays(10);
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"통합상추","quantity":2,"unit":"개","storageType":"REFRIGERATED","expirationDate":"%s"}
                                """
                                        .formatted(exp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("통합상추"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.freshnessStatus").value("FRESH"));

        mockMvc.perform(get("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("통합상추"))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageInfo.page").value(0));
    }

    @Test
    void create_negativeQuantity_400_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_invalidStorageType_400() throws Exception {
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"storageType\":\"NOT_AN_ENUM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STORAGE_TYPE"));
    }

    @Test
    void create_withUnknownCategoryId_ignoresFieldAndCreates() throws Exception {
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"categoryId\":999999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void patch_onlyName_leavesQuantityUnchanged() throws Exception {
        LocalDate exp = LocalDate.now(KST).plusDays(10);
        String body =
                """
                {"name":"원래이름","quantity":5,"unit":"g","storageType":"REFRIGERATED","expirationDate":"%s"}
                """
                        .formatted(exp);
        String response = mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = ((Number) JsonPath.read(response, "$.data.ingredientId")).longValue();

        mockMvc.perform(patch("/api/v1/fridge/ingredients/" + id)
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"바뀐이름\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("바뀐이름"))
                .andExpect(jsonPath("$.data.quantity").value(5));

        mockMvc.perform(get("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("바뀐이름"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5));
    }

    @Test
    void patch_otherUserIngredient_404() throws Exception {
        LocalDate exp = LocalDate.now(KST).plusDays(5);
        String create =
                """
                {"name":"남의재료","storageType":"REFRIGERATED","expirationDate":"%s"}
                """
                        .formatted(exp);
        String response = mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.data.ingredientId")).longValue();

        mockMvc.perform(patch("/api/v1/fridge/ingredients/" + id)
                        .header("X-User-Id", userId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"해킹\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
    }

    @Test
    void delete_returnsDeletedId() throws Exception {
        LocalDate exp = LocalDate.now(KST).plusDays(3);
        String response = mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"삭제대상","storageType":"FROZEN","expirationDate":"%s"}
                                """
                                        .formatted(exp)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.data.ingredientId")).longValue();

        mockMvc.perform(delete("/api/v1/fridge/ingredients/" + id).header("X-User-Id", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedIngredientId").value(id));

        mockMvc.perform(get("/api/v1/fridge/ingredients").header("X-User-Id", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void list_invalidFreshnessFilter_400() throws Exception {
        mockMvc.perform(get("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .param("freshnessStatus", "NOT_AN_ENUM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void summary_reflectsExpiredSoonFresh() throws Exception {
        LocalDate today = LocalDate.now(KST);
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"만료","storageType":"REFRIGERATED","expirationDate":"%s"}
                                """
                                        .formatted(today.minusDays(1))));
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .header("X-User-Id", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"임박","storageType":"REFRIGERATED","expirationDate":"%s"}
                                """
                                        .formatted(today.plusDays(1))));
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                .header("X-User-Id", userId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                        {"name":"신선","storageType":"REFRIGERATED","expirationDate":"%s"}
                        """
                                .formatted(today.plusDays(10))));

        mockMvc.perform(get("/api/v1/fridge/summary").header("X-User-Id", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.expiredCount").value(1))
                .andExpect(jsonPath("$.data.soonCount").value(1))
                .andExpect(jsonPath("$.data.freshCount").value(1))
                .andExpect(jsonPath("$.data.soonItems.length()").value(1))
                .andExpect(jsonPath("$.data.soonItems[0].name").value("임박"));
    }
}
