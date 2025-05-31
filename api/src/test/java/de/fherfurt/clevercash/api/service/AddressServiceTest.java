package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewAddressDTO;
import de.fherfurt.clevercash.storage.models.Address;
import de.fherfurt.clevercash.storage.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class AddressServiceTest {

    private User user;
    @Mock
    UserService userService;

    @InjectMocks
    private AddressService addressService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = TestUtils.getTestUser1();
    }


    @Test
    void testFindUserAddress() {
        // arrange
        int userID = user.getId();
        Address expectedAddress = TestUtils.getTestAddress();
        when(userService.findUserByID(anyInt())).thenReturn(user);
        user.setAddress(expectedAddress);

        // act
        Address actualAddress = addressService.findUserAddress(userID);

        // assert
        assertEquals(expectedAddress, actualAddress);
    }

    @Test
    void testFindUserAddressAddressNotFound() {
        // arrange
        int userID = user.getId();
        when(userService.findUserByID(anyInt())).thenReturn(user);
        assertNull(user.getAddress());

        // act and assert
        assertThrows(NoSuchElementException.class, () -> {
            addressService.findUserAddress(userID);
        });
    }

    @Test
    void testAddUserAddress() throws MappingException {
        // arrange
        int userID = user.getId();
        NewAddressDTO newAddressDTO = TestUtils.getNewTestAddress();

        when(userService.findUserByID(anyInt())).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);
        assertNull(user.getAddress());

        // act
        addressService.addUserAddress(userID, newAddressDTO);
        Address newAddress = Mapper.addressDTOToAddress(newAddressDTO);

        // assert
        assertNotNull(user.getAddress());
        assertEquals(newAddress, user.getAddress());
    }

    @Test
    void testUpdateUserAddressUserWithAddress() throws MappingException {
        // Arrange
        int userID = user.getId();
        Address oldAddress = TestUtils.getTestAddress();
        user.setAddress(oldAddress);
        oldAddress.getUsers().add(user);
        NewAddressDTO newAddressDTO = TestUtils.getNewTestAddress();
        newAddressDTO.setCity("NewCity");

        when(userService.findUserByID(userID)).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // Act
        addressService.updateUserAddress(userID, newAddressDTO);

        // Assert
        assertNotNull(user.getAddress());
        assertNotEquals(user.getAddress(), oldAddress);
        assertFalse(oldAddress.getUsers().contains(user));
    }

    @Test
    void testUpdateUserAddressUserWithoutAddress() throws MappingException {
        // Arrange
        int userID = user.getId();
        Address oldAddress = TestUtils.getTestAddress();
        NewAddressDTO newAddressDTO = TestUtils.getNewTestAddress();
        newAddressDTO.setCity("NewCity");

        when(userService.findUserByID(userID)).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // Act
        addressService.updateUserAddress(userID, newAddressDTO);

        // Assert
        assertNotNull(user.getAddress());
        assertNotEquals(user.getAddress(), oldAddress);
    }

    @Test
    void testDeleteUserAddress() {
        // Arrange
        int userID = user.getId();
        Address address = TestUtils.getTestAddress();
        user.setAddress(address);
        address.getUsers().add(user);
        assertTrue(address.getUsers().contains(user));
        assertNotNull(user.getAddress());

        when(userService.findUserByID(userID)).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // Act
        addressService.deleteUserAddress(userID);

        // Assert
        assertNull(user.getAddress());
        assertFalse(address.getUsers().contains(user));
    }
}