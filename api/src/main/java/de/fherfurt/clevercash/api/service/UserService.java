package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import de.fherfurt.clevercash.storage.models.*;
import de.fherfurt.clevercash.storage.repositories.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service class for managing users.
 * Provides methods for retrieving, creating, updating, and deleting users.
 *
 * @author Lilou Steffen
 */
@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    /**
     * Retrieves all users.
     *
     * @return a list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Creates or updates a user.
     *
     * @param user the user to be created or updated
     * @return the created or updated user
     */
    public User createOrUpdateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Updates an existing user with new data.
     *
     * @param userId     the ID of the user to update
     * @param newUserDTO the new user data
     * @return the updated user
     * @throws NoSuchElementException if the user with the specified ID is not found
     * @throws MappingException       if there is an error during mapping
     */
    public User updateUser(int userId, NewUserDTO newUserDTO) throws NoSuchElementException, MappingException {
        User userToUpdate = findUserByID(userId);
        User newUser = Mapper.newUserDTOToUser(newUserDTO);

        userToUpdate.setFirstName(newUser.getFirstName());
        userToUpdate.setLastName(newUser.getLastName());
        userToUpdate.setEmail(newUser.getEmail());
        userToUpdate.setBirthDate(newUser.getBirthDate());

        return createOrUpdateUser(userToUpdate);
    }

    /**
     * Finds a user by ID.
     *
     * @param userID the ID of the user to find
     * @return the found user
     * @throws NoSuchElementException if the user with the specified ID is not found
     */
    public User findUserByID(int userID) {
        return userRepository.findById(userID)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userID));
    }

    /**
     * Finds a user by email.
     *
     * @param mail the email of the user to find
     * @return the found user
     * @throws NoSuchElementException if the user with the specified email is not found
     */
    public User getUserByMail(String mail) {
        return userRepository.findByEmail(mail)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + mail));
    }

    /**
     * Deletes a user by ID.
     *
     * @param userID the ID of the user to delete
     */
    public void deleteUser(int userID) {
        userRepository.deleteById(userID);
    }
}
