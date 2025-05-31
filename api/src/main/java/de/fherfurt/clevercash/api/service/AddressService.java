package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewAddressDTO;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.Address;
import de.fherfurt.clevercash.storage.models.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Service class for managing user addresses.
 * Provides methods to find, add, update, and delete user addresses.
 *
 * @author Anton Götz
 */
@AllArgsConstructor
@Service
public class AddressService {
    private final UserService userService;

    /**
     * Finds the address associated with a given user ID.
     *
     * @param userID the ID of the user
     * @return the address of the user
     * @throws NoSuchElementException if the user or address is not found
     */
    public Address findUserAddress(int userID) throws NoSuchElementException {
        User user = userService.findUserByID(userID);
        Address address = user.getAddress();
        if (address == null) {
            throw new NoSuchElementException("Address not found for user with ID: " + userID);
        }
        return address;
    }

    /**
     * Adds a new address to a user.
     *
     * @param userID        the ID of the user
     * @param newAddressDTO the new address data transfer object
     * @return the updated address of the user
     * @throws NoSuchElementException if the user is not found
     * @throws MappingException       if there is an error during mapping
     */
    public Address addUserAddress(int userID, NewAddressDTO newAddressDTO) throws NoSuchElementException, MappingException {
        User userToUpdate = userService.findUserByID(userID);
        HelperFunctions.validateAddress(newAddressDTO);
        Address address = Mapper.addressDTOToAddress(newAddressDTO);

        address.getUsers().add(userToUpdate);
        userToUpdate.setAddress(address);
        return userService.createOrUpdateUser(userToUpdate).getAddress();
    }

    /**
     * Updates the address of a user.
     *
     * @param userID        the ID of the user
     * @param newAddressDTO the new address data transfer object
     * @return the updated address of the user
     * @throws NoSuchElementException   if the user is not found
     * @throws MappingException         if there is an error during mapping
     * @throws IllegalArgumentException if the new address is invalid
     */
    public Address updateUserAddress(int userID, NewAddressDTO newAddressDTO) throws NoSuchElementException, MappingException, IllegalArgumentException {
        User userToUpdate = userService.findUserByID(userID);
        HelperFunctions.validateAddress(newAddressDTO);

        Address oldAddress = userToUpdate.getAddress();
        if (oldAddress != null) {
            oldAddress.getUsers().remove(userToUpdate);
        }

        Address address = Mapper.addressDTOToAddress(newAddressDTO);
        address.getUsers().add(userToUpdate);
        userToUpdate.setAddress(address);

        return userService.createOrUpdateUser(userToUpdate).getAddress();
    }

    /**
     * Deletes the address of a user.
     *
     * @param userID the ID of the user
     * @throws NoSuchElementException if the user or address is not found
     */
    public void deleteUserAddress(int userID) throws NoSuchElementException {
        User user = userService.findUserByID(userID);
        Address address = user.getAddress();

        address.getUsers().remove(user);
        user.setAddress(null);

        userService.createOrUpdateUser(user);
    }
}
