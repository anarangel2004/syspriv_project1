package pt.unl.fct.di.syspriv.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "birthdate", nullable = false)
    private Date birthdate;

    @Column(length = 50)
    private String insurance;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @ManyToOne
    @JoinColumn(name = "primaryDoctor_id")
    private Employee primaryDoctor;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "patientSubject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConsentRecord> consentRecords = new ArrayList<>();

    @OneToMany(mappedBy = "patientSubject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GdprRequest> gdprRequests = new ArrayList<>();

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @Column(name = "erasure_requested")
    private Boolean erasureRequested = false;

    @Column(name = "erasure_completed_at")
    private LocalDateTime erasureCompletedAt;

    public Patient() {}

    public Patient(String name, Date birthdate, String insurance, String street, String postalCode, Employee primaryDoctorId) {
        this.name = name;
        this.birthdate = birthdate;
        this.insurance = insurance;
        this.street = street;
        this.postalCode = postalCode;
        this.primaryDoctor = primaryDoctorId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getStringId() { return Long.toString(id); }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Date getBirthdate() { return birthdate; }
    public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }
    public String getInsurance() { return insurance; }
    public void setInsurance(String insurance) { this.insurance = insurance; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public Employee getPrimaryDoctor() { return primaryDoctor; }
    public void setPrimaryDoctor(Employee primaryDoctor) { this.primaryDoctor = primaryDoctor; }
    public List<Appointment> getAppointments() { return appointments; }
    public void setAppointments(List<Appointment> appointments) { this.appointments = appointments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt; }
    public List<ConsentRecord> getConsentRecords() { return consentRecords; }
    public void setConsentRecords(List<ConsentRecord> consentRecords){ this.consentRecords = consentRecords; }
    public List<GdprRequest> getGdprRequests() { return gdprRequests; }
    public void setGdprRequests(List<GdprRequest> gdprRequests){ this.gdprRequests = gdprRequests; }
    public LocalDateTime getRetentionUntil() { return retentionUntil; }
    public void setRetentionUntil(LocalDateTime retentionUtil) {this.retentionUntil = retentionUtil; }
    public Boolean getErasureRequested() { return erasureRequested; }
    public void setErasureRequested(Boolean erasureRequested) { this.erasureRequested = erasureRequested; }
    public LocalDateTime getErasureCompletedAt() { return erasureCompletedAt; }
    public void setErasureCompletedAt(LocalDateTime erasureCompletedAt) { this.erasureCompletedAt = erasureCompletedAt; }

    // For debugging and logging purposes.
    @Override
    public String toString() {
        return "Patient{" +
                "id=" + Long.toString(id) +
                ", name='" + name + '\'' +
                ", birthdate=" + birthdate +
                ", insurance='" + insurance + '\'' +
                ", street='" + street + '\'' +
                ", postalCode='" + postalCode + '\'' +
                '}';
    }
}
