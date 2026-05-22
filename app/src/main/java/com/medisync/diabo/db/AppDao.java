package com.medisync.diabo.db;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.medisync.diabo.model.*;
import java.util.List;

@Dao
public interface AppDao {
    // User Profile
    @Query("SELECT * FROM user_profiles LIMIT 1")
    LiveData<UserProfile> getUserProfile();

    @Query("SELECT * FROM user_profiles LIMIT 1")
    UserProfile getUserProfileSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfile profile);

    @Update
    void updateUserProfile(UserProfile profile);

    @Query("DELETE FROM user_profiles")
    void clearUserProfiles();

    // Medical Reports
    @Query("SELECT * FROM medical_reports ORDER BY uploadDate DESC")
    LiveData<List<MedicalReport>> getAllReports();

    @Query("SELECT * FROM medical_reports ORDER BY uploadDate DESC LIMIT 1")
    MedicalReport getLatestReportSync();

    @Query("SELECT * FROM medical_reports ORDER BY uploadDate DESC")
    List<MedicalReport> getAllReportsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReport(MedicalReport report);

    @Delete
    void deleteReport(MedicalReport report);

    // Lab Results
    @Query("SELECT * FROM lab_results WHERE reportId = :reportId")
    LiveData<List<LabResult>> getLabResultsForReport(String reportId);

    @Query("SELECT * FROM lab_results ORDER BY testDate DESC")
    LiveData<List<LabResult>> getAllLabResults();

    @Query("SELECT * FROM lab_results ORDER BY testDate DESC")
    List<LabResult> getAllLabResultsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLabResults(List<LabResult> results);

    // Medications
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY startDate DESC")
    LiveData<List<Medication>> getActiveMedications();

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY startDate DESC")
    List<Medication> getActiveMedicationsSync();

    @Query("SELECT * FROM medications WHERE isActive = 0 ORDER BY endDate DESC")
    LiveData<List<Medication>> getPastMedications();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMedications(List<Medication> medications);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMedication(Medication medication);

    @Query("UPDATE medications SET isActive = 0, endDate = :endDate WHERE id = :id")
    void markMedicationInactive(String id, java.util.Date endDate);

    @Delete
    void deleteMedication(Medication medication);
}
