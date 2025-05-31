package de.fherfurt.clevercash.api.models.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewUserDTO {
    private String firstName;
    private String lastName;
    private String birthDate;
    private String password;
    private String email;
}
