package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.api.models.output.UserDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import de.fherfurt.clevercash.api.TestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {
    private final String USER_CONTROLLER_BASE_URL = "/api/users";
    private MockMvc mockMvc;

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationService authenticationService;

    private User user;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        user = TestUtils.getTestUser1();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testGetAllUsers() throws Exception {
        // Arrange
        List<User> users = TestUtils.getTestUserList();

        List<UserDTO> userDTOs = users.stream().map(Mapper::userToUserDTO).toList();

        when(userService.getAllUsers()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userDTOs.get(0).getId()))
                .andExpect(jsonPath("$[0].email").value(userDTOs.get(0).getEmail()))
                .andExpect(jsonPath("$[0].firstName").value(userDTOs.get(0).getFirstName()))
                .andExpect(jsonPath("$[0].lastName").value(userDTOs.get(0).getLastName()))
                .andExpect(jsonPath("$[1].id").value(userDTOs.get(1).getId()))
                .andExpect(jsonPath("$[1].email").value(userDTOs.get(1).getEmail()))
                .andExpect(jsonPath("$[1].firstName").value(userDTOs.get(1).getFirstName()))
                .andExpect(jsonPath("$[1].lastName").value(userDTOs.get(1).getLastName()));
    }

    @Test
    void testGetUserById() throws Exception {
        // Arrange
        when(userService.findUserByID(1)).thenReturn(user);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL + "/{userID}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        // Arrange
        when(userService.findUserByID(1)).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL + "/{userID}", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserByIdNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL + "/{userID}", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserByEmail() throws Exception {
        // Arrange
        when(userService.getUserByMail(user.getEmail())).thenReturn(user);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL + "/email")
                        .param("email", user.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void testGetUserByEmailNotFound() throws Exception {
        // Arrange
        when(userService.getUserByMail(anyString())).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL + "/email")
                        .param("email", user.getEmail()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserByEmailNoAccess() throws Exception {
        // Arrange
        when(userService.getUserByMail(anyString())).thenReturn(user);
        doThrow(new AccessException()).when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(USER_CONTROLLER_BASE_URL + "/email")
                        .param("email", user.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateUser() throws Exception {
        // Arrange
        int userId = 1;
        String newEmail = "updated@example.com";
        String newFirstName = "Rainer";
        NewUserDTO updatedUserDTO = new NewUserDTO();
        updatedUserDTO.setEmail(newEmail);
        updatedUserDTO.setFirstName(newFirstName);
        updatedUserDTO.setBirthDate("2000-11-11");
        updatedUserDTO.setPassword("String123!*");

        User updatedUser = Mapper.newUserDTOToUser(updatedUserDTO);

        when(userService.updateUser(eq(userId), any(NewUserDTO.class))).thenReturn(updatedUser);
        when(userService.findUserByID(userId)).thenReturn(user);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        String userJson = objectMapper.writeValueAsString(updatedUserDTO);

        // Act & Assert
        mockMvc.perform(put(USER_CONTROLLER_BASE_URL + "/{userID}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.firstName").value(newFirstName));
    }

    @Test
    public void testUpdateUserNotFound() throws Exception {
        // Arrange
        int userId = 1;
        NewUserDTO newUser = new NewUserDTO();
        newUser.setFirstName("John");
        newUser.setLastName("Doe");
        newUser.setEmail("john.doe@example.com");
        newUser.setBirthDate("2000-10-10");
        newUser.setPassword("String123!*");

        when(userService.updateUser(
                eq(userId), any(NewUserDTO.class))).thenThrow(new NoSuchElementException("User not found"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(USER_CONTROLLER_BASE_URL + "/{userID}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUserInvalidInput() throws Exception {
        // Arrange
        int userId = 1;
        NewUserDTO newUser = new NewUserDTO();
        newUser.setFirstName("John");
        newUser.setLastName("Doe");
        newUser.setEmail("john.doe@example.com");
        newUser.setBirthDate("2000-10-10");
        newUser.setPassword("String123!*");

        when(userService.updateUser(
                eq(userId), any(NewUserDTO.class))).thenThrow(new MappingException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(USER_CONTROLLER_BASE_URL + "/{userID}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateUserNoAccess() throws Exception {
        // Arrange
        int userId = 1;
        String newEmail = "updated@example.com";
        String newFirstName = "Rainer";
        NewUserDTO updatedUserDTO = new NewUserDTO();
        updatedUserDTO.setEmail(newEmail);
        updatedUserDTO.setFirstName(newFirstName);
        updatedUserDTO.setBirthDate("2000-11-11");
        updatedUserDTO.setPassword("String123!*");

        User updatedUser = Mapper.newUserDTOToUser(updatedUserDTO);

        when(userService.updateUser(eq(userId), any(NewUserDTO.class))).thenReturn(updatedUser);
        when(userService.findUserByID(userId)).thenReturn(user);
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        String userJson = objectMapper.writeValueAsString(updatedUserDTO);

        // Act & Assert
        mockMvc.perform(put(USER_CONTROLLER_BASE_URL + "/{userID}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteUser() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(delete(USER_CONTROLLER_BASE_URL + "/{userID}",1))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteUserNoAccess() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1);
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(delete(USER_CONTROLLER_BASE_URL + "/{userID}",1))
                .andExpect(status().isForbidden());
    }
}
