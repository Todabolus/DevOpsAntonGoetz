package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.models.input.AuthenticationRequest;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.api.models.output.AuthenticationResponse;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.UserService;
import de.fherfurt.clevercash.storage.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationControllerTest {
    private final String AUTHENTICATION_CONTROLLER_BASE_URL = "/api/auth";

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testRegisterSuccessful() throws Exception {
        NewUserDTO newUserDTO = TestUtils.getNewTestUser1();

        when(userService.getUserByMail(eq(newUserDTO.getEmail()))).thenThrow(NoSuchElementException.class);
        when(authenticationService.isValidUserInput(any(NewUserDTO.class))).thenReturn(true);
        when(authenticationService.register(any(NewUserDTO.class)))
                .thenReturn(new AuthenticationResponse(1, "token"));

        mockMvc.perform(post(AUTHENTICATION_CONTROLLER_BASE_URL+ "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").value(1))
                .andExpect(jsonPath("$.access_token").value("token"));
    }

    @Test
    void testRegisterUserAlreadyExists() throws Exception {
        NewUserDTO newUserDTO = TestUtils.getNewTestUser1();

        when(userService.getUserByMail(eq("test@example.com"))).thenReturn(new User());

        mockMvc.perform(post(AUTHENTICATION_CONTROLLER_BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("User with provided email already exists"));
    }

    @Test
    void testRegisterInvalidUserInput() throws Exception {
        NewUserDTO newUserDTO = TestUtils.getNewTestUser1();

        when(userService.getUserByMail(eq(newUserDTO.getEmail()))).thenThrow(NoSuchElementException.class);
        when(authenticationService.isValidUserInput(any(NewUserDTO.class))).thenReturn(false);

        mockMvc.perform(post(AUTHENTICATION_CONTROLLER_BASE_URL+ "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("Invalid user input"));
    }

    @Test
    void testRegisterInvalidUserInputInvalidDateFormat() throws Exception {
        NewUserDTO newUserDTO = TestUtils.getNewTestUser1();

        when(userService.getUserByMail(eq(newUserDTO.getEmail()))).thenThrow(NoSuchElementException.class);
        when(authenticationService.isValidUserInput(any(NewUserDTO.class))).thenReturn(true);
        when(authenticationService.register(any(NewUserDTO.class))).thenThrow(DateTimeParseException.class);

        mockMvc.perform(post(AUTHENTICATION_CONTROLLER_BASE_URL+ "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthenticateSuccessful() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");

        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenReturn(new AuthenticationResponse(1, "token"));

        mockMvc.perform(post(AUTHENTICATION_CONTROLLER_BASE_URL+ "/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("token"))
                .andExpect(jsonPath("$.user_id").value(1));
    }
}