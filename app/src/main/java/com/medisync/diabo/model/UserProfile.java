package com.medisync.diabo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profiles")
public class UserProfile {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int age;
    public String gender;
    public Double height;
    public Double weight;
    public byte[] profilePhotoData;
    public boolean enableAI;
    
    // Diabetes Specific Fields
    public String diabetesType;
    public Integer diagnosisYear;
    public String treatmentType;
    public String comorbidities; // Stored as comma-separated string or JSON
    public String familyHistory;

    public UserProfile() {
        this.name = "User";
        this.age = 30;
        this.gender = "male";
        this.enableAI = true;
        this.diabetesType = "Type 2";
        this.treatmentType = "Oral";
        this.familyHistory = "None";
    }
}
