package com.medisync.diabo.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import java.util.Date;
import java.util.UUID;

@Entity(
    tableName = "medications",
    foreignKeys = @ForeignKey(
        entity = MedicalReport.class,
        parentColumns = "id",
        childColumns = "reportId",
        onDelete = ForeignKey.CASCADE
    )
)
public class Medication {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String dosage;
    public String frequency;
    public String instructions;
    public Date startDate;
    public Date endDate;
    public String prescribedBy;
    public String notes;
    public String sideEffects;
    public String alternatives;
    public boolean isActive;
    public String source;
    public String syncState;
    public String reportId;

    public Medication() {
        this.id = UUID.randomUUID().toString();
        this.startDate = new Date();
        this.isActive = true;
        this.source = "Unknown";
        this.syncState = "pending";
    }
}
