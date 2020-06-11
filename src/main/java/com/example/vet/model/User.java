package com.example.vet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String _id;

    @NotBlank(message = "Name must not blank")
    private String name;

    private String about;

    @Max(value = 200, message = "Invalid age: Too large (max 200)")
    @Min(value = 18, message = "Invalid age: Too small (min 18)")
    private Integer age;

    private String birth;

    @Min(value = 0, message = "Height should not be negative")
    private Integer height;

    @Email(message = "Invalid email")
    private String email;

    @Pattern(regexp = "^0[0-9]{6,14}$")
    private String phone;

    private String address;

    @NotNull
    private Boolean activated;

    @NotNull
    private Date activatedDate;

    @NotNull
    private Date expirationDate;
}
