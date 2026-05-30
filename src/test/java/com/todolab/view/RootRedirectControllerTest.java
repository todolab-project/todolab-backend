package com.todolab.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RootRedirectControllerTest {

    @Test
    @DisplayName("루트 경로는 Today 화면으로 이동한다")
    void root_redirectsToToday() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RootRedirectController()).build();

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/today"));
    }
}
