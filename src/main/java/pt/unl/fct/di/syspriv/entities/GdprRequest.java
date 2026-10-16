package pt.unl.fct.di.syspriv.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "gdpr_requests")
public class GdprRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_subject_id")
    private Patient patientSubject;

    @ManyToOne
    @JoinColumn(name = "employee_subject_id")
    private Employee employeeSubject;

    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "status", length = 20)
    @ColumnDefault("'PENDING'")
    private String status = "PENDING";

    @Column(name = "handled_by")
    private Long handledBy;

    public GdprRequest() {};

    public GdprRequest(String subjectType, String requestType, LocalDateTime requestedAt){
        this.subjectType = subjectType;
        this.requestType = requestType;
        this.requestedAt = requestedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

}
