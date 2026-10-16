package pt.unl.fct.di.syspriv;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import pt.unl.fct.di.syspriv.entities.Appointment;
import pt.unl.fct.di.syspriv.entities.Patient;
import pt.unl.fct.di.syspriv.entities.Employee;
import pt.unl.fct.di.syspriv.entities.Payment;
import pt.unl.fct.di.syspriv.storage.Hibernate;
import pt.unl.fct.di.syspriv.xacml.XACMLPDP;

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

    @Test
    public void shouldAllowAccountantToPaymentAndEmployeeData() throws Exception {
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var accountants = db.sql("SELECT * FROM employees WHERE role = 'Accountant'", Employee.class);
        var payments = db.sql("SELECT * FROM payments", Payment.class);
        var employees = db.sql("SELECT * FROM employees", Employee.class);

        for (var accountant : accountants){
            for (var payment : payments){
                assertTrue(pdp.checkAccess(accountant, payment, "BillingAndPayment"));
            }
            for (var employee : employees){
                assertTrue(pdp.checkAccess(accountant, employee, "BillingAndPayment"));
            }
        }
    }

    @Test
    public  void shouldAllowDoctorAndNurseToPatientData() throws Exception{
        var db = Hibernate.getInstance();
        var pdp = new XACMLPDP();
        var doctorsNurses = db.sql("SELECT * FROM employees WHERE role = 'Doctors' OR role = 'Nurses'", Employee.class);
        var patients = db.sql("SELECT * FROM patients", Patient.class);
        var appointments = db.sql("SELECT * FROM appointments", Appointment.class);

        for ( var doctorNurse : doctorsNurses){
            for (var patient : patients){
                assertTrue(pdp.checkAccess(doctorNurse, patient, ""));
            }
            for (var appointment : appointments){
                assertTrue(pdp.checkAccess(doctorNurse, appointment, ""));
            }
        }
    }
}
