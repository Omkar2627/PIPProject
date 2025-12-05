package com.taskmgmt.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    private static String tokenA;
    private static String tokenB;
    private final String userA = "alice@example.com";
    private final String userB = "bob@example.com";

    @Test
    @Order(1)
    void test_register_users() throws Exception {

        // Register A
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"email":"alice@example.com","password":"12345","name":"Alice"}
                        """))
                .andExpect(status().isOk());

        // Register B
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"email":"bob@example.com","password":"12345","name":"Bob"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void test_login_users() throws Exception {

        // Login A
        MvcResult r1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"email":"alice@example.com","password":"12345"}
                        """))
                .andExpect(status().isOk())
                .andReturn();

        tokenA = mapper.readTree(r1.getResponse().getContentAsString()).get("token").asText();

        // Login B
        MvcResult r2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"email":"bob@example.com","password":"12345"}
                        """))
                .andExpect(status().isOk())
                .andReturn();

        tokenB = mapper.readTree(r2.getResponse().getContentAsString()).get("token").asText();

        assertNotNull(tokenA);
        assertNotNull(tokenB);
    }

    @Test
    @Order(3)
    void test_full_task_flow() throws Exception {

        // --- Create Task ---
        String create = mapper.createObjectNode()
                .put("title", "Traditional Task")
                .put("description", "Created by traditional test")
                .put("dueDate", LocalDateTime.now().plusDays(1)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .put("priority", "MEDIUM")
                .toString();

        MvcResult r = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andReturn();

        Long taskId = mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        assertTrue(taskId > 0);

        // --- Retrieve ---
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Traditional Task"));

        // --- Update ---
        String update = mapper.createObjectNode()
                .put("title", "Traditional Task - updated").toString();

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk());

        // --- Assign to B ---
        String assign = mapper.createObjectNode()
                .put("assigneeEmail", userB)
                .toString();

        mockMvc.perform(post("/api/tasks/" + taskId + "/assign")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assign))
                .andExpect(status().isOk());

        // --- Assignee sees assigned task ---
        mockMvc.perform(get("/api/tasks/my")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.id == " + taskId + ")]").exists());
    }
}
