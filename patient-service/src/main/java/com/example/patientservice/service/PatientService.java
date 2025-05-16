package com.example.patientservice.service;

import com.example.patientservice.dto.PatientRequestDTO;
import com.example.patientservice.exception.EmailAlreadyExitsException;
import com.example.patientservice.exception.PatientNotFoundException;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import com.example.patientservice.dto.PatientResponseDTO;
import com.example.patientservice.mapper.PatientMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Service
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();

        List<PatientResponseDTO> patientResponseDTOS =
                patients.stream().map(patient ->
                        PatientMapper.toDTO(patient)).toList();


        return patientResponseDTOS;
    }


    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExitsException("A patient with this Email already exists " + patientRequestDTO.getEmail() );
        }


        //sends to the database
        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));


        //returns responds?
        return PatientMapper.toDTO(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id,PatientRequestDTO patientRequestDTO) {
        Patient  patient = patientRepository.findById(id).
                orElseThrow(()->
                     new PatientNotFoundException("Patient not found with ID: "+id)
        );
        if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(),id)) {
            throw new EmailAlreadyExitsException("A patient with this Email already exists " + patientRequestDTO.getEmail() );
        }

        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));


        return PatientMapper.toDTO(patientRepository.save(patient));
    }


    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }
}
