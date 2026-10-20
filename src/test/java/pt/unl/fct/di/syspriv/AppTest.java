package pt.unl.fct.di.syspriv;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.unl.fct.di.syspriv.entities.*;
import pt.unl.fct.di.syspriv.storage.Hibernate;
import pt.unl.fct.di.syspriv.xacml.XACMLPDP;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    // 1. Patients can access all of their own data
    @Test
    public void shouldAllowPatientToOwnData() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var patients = db.sql("SELECT * FROM patients", Patient.class);
        for (var patient : patients){
            var canAccess = pdp.checkAccess(patient, patient, "something irrelevant");
            assertTrue(canAccess);
        }
    }

    // 2. Employees can access all of their own data.
    @Test
    public void shouldAllowEmployeeToOwnData() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var employees = db.sql("SELECT * FROM employees", Employee.class);
        for (var employee : employees){
            var canAccess = pdp.checkAccess(employee, employee, "something irrelevant");
            assertTrue(canAccess);
        }
    }

    // 3. Clerks can only access appointment data for the purpose of scheduling and check-in.
    @Test
    public void shouldAllowClerkToAppointmentData() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var clerks = db.sql("SELECT * FROM employees WHERE role = 'Clerk'", Employee.class);
        var appointments = db.sql("SELECT * FROM appointments", Appointment.class);

        assertFalse(clerks.isEmpty(), "No Clerk found in the database to test with.");
        assertFalse(appointments.isEmpty(), "No Appointment found in the database to test with.");

        for (var clerk : clerks){
            for (var appointment : appointments){
                assertTrue(pdp.checkAccess(clerk, appointment, "Scheduling"));
                assertTrue(pdp.checkAccess(clerk, appointment, "Check-in"));
            }

        }
    }

    // 4. Accountants can only access payment data or employee data for the purpose of billing and payments.
    @Test
    public void shouldAllowAccountantToPaymentAndEmployeeData() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var accountants = db.sql("SELECT * FROM employees WHERE role = 'Accountant'", Employee.class);
        var payments = db.sql("SELECT * FROM payments", Payment.class);
        var employees = db.sql("SELECT * FROM employees", Employee.class);

        for (var accountant : accountants){
            for (var payment : payments){
                assertTrue(pdp.checkAccess(accountant, payment, "Billing and Payments"));
            }
            for (var employee : employees){
                assertTrue(pdp.checkAccess(accountant, employee, "Billing and Payments"));
            }
        }
    }

    // 5. Doctors and Nurses can only access their own data, their patients’ data and appointment data outside of an emergency.
    @Test
    public void shouldAllowDoctorAndNurseToData() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var doctorsNurses = db.sql("SELECT * FROM employees WHERE role = 'Doctor' OR role = 'Nurse'", Employee.class);
        var patients = db.sql("SELECT * FROM patients", Patient.class);
        var appointments = db.sql("SELECT * FROM appointments", Appointment.class);

        for (var doctorNurse : doctorsNurses){
            // own data
            assertTrue(pdp.checkAccess(doctorNurse, doctorNurse, ""));

            // patient data
            for (var patient : patients){
                if(patient.getPrimaryDoctor().getId().equals(doctorNurse.getId()))
                    assertTrue(pdp.checkAccess(doctorNurse, patient, ""));
            }

            // appointment data
            for (var appointment : appointments){
                if(appointment.getEmployee().getId().equals(doctorNurse.getId()))
                    assertTrue(pdp.checkAccess(doctorNurse, appointment, ""));
            }
        }
    }

    // 6. Doctors and Nurses can access any patient information in an emergency.
    @Test
    public void shouldAllowDoctorAndNurseToDataInEmergency() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var doctorsNurses = db.sql("SELECT * FROM employees WHERE role = 'Doctor' OR role = 'Nurse'", Employee.class);
        var patients = db.sql("SELECT * FROM patients", Patient.class);

        for (var doctorNurse : doctorsNurses){
            for (var patient : patients){
                assertTrue(pdp.checkAccess(doctorNurse, patient, "Emergency"));
            }
        }
    }

    // 7. IT Admins can access any information in an emergency.
    @Test
    public void shouldAllowITAdminToDataInEmergency() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var itAdmins = db.sql("SELECT * FROM employees WHERE role = 'ITAdmin'", Employee.class);
        var employees = db.sql("SELECT * FROM employees", Employee.class);
        var patients = db.sql("SELECT * FROM patients", Patient.class);
        var appointments = db.sql("SELECT * FROM appointments", Appointment.class);
        var payments = db.sql("SELECT * FROM payments", Payment.class);

        for (var itAdmin : itAdmins){
            for (var employee : employees){
                assertTrue(pdp.checkAccess(itAdmin, employee, "Emergency"));
            }
            for (var patient : patients){
                assertTrue(pdp.checkAccess(itAdmin, patient, "Emergency"));
            }
            for (var appointment : appointments){
                assertTrue(pdp.checkAccess(itAdmin, appointment, "Emergency"));
            }
            for (var payment : payments){
                assertTrue(pdp.checkAccess(itAdmin, payment, "Emergency"));
            }
        }
    }

    // 8. Outside of an emergency, IT Admins should only be able to access employee data for hiring.
    @Test
    public void shouldAllowITAdminToDataWhenHiring() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var itAdmins = db.sql("SELECT * FROM employees WHERE role = 'ITAdmin'", Employee.class);
        var employees = db.sql("SELECT * FROM employees", Employee.class);

        for (var itAdmin : itAdmins){
            for (var employee : employees){
                assertTrue(pdp.checkAccess(itAdmin, employee, "Hiring"));
            }
        }
    }

    // Lab 2
    @BeforeAll
    static void fillTestData() throws Exception {
        var db = Hibernate.getInstance();
        var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
        assertFalse(doctors.isEmpty(), "No Doctor found to assign as primary doctor.");
        Employee doctor = doctors.get(0);

        patientMarketingTrueWithdrawFalse(db, doctor);
        patientErasureRequest(db, doctor);
        patientRetentionUntil(db, doctor);
    }

    // 1. Have at least one patient with marketing consent set to true and consent withdrawn set to false.
    private static void patientMarketingTrueWithdrawFalse(Hibernate db, Employee doctor) throws Exception {
        String query = "SELECT p.* FROM patients p JOIN consent_records c ON c.patient_subject_id = p.id " +
                "WHERE c.purpose = 'marketing' AND c.granted = TRUE AND c.withdrawn_at IS NULL";
        if (!db.sql(query, Patient.class).isEmpty()) return;

        Patient p = new Patient("Patient 1", java.sql.Date.valueOf("2000-01-01"), "Insurance A", "Street 1", "2800-000", doctor);
        ConsentRecord consent = new ConsentRecord("PATIENT", "marketing", true);
        consent.setGrantedAt(LocalDateTime.now());
        consent.setPolicyVersion("v1.0");
        consent.setSource("web");
        consent.setPatientSubject(p);
        List<ConsentRecord> consents = p.getConsentRecords();
        consents.add(consent);
        p.setConsentRecords(consents);
        db.persist(p);
    }

    // 2. Have at least one patient with an erasure request.
    private static void patientErasureRequest(Hibernate db, Employee doctor) throws Exception {
        String query = "SELECT p.* FROM patients p JOIN gdpr_requests g ON g.patient_subject_id = p.id " +
                "WHERE g.request_type = 'ERASURE'";
        if (!db.sql(query, Patient.class).isEmpty()) return;

        Patient p = new Patient("Patient 2", java.sql.Date.valueOf("2000-01-01"), "Insurance B", "Street 2", "1234-567", doctor);
        GdprRequest request = new GdprRequest("PATIENT", "ERASURE", LocalDateTime.now());
        request.setPatientSubject(p);
        List<GdprRequest> requests = p.getGdprRequests();
        requests.add(request);
        db.persist(p);
    }

    // 3. Insert at least one patient whose retentionUntil is in the past.
    private static void patientRetentionUntil(Hibernate db, Employee doctor) throws Exception {
        String query = "SELECT * FROM patients WHERE retention_until < CURRENT_TIMESTAMP";
        if (!db.sql(query, Patient.class).isEmpty()) return;

        Patient p = new Patient("Patient 3", java.sql.Date.valueOf("2000-01-01"), "Insurance C", "Street 3", "0987-654", doctor);
        p.setRetentionUntil(LocalDateTime.now().minusYears(1));
        db.persist(p);
    }


    // 1. Marketing is permitted when consent is granted and not withdrawn.
    @Test
    public void marketingWhenConsentAndNotWithdraw() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var employees = db.sql("SELECT * FROM employees", Employee.class);

        for (var employee : employees){
            for(var consent : employee.getConsentRecords()){
                if(consent.granted())
            }
        }
    }
}