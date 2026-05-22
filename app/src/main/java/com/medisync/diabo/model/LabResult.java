package com.medisync.diabo.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import java.util.Date;
import java.util.UUID;

@Entity(
    tableName = "lab_results",
    foreignKeys = @ForeignKey(
        entity = MedicalReport.class,
        parentColumns = "id",
        childColumns = "reportId",
        onDelete = ForeignKey.CASCADE
    )
)
public class LabResult {
    @PrimaryKey
    @NonNull
    public String id;
    public String testName;
    public String parameter;
    public double value;
    public String stringValue;
    public String unit;
    public String normalRange;
    public String status;
    public Date testDate;
    public String category;
    public String syncState;
    public String reportId; // Foreign key-like relationship

    public LabResult() {
        this.id = UUID.randomUUID().toString();
        this.testDate = new Date();
        this.syncState = "pending";
    }
}
