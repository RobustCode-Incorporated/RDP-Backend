package com.robustcode.delivery.user.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.dto.CreateUserRequest;
import com.robustcode.delivery.user.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserController.class)
class UserControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private ObjectMapper objectMapper;


    @MockBean
    private UserService userService;



    @Test
    @WithMockUser
    void shouldCreateUser() throws Exception {


        CreateUserRequest request =
                new CreateUserRequest(
                        "driver@test.com",
                        "password",
                        "John",
                        "Driver",
                        User.Role.DRIVER
                );


        User user =
                User.builder()
                        .id(1L)
                        .email("driver@test.com")
                        .firstName("John")
                        .lastName("Driver")
                        .role(User.Role.DRIVER)
                        .build();



        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(user);



        mockMvc.perform(
                post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )

        .andExpect(status().isCreated())

        .andExpect(jsonPath("$.email")
                .value("driver@test.com"))

        .andExpect(jsonPath("$.role")
                .value("DRIVER"));

    }

}