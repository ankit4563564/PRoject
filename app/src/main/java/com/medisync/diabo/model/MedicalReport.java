package com.medisync.diabo.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;
import java.util.UUID;

@Entity(tableName = "medical_reports")
public class MedicalReport {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public Date uploadDate;
    public Date clinicalDate;
    public String reportType;
    public String organ;
    public String imageURL;
    public String pdfURL;
    public String extractedText;
    public String aiInsights;
    public String syncState;

    public MedicalReport() {
        this.id = UUID.randomUUID().toString();
        this.uploadDate = new Date();
        this.clinicalDate = new Date();
        this.syncState = "pending";
    }
    
    public Date getDisplayDate() {
        if (clinicalDate != null && uploadDate != null) {
            long diff = Math.abs(clinicalDate.getTime() - uploadDate.getTime());
            if (diff < 60000) { // 1 minute
                return uploadDate;
            }
        }
        return clinicalDate != null ? clinicalDate : uploadDate;
    }
}
