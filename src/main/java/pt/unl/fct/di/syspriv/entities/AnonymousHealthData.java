package pt.unl.fct.di.syspriv.entities;

import jakarta.persistence.*;
import pt.unl.fct.di.syspriv.anonymity.Anonymizable;

import java.time.LocalDate;
import java.util.*;

public class AnonymousHealthData implements Anonymizable {
    private Long id;

    private float glucose;
    
    private float patientBirthYear;

    private float patientPostalCode;

    private Date diagnosisDate;

    public AnonymousHealthData() {}


    public AnonymousHealthData(Long id, float patientBirthYear, float patientPostalCode,
                    float glucose, Date diagnosisDate) {
        this.id = id;
        this.patientBirthYear = patientBirthYear;
        this.patientPostalCode = patientPostalCode;
        this.glucose = glucose;
        this.diagnosisDate = diagnosisDate;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public String getStringId() { return Long.toString(id); }
    public void setId(Long id) { this.id = id; }
    public void setPatientPostalCode(float patientPostalCode) {
            this.patientPostalCode = patientPostalCode;
    }
    public float getPatientPostalCode() { return patientPostalCode; }
    public void setPatientBirthYear(float patientBirthYear) {
            this.patientBirthYear = patientBirthYear;
    }
    public float getPatientBirthYear() { return patientBirthYear; }
    public float getGlucose() { return glucose; }
    public void setGlucose(float glucose) { this.glucose = glucose; }
    public Date getDiagnosisDate() { return diagnosisDate; }
    public void setDiagnosisDate(Date diagnosisDate) {
        this.diagnosisDate = diagnosisDate;
    }

    // For debugging and logging purposes.
    @Override
    public String toString() {
        return "AnonymousHealthData={" +
                "id=" + getStringId() +
                ", glucose=" + Float.toString(glucose) + 
                ", diagnosisDate=" + diagnosisDate.toString() +
                '}';
    }

    @Override
    public Map<String, Float> getQuasiIdentifiers() {
        Map<String, Float> map = new HashMap<>();

        map.put("postalCode", patientPostalCode);
        map.put("birthdayYear", patientBirthYear);

        return map;
    }

    @Override
    public Object getSensitiveValue() {
        return glucose;
    }

    @Override
    public boolean haveSameQI(Anonymizable other) {
        return getQuasiIdentifiers().equals(other.getQuasiIdentifiers());
    }

    @Override
    public void generalize(Map<String, Float> min, Map<String, Float> max) {
        patientBirthYear = (float) ((min.get("birthdayYear") + max.get("birthdayYear")) / 2);
        patientPostalCode = (float) ((min.get("postalCode") + max.get("postalCode")) / 2);
    }
}
