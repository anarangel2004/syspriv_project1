package pt.unl.fct.di.syspriv.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consent_records")
public class ConsentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;


    @ManyToOne
    @JoinColumn(name = "patient_subject_id")
    private Patient patientSubject;

    @ManyToOne
    @JoinColumn(name = "employee_subject_id")
    private Employee employeeSubject;

    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    @Column(name = "policy_version", length = 20)
    private String policyVersion;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name= "granted", nullable = false)
    private Boolean granted;

    public ConsentRecord() {}

    public ConsentRecord(String subjectType, String purpose, Boolean granted) {
        this.subjectType = subjectType;
        this.purpose = purpose;
        this.granted = granted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubjectType() { return subjectType; }

    public void setSubjectType( String subjectType){
        this.subjectType = subjectType;
    }

    public Patient getPatientSubject() {
        return patientSubject;
    }

    public void setPatientSubject(Patient patientSubject) {
        this.patientSubject = patientSubject;
    }

    public Employee getEmployeeSubject() {
        return employeeSubject;
    }

    public void setEmployeeSubject(Employee employeeSubject) {
        this.employeeSubject = employeeSubject;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }
}
