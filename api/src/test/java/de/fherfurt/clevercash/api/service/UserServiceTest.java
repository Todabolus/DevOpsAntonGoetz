package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private User user;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = TestUtils.getTestUser1();
    }

    @Test
    void testGetAllUsers(){
        // arrange
        when(userRepository.findAll()).thenReturn(TestUtils.getTestUserList());

        // act
        List<User> users = userService.getAllUsers();

        // assert
        assertEquals(2, users.size());
        assertTrue(users.contains(TestUtils.getTestUser1()));
        assertTrue(users.contains(TestUtils.getTestUser2()));
    }

    @Test
    void testCreateOrUpdateUserUpdate() {
        // arrange
        when(userService.createOrUpdateUser(user)).thenReturn(user);

        // act
        User createdUser = userService.createOrUpdateUser(user);

        // assert
        assertEquals(user, createdUser);
    }

    @Test
    void testUpdateUser() throws MappingException {
        // Arrange
        int userId = user.getId();
        NewUserDTO updatedUser = TestUtils.getNewTestUser1();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // Act
        User result = userService.updateUser(userId, updatedUser);

        // Assert
        assertEquals(userId, result.getId());
        assertEquals(updatedUser.getFirstName(), result.getFirstName());
        assertEquals(updatedUser.getLastName(), result.getLastName());
        assertEquals(updatedUser.getEmail(), result.getEmail());
    }

    @Test
    void testFindUserByID() {
        // arrange
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // act
        User result = userService.findUserByID(user.getId());

        // assert
        assertEquals(user, result);
    }

    @Test
    void testFindUserByIDNotFound() {
        // arrange
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        // act & assert
        assertThrows(NoSuchElementException.class, () -> userService.findUserByID(user.getId()));
    }

    @Test
    void testGetUserByMail() {
        // arrange
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // act
        User result = userService.getUserByMail(user.getEmail());

        // assert
        assertEquals(user, result);
    }

    @Test
    void testGetUserByMailNotFound() {
        // arrange
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        // act & assert
        assertThrows(NoSuchElementException.class, () -> userService.getUserByMail(user.getEmail()));
    }

    @Test
    void testDeleteUser() {
        // arrange
        int userID = user.getId();

        // act
        userService.deleteUser(userID);

        // assert
        verify(userRepository, times(1)).deleteById(userID);
    }
}