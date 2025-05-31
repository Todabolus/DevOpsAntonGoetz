package de.fherfurt.clevercash.api.models.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {
    private int id;
    private String streetNumber;
    private String street;
    private String postalCode;
    private String city;
    private String country;
    private String state;
}
