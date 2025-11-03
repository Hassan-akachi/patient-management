package com.example.patientservice.dto;

import com.example.patientservice.dto.validation.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PatientRequestDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name can not be more than 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be Valid")
    private String email;

    @NotBlank(message = "Address is required")
    private  String address;

    @NotBlank(message = "Date of Birth is required")
    private  String dateOfBirth;

    @NotBlank(groups = CreatePatientValidationGroup.class, message = "Registered Address is required")
    private  String registeredAddress;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }
}


