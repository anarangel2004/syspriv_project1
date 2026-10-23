package pt.unl.fct.di.syspriv;

import pt.unl.fct.di.syspriv.entities.*;
import pt.unl.fct.di.syspriv.storage.Hibernate;
import pt.unl.fct.di.syspriv.xacml.XACMLPDP;
import pt.unl.fct.di.syspriv.util.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class DataSafe {

    private static final Logger logger = LoggerFactory.getLogger(DataSafe.class);
    private final Hibernate hibernate = Hibernate.getInstance();
    private final XACMLPDP pdp;
    private Object loggedInPerson;

    public DataSafe() throws Exception {
        this.pdp = new XACMLPDP();
        logger.info("DataSafe instance initialized.");
    }

    public static void main(String[] args) {
        try {
            DataSafe app = new DataSafe();
            app.run();

        } catch (Exception e) {
            logger.error("Failed to initialize application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        
        if (!login(scanner)) {
            logger.error("Login failed. Exiting...");
            return;
        }

        displayWelcome();

        boolean running = true;
        while (running) {
            int action = promptMainAction(scanner);

            switch (action) {
                case 1 -> {
                    String context = promptContext(scanner);
                    if (context == null) {
                        System.out.println("Invalid context. Please try again.");
                        continue;
                    }

                    int tableChoice = promptTableSelection(scanner);
                    if (tableChoice == 5) {
                        continue; // volta ao menu principal, não sai da app
                    }

                    Long id = promptId(scanner);
                    if (id == null) {
                        System.out.println("Invalid ID. Please try again.");
                        continue;
                    }

                    handleDataAccess(context, tableChoice, id);
                }
                case 2 -> exportMyData();
                case 3 -> requestErasure();
                case 4 -> {
                    running = false;
                    System.out.println("Exiting...");
                }
                default -> System.out.println("Invalid option.");
            }
        }

        scanner.close();
    }

    private boolean login(Scanner scanner) {
        System.out.println("Are you an employee or patient?");
        System.out.println("1. Employee");
        System.out.println("2. Patient");
        System.out.print("Select an option: ");

        String choice = scanner.nextLine();

        System.out.print("Enter your name to login: ");
        String loginName = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                List<Employee> employees = hibernate.jpql("FROM Employee", Employee.class);
                for (Employee e : employees) {
                    if (e.getName().equalsIgnoreCase(loginName)) {
                        loggedInPerson = e;
                        return true;
                    }
                }
                System.out.println("Employee not found.");
                return false;

            case "2":
                List<Patient> patients = hibernate.jpql("FROM Patient", Patient.class);
                for (Patient p : patients) {
                    if (p.getName().equalsIgnoreCase(loginName)) {
                        loggedInPerson = p;
                        return true;
                    }
                }
                System.out.println("Patient not found.");
                return false;

            default:
                System.out.println("Invalid option.");
                return false;
        }
    }

    private void displayWelcome() {
        if (loggedInPerson instanceof Employee emp) {
            System.out.println("Welcome, " + emp.getName() + " (" + emp.getRole() + ")!");
        } else if (loggedInPerson instanceof Patient pat) {
            System.out.println("Welcome, " + pat.getName() + "!");
        } else {
            System.out.println("Welcome!");
        }
    }

    private String promptContext(Scanner scanner) {
        System.out.println("\n=== Please select a context ===");
        System.out.println("1. Billing and Payments");
        System.out.println("2. Scheduling");
        System.out.println("3. Check-in");
        System.out.println("4. Hiring");
        System.out.println("5. Appointment");
        System.out.println("6. Emergency");
        System.out.print("Select an option: ");

        String choice = scanner.nextLine();
        return switch (choice) {
            case "1" -> "Billing and Payments";
            case "2" -> "Scheduling";
            case "3" -> "Check-in";
            case "4" -> "Hiring";
            case "5" -> "Appointment";
            case "6" -> "Emergency";
            default -> null;
        };
    }

    private int promptTableSelection(Scanner scanner) {
        System.out.println("\n=== Available tables to request data from ===");
        System.out.println("1. Employees");
        System.out.println("2. Patients");
        System.out.println("3. Appointments");
        System.out.println("4. Payments");
        System.out.println("5. Exit");
        System.out.print("Select an option: ");

        String input = scanner.nextLine();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Long promptId(Scanner scanner) {
        System.out.print("Please enter the ID of the row you'd like to access: ");
        String input = scanner.nextLine();
        try {
            return Long.valueOf(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleDataAccess(String context, int tableChoice, Long id) {
        Class<?> entityClass;
        String entityName;

        switch (tableChoice) {
            case 1:
                entityClass = Employee.class;
                entityName = "Employee";
                break;
            case 2:
                entityClass = Patient.class;
                entityName = "Patient";
                break;
            case 3:
                entityClass = Appointment.class;
                entityName = "Appointment";
                break;
            case 4:
                entityClass = Payment.class;
                entityName = "Payment";
                break;
            default:
                System.out.println("Invalid table selection.");
                return;
        }

        try {
            // Fetch the entity from the database
            logger.info("Fetching from" + entityName + "ID " + Long.toString(id));
            Object result = hibernate.get(entityClass, id);

            if (result == null) {
                logger.error("No " + entityName + " found with ID: " + id);
                return;
            }

            // Check access via XACML PDP
            boolean allowed = pdp.checkAccess(loggedInPerson, result, context);

            if (allowed) {
                System.out.println("Access Granted!");
            } else {
                System.out.println("Access Denied: You do not have permission to view " 
                                + entityName + " data.");
            }

        } catch (Exception e) {
            logger.error("Error accessing data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int promptMainAction(Scanner scanner) {
        System.out.println("\n=== What would you like to do? ===");
        System.out.println("1. Access specific data");
        System.out.println("2. Export my data (Right to Access)");
        System.out.println("3. Request erasure (Right to Erasure)");
        System.out.println("4. Exit");
        System.out.print("Select an option: ");

        String input = scanner.nextLine();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void exportMyData() {
        Map<String, Object> export = new LinkedHashMap<>();

        if (loggedInPerson instanceof Patient pat) {
            export.put("self", recordOrRedacted(pat, pat, patientToMap(pat)));

            List<Object> appointmentsOut = new ArrayList<>();
            for (Appointment app : pat.getAppointments()) {
                appointmentsOut.add(recordOrRedacted(pat, app, appointmentToMap(app)));
            }
            export.put("appointments", appointmentsOut);

            List<Payment> payments = hibernate.sql(
                    "SELECT * FROM payments WHERE patient_id = " + pat.getId(), Payment.class);
            List<Object> paymentsOut = new ArrayList<>();
            for (Payment pay : payments) {
                paymentsOut.add(recordOrRedacted(pat, pay, paymentToMap(pay)));
            }
            export.put("payments", paymentsOut);

            List<Object> consentOut = new ArrayList<>();
            for (ConsentRecord c : pat.getConsentRecords()) {
                consentOut.add(recordOrRedacted(pat, c, consentToMap(c)));
            }
            export.put("consent_records", consentOut);

            List<Object> gdprOut = new ArrayList<>();
            for (GdprRequest g : pat.getGdprRequests()) {
                gdprOut.add(recordOrRedacted(pat, g, gdprToMap(g)));
            }
            export.put("gdpr_requests", gdprOut);

        } else if (loggedInPerson instanceof Employee emp) {
            export.put("self", recordOrRedacted(emp, emp, employeeToMap(emp)));

            List<Object> appointmentsOut = new ArrayList<>();
            for (Appointment app : emp.getAppointments()) {
                appointmentsOut.add(recordOrRedacted(emp, app, appointmentToMap(app)));
            }
            export.put("appointments", appointmentsOut);
            export.put("payments", new ArrayList<>()); // Payment nunca se liga a Employee

            List<Object> consentOut = new ArrayList<>();
            for (ConsentRecord c : emp.getConsentRecords()) {
                consentOut.add(recordOrRedacted(emp, c, consentToMap(c)));
            }
            export.put("consent_records", consentOut);

            List<Object> gdprOut = new ArrayList<>();
            for (GdprRequest g : emp.getGdprRequests()) {
                gdprOut.add(recordOrRedacted(emp, g, gdprToMap(g)));
            }
            export.put("gdpr_requests", gdprOut);
        }

        try {
     //       ObjectMapper mapper = new ObjectMapper();
    //        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
   //         System.out.println(json);
        } catch (Exception e) {
            logger.error("Failed to export data: " + e.getMessage());
        }
    }

    private Object recordOrRedacted(Object requester, Object record, Map<String, Object> fullRecord) {
        try {
            boolean allowed = pdp.checkAccessGDPR(requester, record, "", "export");
            if (allowed) {
                return fullRecord;
            }
        } catch (Exception e) {
            logger.error("Error checking access during export: " + e.getMessage());
        }
        Map<String, Object> redacted = new LinkedHashMap<>();
        redacted.put("id", fullRecord.get("id"));
        redacted.put("redacted", true);
        return redacted;
    }

    private Map<String, Object> patientToMap(Patient pat) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pat.getId());
        m.put("name", pat.getName());
        m.put("birthdate", pat.getBirthdate());
        m.put("insurance", pat.getInsurance());
        m.put("street", pat.getStreet());
        m.put("postalCode", pat.getPostalCode());
        return m;
    }

    private Map<String, Object> employeeToMap(Employee emp) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", emp.getId());
        m.put("name", emp.getName());
        m.put("role", emp.getRole());
        m.put("department", emp.getDepartment());
        return m;
    }

    private Map<String, Object> appointmentToMap(Appointment app) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", app.getId());
        m.put("dateTime", app.getDateTime());
        return m;
    }

    private Map<String, Object> paymentToMap(Payment pay) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pay.getId());
        m.put("dateTime", pay.getDateTime());
        m.put("value", pay.getValue());
        m.put("cardNumber", pay.getCardNumber());
        m.put("anonymized", pay.getAnonymized());
        return m;
    }

    private Map<String, Object> consentToMap(ConsentRecord c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("purpose", c.getPurpose());
        m.put("granted", c.getGranted());
        m.put("grantedAt", c.getGrantedAt());
        m.put("withdrawnAt", c.getWithdrawnAt());
        return m;
    }

    private Map<String, Object> gdprToMap(GdprRequest g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("requestType", g.getRequestType());
        m.put("requestedAt", g.getRequestedAt());
        m.put("status", g.getStatus());
        return m;
    }

    private void requestErasure() {
        if (!(loggedInPerson instanceof Patient pat)) {
            System.out.println("Only patients can request erasure.");
            return;
        }

     //   pat.setErasureRequested(true);

        GdprRequest request = new GdprRequest("PATIENT", "ERASURE", LocalDateTime.now());
        request.setPatientSubject(pat);
        pat.getGdprRequests().add(request);

        pat.setName("ANON_" + UUID.randomUUID());
        pat.setStreet("ANON_" + UUID.randomUUID());

        List<Payment> payments = hibernate.sql(
                "SELECT * FROM payments WHERE patient_id = " + pat.getId(), Payment.class);
        for (Payment pay : payments) {
            pay.setCardNumber("ANON_" + UUID.randomUUID());
            pay.setAnonymized(true);
            hibernate.update(pay);
        }

    //    pat.setErasureCompletedAt(LocalDateTime.now());
        hibernate.update(pat);

        System.out.println("Erasure request completed. Your data has been anonymized.");
    }


}
