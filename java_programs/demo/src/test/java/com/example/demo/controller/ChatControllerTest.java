package com.example.demo.controller;

import com.example.demo.tools.NoteRetriever;

import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean NoteRetriever retriever;   // 构造注入需要这个 bean，哪怕用不上
    @MockitoBean MeterRegistry registry;

    @Test
    void 空问题返回400() throws Exception {
        mvc.perform(get("/api/chat/stream").param("question", ""))
           .andExpect(status().isBadRequest());
    }

    @Test
    void 超长问题返回400() throws Exception {
        String longQuestion  = "线".repeat(501);
        mvc.perform(get("/api/chat/stream").param("question", longQuestion ))
           .andExpect(status().isBadRequest());
    }

    @Test
    void 正常问题返回200() throws Exception {
        // 用 xss-test 走测试桩：不调真实 LLM，确定性 200
        mvc.perform(get("/api/chat/stream").param("question", "xss-test"))
           .andExpect(status().isOk());
    }
}