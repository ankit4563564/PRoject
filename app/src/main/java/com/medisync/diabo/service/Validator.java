package com.medisync.diabo.service;

import java.util.Calendar;

public class Validator {
    
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static void validateUserProfile(String name, String ageStr, String heightStr, String weightStr, String diagnosisYearStr) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name cannot be empty");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new ValidationException("Name must be between 2 and 50 characters");
        }

        int age;
        try {
            age = Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Age must be a valid number");
        }
        if (age < 0 || age > 120) {
            throw new ValidationException("Age must be between 0 and 120");
        }

        double height;
        try {
            height = Double.parseDouble(heightStr.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Height must be a valid number");
        }
        if (height < 50 || height > 250) {
            throw new ValidationException("Height must be between 50 and 250 cm");
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Weight must be a valid number");
        }
        if (weight < 10 || weight > 300) {
            throw new ValidationException("Weight must be between 10 and 300 kg");
        }

        if (diagnosisYearStr != null && !diagnosisYearStr.trim().isEmpty()) {
            int diagnosisYear;
            try {
                diagnosisYear = Integer.parseInt(diagnosisYearStr.trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Diagnosis year must be a valid number");
            }
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (diagnosisYear < 1900 || diagnosisYear > currentYear) {
                throw new ValidationException("Diagnosis year must be between 1900 and " + currentYear);
            }
        }
    }

    public static void validateMedication(String name, String dosage, String frequency) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Medication name cannot be empty");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new ValidationException("Medication name must be between 2 and 100 characters");
        }

        if (dosage == null || dosage.trim().isEmpty()) {
            throw new ValidationException("Dosage cannot be empty");
        }
        if (dosage.length() < 1 || dosage.length() > 50) {
            throw new ValidationException("Dosage must be between 1 and 50 characters");
        }

        if (frequency == null || frequency.trim().isEmpty()) {
            throw new ValidationException("Frequency cannot be empty");
        }
        if (frequency.length() < 2 || frequency.length() > 100) {
            throw new ValidationException("Frequency must be between 2 and 100 characters");
        }
    }
}
