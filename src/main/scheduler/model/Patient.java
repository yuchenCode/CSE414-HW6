package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;

public class Patient {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Patient(PatientBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Patient(PatientGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patients VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void reserve(Date d, String vac) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String schedule = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
        String vaccine = "SELECT Doses FROM Vaccines WHERE Name = ?";
        String updateAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        String updateVaccine = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
        String insertAppointment = "INSERT INTO Reservations VALUES (?, ?, ?, ?)";

        try {
            // Check for available schedule
            PreparedStatement scheduleStatement = con.prepareStatement(schedule);
            scheduleStatement.setDate(1, d);
            ResultSet scheduleResultSet = scheduleStatement.executeQuery();

            // Check for vaccine availability
            PreparedStatement vaccineStatement = con.prepareStatement(vaccine);
            vaccineStatement.setString(1, vac);
            ResultSet vaccineResultSet = vaccineStatement.executeQuery();

            if (!scheduleResultSet.next()) {
                System.out.println("No Caregiver is Available!");
            } else if (!vaccineResultSet.next() || vaccineResultSet.getInt(1) < 1) {
                System.out.println("Not enough available doses!");
            } else {
                String caregiverUsername = scheduleResultSet.getString(1);

                // Update caregiver's availability
                PreparedStatement updateAvailabilityStatement = con.prepareStatement(updateAvailability);
                updateAvailabilityStatement.setDate(1, d);
                updateAvailabilityStatement.setString(2, caregiverUsername);
                updateAvailabilityStatement.executeUpdate();

                // Update vaccine doses
                PreparedStatement updateVaccineStatement = con.prepareStatement(updateVaccine);
                updateVaccineStatement.setString(1, vac);
                updateVaccineStatement.executeUpdate();

                // Create a new appointment
                PreparedStatement insertAppointmentStatement = con.prepareStatement(insertAppointment, Statement.RETURN_GENERATED_KEYS);
                insertAppointmentStatement.setString(1, this.getUsername());
                insertAppointmentStatement.setString(2, caregiverUsername);
                insertAppointmentStatement.setString(3, vac);
                insertAppointmentStatement.setDate(4, d);
                insertAppointmentStatement.executeUpdate();

                // Retrieve and output the appointment ID
                ResultSet rs = insertAppointmentStatement.getGeneratedKeys();
                if (rs.next()) {
                    int appointmentId = rs.getInt(1);
                    System.out.println("Appointment ID: " + appointmentId + ", Caregiver username: " + caregiverUsername);
                }
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class PatientBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public PatientBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Patient build() {
            return new Patient(this);
        }
    }

    public static class PatientGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public PatientGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Patient get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT Salt, Hash FROM Patients WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Patient(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}