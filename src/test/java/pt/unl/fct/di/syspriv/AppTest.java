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
                assertTrue(pdp.checkAccess(accountant, payment, "BillingAndPayment"));
            }
            for (var employee : employees){
                assertTrue(pdp.checkAccess(accountant, employee, "BillingAndPayment"));
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
}
