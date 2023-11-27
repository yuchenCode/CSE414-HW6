CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255),
    PRIMARY KEY (Time, Username),
    FOREIGN KEY (Username) REFERENCES Caregivers(Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Reservations (
    AppointmentID int IDENTITY,
    PatientUsername varchar(255) REFERENCES Patients(Username),
    CaregiverUsername varchar(255) REFERENCES Caregivers(Username),
    VaccineName varchar(255) REFERENCES Vaccines(Name),
    AppointmentTime date,
    PRIMARY KEY (AppointmentID),
    FOREIGN KEY (PatientUsername) REFERENCES Patients(Username),
    FOREIGN KEY (CaregiverUsername) REFERENCES Caregivers(Username),
    FOREIGN KEY (VaccineName) REFERENCES Vaccines(Name)
);
