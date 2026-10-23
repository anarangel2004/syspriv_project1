    package pt.unl.fct.di.syspriv;

    import static org.junit.jupiter.api.Assertions.assertTrue;
    import static org.junit.jupiter.api.Assertions.assertFalse;

    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.Test;
    import pt.unl.fct.di.syspriv.entities.*;
    import pt.unl.fct.di.syspriv.storage.Hibernate;
    import pt.unl.fct.di.syspriv.xacml.XACMLPDP;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.StandardCopyOption;
    import java.time.LocalDateTime;
    import java.util.List;

    /**
     * Unit test for simple App.
     */
    public class AppTestLab2 {

        /**
         * Rigorous Test :-)
         */
        @Test
        public void shouldAnswerWithTrue() {
            assertTrue(true);
        }

        @BeforeAll
        public static void createTestDb() throws IOException {
            Files.copy(Path.of("privsys.db"), Path.of("testDB.db"), StandardCopyOption.REPLACE_EXISTING);
        }

        // 1. Marketing is permitted when consent is granted and not withdrawn.
        @Test
        public void marketingWhenConsentAndNotWithdrawn() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // add data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            ConsentRecord c = new ConsentRecord("PATIENT", "marketing", true);
            c.setPatientSubject(p);
            p.addConsentRecord(c);
            db.persist(p);

            // test
            var pdp = new XACMLPDP();
            var employees = db.sql("SELECT * FROM employees", Employee.class);
            var patients = db.sql("SELECT * FROM patients", Patient.class);

            for (var patient : patients) {
                for (var consent : patient.getConsentRecords()) {
                    if (consent.getPurpose().equals("marketing") && consent.getGranted() && consent.getWithdrawnAt() == null) {
                        for (var employee : employees) {
                            assertTrue(pdp.checkAccessGDPR(employee, patient, "marketing", "marketing"));
                            tested = true;
                        }
                    }
                }
            }

            assertTrue(tested);
        }

        // 2. Marketing is denied when consent is withdrawn.
        @Test
        public void marketingDeniedWhenConsentWithdrawn() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // add data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            ConsentRecord c = new ConsentRecord("PATIENT", "marketing", true);
            c.setPatientSubject(p);
            p.addConsentRecord(c);
            c.setWithdrawnAt(LocalDateTime.now());
            db.persist(p);

            // test
            var pdp = new XACMLPDP();
            var employees = db.sql("SELECT * FROM employees", Employee.class);
            var patients = db.sql("SELECT * FROM patients", Patient.class);
            for (var patient : patients) {
                for (var consent : patient.getConsentRecords()) {
                    if (consent.getPurpose().equals("marketing") && consent.getWithdrawnAt() != null) {
                        for (var employee : employees) {
                            assertFalse(pdp.checkAccessGDPR(employee, patient, "marketing", "marketing"));
                            tested = true;
                        }
                    }
                }
            }

            assertTrue(tested);
        }


        // 3. Marketing is denied when consent was never granted.
        @Test
        public void marketingDeniedWhenConsentNotGranted() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // create data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            ConsentRecord c = new ConsentRecord("PATIENT", "marketing", false);
            c.setPatientSubject(p);
            p.addConsentRecord(c);
            db.persist(p);

            // test
            var pdp = new XACMLPDP();
            var employees = db.sql("SELECT * FROM employees", Employee.class);
            var patients = db.sql("SELECT * FROM patients", Patient.class);
            for (var patient : patients) {
                for (var consent : patient.getConsentRecords()) {
                    if (consent.getPurpose().equals("marketing") && !consent.getGranted()){
                        for (var employee : employees) {
                            assertFalse(pdp.checkAccessGDPR(employee, patient, "marketing", "marketing"));
                            tested = true;
                        }
                    }
                }
            }

            assertTrue(tested);
        }

        // 4. A doctor cannot access a record where erasure-requested == true
        @Test
        public void doctorCannotAccessWhenErasureRequested() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // create data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            GdprRequest r = new GdprRequest("PATIENT", "ERASURE", LocalDateTime.now());
            r.setPatientSubject(p);
            r.setCompletedAt(LocalDateTime.now());
            p.addGdprRecord(r);
            db.persist(p);

            // test
            var pdp = new XACMLPDP();
            var patients = db.sql("SELECT * FROM patients WHERE erasure_requested = TRUE", Patient.class);
            for (var patient : patients) {
                var doctor = patient.getPrimaryDoctor();
                assertFalse(pdp.checkAccessGDPR(doctor, patient, "Appointment", "read"));
                tested = true;
            }

            assertTrue(tested);
        }

        // 5. A DPO can access a record where erasure-requested == true.
        @Test
        public void dpoAccessWhenErasureRequested() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // create data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            GdprRequest r = new GdprRequest("PATIENT", "ERASURE", LocalDateTime.now());
            r.setPatientSubject(p);
            p.addGdprRecord(r);
            db.persist(p);
            db.persist(new Employee("b", "DPO", "c", true));

            // test
            var pdp = new XACMLPDP();
            var dpos = db.sql("SELECT * FROM employees WHERE role = 'DPO'", Employee.class);
            var patients = db.sql("SELECT * FROM patients WHERE erasure_requested = TRUE", Patient.class);
            for (var dpo : dpos){
                for (var patient : patients) {
                    assertTrue(pdp.checkAccessGDPR(dpo, patient, "GDPR Request", "read"));
                    tested = true;
                }
            }

            assertTrue(tested);
        }

        // 6. A doctor cannot access a record where retention-expired == true.
        @Test
        public void doctorCannotAccessWhenRetentionExpired() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // create data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            p.setRetentionUntil(LocalDateTime.now().minusDays(1));
            db.persist(p);

            // test
            var pdp = new XACMLPDP();
            var patients = db.sql("SELECT * FROM patients WHERE retention_until < CURRENT_TIMESTAMP", Patient.class);
            for (var patient : patients) {
                var doctor = patient.getPrimaryDoctor();
                assertFalse(pdp.checkAccessGDPR(doctor, patient, "Appointment", "read"));
                tested = true;
            }

            assertTrue(tested);
        }

        // 7. An auditor can access a record where retention-expired == true.
        @Test
        public void auditorAccessWhenRetentionExpired() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested = false;

            // create data
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctors.get(0);
            Patient p = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            p.setRetentionUntil(LocalDateTime.now().minusDays(1));
            db.persist(p);
            db.persist(new Employee("b", "Auditor", "c", true));

            // test
            var pdp = new XACMLPDP();
            var auditors = db.sql("SELECT * FROM employees WHERE role = 'Auditor'", Employee.class);
            var patients = db.sql("SELECT * FROM patients WHERE retention_until < CURRENT_TIMESTAMP", Patient.class);
            for (var auditor : auditors) {
                for (var patient : patients) {
                    assertTrue(pdp.checkAccessGDPR(auditor, patient, "Audit", "read"));
                    tested = true;
                }
            }

            assertTrue(tested);
        }

        // 8. Health data is denied unless consent-research == true.
        @Test
        public void healthDataDeniedUnlessResearchConsent() throws Exception {
            var db = Hibernate.getInstance();
            boolean tested1 = false;
            boolean tested2 = false;

            // create data
            var doctorsList = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            Employee d = doctorsList.get(0);

            // patient with consent
            Patient p1 = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            ConsentRecord consentRecord = new ConsentRecord("PATIENT", "research", true);
            consentRecord.setPatientSubject(p1);
            p1.addConsentRecord(consentRecord);
            db.persist(p1);

            // patient without consent
            Patient p2 = new Patient("a", java.sql.Date.valueOf("2000-01-01"), "b", "c", "0000-000", d);
            db.persist(p2);

            // test
            var pdp = new XACMLPDP();
            var doctors = db.sql("SELECT * FROM employees WHERE role = 'Doctor'", Employee.class);
            var patients = db.sql("SELECT * FROM patients", Patient.class);
            for (var doctor : doctors) {
                for (var patient : patients) {
                    boolean erased = Boolean.TRUE.equals(patient.getErasureRequested());
                    boolean expired = patient.getRetentionUntil() != null && patient.getRetentionUntil().isBefore(LocalDateTime.now());
                    if (erased || expired) continue;

                    boolean consents = false;
                    for (var c : patient.getConsentRecords()) {
                        if (c.getPurpose().equals("research") && c.getGranted() && c.getWithdrawnAt() == null) {
                            consents = true;
                            break;
                        }
                    }

                    if (consents){
                        assertTrue(pdp.checkAccessGDPR(doctor, patient, "research", "research"));
                        tested1 = true;
                    }
                    else{
                        assertFalse(pdp.checkAccessGDPR(doctor, patient, "research", "research"));
                        tested2 = true;
                    }

                }
            }

            assertTrue(tested1 && tested2);
        }
    }