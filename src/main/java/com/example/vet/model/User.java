package com.example.vet.model;

import com.example.vet.validation.Password;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String _id;

    @NotBlank(message = "Name must not blank")
    private String name;

    @Max(value = 200, message = "Invalid age: Too large (max 200)")
    @Min(value = 18, message = "Invalid age: Too small (min 18)")
    private String age;

    @NotNull(message = "Password required")
    @Password(message = "Bad password. Require at least 1 number, 1 uppercase letter and at least 8 character")
    private String password;

    @Email(message = "Invalid email")
    private String email;
}
