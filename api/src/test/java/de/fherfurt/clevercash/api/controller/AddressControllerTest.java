package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.models.input.NewAddressDTO;
import de.fherfurt.clevercash.api.service.AddressService;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.UserService;
import de.fherfurt.clevercash.storage.models.Address;
import de.fherfurt.clevercash.storage.models.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressControllerTest {
    private final String ADDRESS_CONTROLLER_BASE_URL = "/api/users/{userID}/address";

    private User user;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    AddressService addressService;

    @Mock
    UserService userService;

    @Mock
    AuthenticationService authenticationService;

    @InjectMocks
    AddressController addressController;

    private Address address;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(addressController).build();
        user = TestUtils.getTestUser1();
        address = TestUtils.getTestAddress();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testGetUserAddress() throws Exception {
        // Arrange
        Address address = TestUtils.getTestAddress();

        when(addressService.findUserAddress(anyInt())).thenReturn(address);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(ADDRESS_CONTROLLER_BASE_URL, user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(address.getCity()))
                .andExpect(jsonPath("$.country").value(address.getCountry()))
                .andExpect(jsonPath("$.street").value(address.getStreet()))
                .andExpect(jsonPath("$.streetNumber").value(address.getStreetNumber()))
                .andExpect(jsonPath("$.state").value(address.getState()))
                .andExpect(jsonPath("$.postalCode").value(address.getPostalCode()));
    }

    @Test
    void testGetUserAddressUserNotFound() throws Exception {
        // Arrange
        int userID = user.getId();
        when(addressService.findUserAddress(anyInt())).thenThrow(new NoSuchElementException("Address not found for user with ID: " + userID));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(ADDRESS_CONTROLLER_BASE_URL, userID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserAddressUserNoAccess() throws Exception {
        // Arrange
        int userID = user.getId();
        doThrow(new AccessException()).when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(ADDRESS_CONTROLLER_BASE_URL, userID))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddUserAddress() throws Exception {
        // Arrange
        NewAddressDTO newAddressDTO = TestUtils.getNewTestAddress();
        int userID = user.getId();

        when(addressService.addUserAddress(anyInt(), any(NewAddressDTO.class))).thenReturn(address);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAddressDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value(newAddressDTO.getCity()))
                .andExpect(jsonPath("$.postalCode").value(newAddressDTO.getPostalCode()));

        verify(addressService, times(1)).addUserAddress(anyInt(), any(NewAddressDTO.class));
    }


    @Test
    void testAddUserAddressNotFound() throws Exception {
        // arrange
        int userID = 1;
        when(addressService.addUserAddress(anyInt(), any(NewAddressDTO.class))).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestUtils.getNewTestAddress())))
                .andExpect(status().isNotFound());

        verify(addressService, times(1)).addUserAddress(anyInt(), any(NewAddressDTO.class));
    }

    @Test
    void testAddUserAddressBadRequest() throws Exception {
        // arrange
        int userID = 1;
        when(addressService.addUserAddress(anyInt(), any(NewAddressDTO.class))).thenThrow(new MappingException());
        doNothing().when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestUtils.getNewTestAddress())))
                .andExpect(status().isBadRequest());

        verify(addressService, times(1)).addUserAddress(anyInt(), any(NewAddressDTO.class));
    }

    @Test
    void testAddUserAddressNoAccess() throws Exception {
        // arrange
        int userID = 1;
        doThrow(new AccessException()).when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestUtils.getNewTestAddress())))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateUserAddress() throws Exception {
        // Arrange
        NewAddressDTO newAddressDTO = TestUtils.getNewTestAddress();
        int userID = user.getId();

        when(addressService.updateUserAddress(anyInt(), any(NewAddressDTO.class))).thenReturn(address);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAddressDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(newAddressDTO.getCity()))
                .andExpect(jsonPath("$.postalCode").value(newAddressDTO.getPostalCode()));

        verify(addressService, times(1)).updateUserAddress(anyInt(), any(NewAddressDTO.class));
    }


    @Test
    void testUpdateUserAddressNotFound() throws Exception {
        // arrange
        int userID = 1;
        when(addressService.updateUserAddress(anyInt(), any(NewAddressDTO.class))).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestUtils.getNewTestAddress())))
                .andExpect(status().isNotFound());

        verify(addressService, times(1)).updateUserAddress(anyInt(), any(NewAddressDTO.class));
    }

    @Test
    void testUpdateUserAddressBadRequest() throws Exception {
        // arrange
        int userID = 1;
        when(addressService.updateUserAddress(anyInt(), any(NewAddressDTO.class))).thenThrow(new MappingException());
        doNothing().when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestUtils.getNewTestAddress())))
                .andExpect(status().isBadRequest());

        verify(addressService, times(1)).updateUserAddress(anyInt(), any(NewAddressDTO.class));
    }

    @Test
    void testUpdateUserAddressNoAccess() throws Exception {
        // arrange
        int userID = 1;
        doThrow(new AccessException()).when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestUtils.getNewTestAddress())))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteUserAddress() throws Exception {
        // arrange
        int userID = user.getId();

        doNothing().when(addressService).deleteUserAddress(anyInt());
        doNothing().when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(addressService, times(1)).deleteUserAddress(anyInt());
    }

    @Test
    void testDeleteUserAddressNotFound() throws Exception {
        // arrange
        int userID = user.getId();

        doThrow(NoSuchElementException.class).when(addressService).deleteUserAddress(anyInt());
        doNothing().when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(addressService, times(1)).deleteUserAddress(anyInt());
    }

    @Test
    void testDeleteUserAddressNoAccess() throws Exception {
        // arrange
        int userID = user.getId();

        doThrow(AccessException.class).when(authenticationService).verifyUserAccess(any(int.class), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(ADDRESS_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}