-- ==========================================================
-- Bus Ticket Booking System (BTBS) - Database DDL & Seed Data
-- Course Project: 2025 - 2026
-- Compatible with MySQL 8.0+
-- ==========================================================

SET SESSION FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------
-- Drop Tables (in reverse dependency order)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS BookingSeat;
DROP TABLE IF EXISTS Booking;
DROP TABLE IF EXISTS Transaction;
DROP TABLE IF EXISTS Wallet;
DROP TABLE IF EXISTS Passenger;
DROP TABLE IF EXISTS ScheduleID;
DROP TABLE IF EXISTS Seat;
DROP TABLE IF EXISTS Bus;
DROP TABLE IF EXISTS Route;
DROP TABLE IF EXISTS Account;
DROP TABLE IF EXISTS Admin;

-- ----------------------------------------------------------
-- 1. Table: Admin
-- ----------------------------------------------------------
CREATE TABLE Admin
(
	AdminID INT NOT NULL AUTO_INCREMENT,
	Name VARCHAR(50) NOT NULL,
	Username VARCHAR(20) NOT NULL UNIQUE,
	Password VARCHAR(50) NOT NULL,
	PRIMARY KEY (AdminID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 2. Table: Account
-- ----------------------------------------------------------
CREATE TABLE Account
(
	AccountID INT NOT NULL AUTO_INCREMENT,
	FullName VARCHAR(50) NOT NULL,
	Email VARCHAR(50) NOT NULL UNIQUE,
	Password VARCHAR(50) NOT NULL,
	Phone VARCHAR(20),
	Role VARCHAR(20) DEFAULT 'Passenger',
	Status VARCHAR(20) DEFAULT 'Active',
	CreateAt DATETIME DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (AccountID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 3. Table: Wallet (1:1 with Account)
-- ----------------------------------------------------------
CREATE TABLE Wallet
(
	WalletID INT NOT NULL AUTO_INCREMENT,
	Balance DECIMAL(10, 2) DEFAULT 0.00,
	LastUpdated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PIN INT NOT NULL,
	AccountID INT NOT NULL UNIQUE,
	PRIMARY KEY (WalletID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 4. Table: Transaction
-- ----------------------------------------------------------
CREATE TABLE Transaction
(
	TransactionID INT NOT NULL AUTO_INCREMENT,
	WalletID INT NOT NULL,
	TransactionType VARCHAR(20) NOT NULL, -- 'Deposit', 'Payment', 'Refund'
	Amount DECIMAL(10, 2) NOT NULL,
	TransactionNo INT,
	CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (TransactionID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 5. Table: Route
-- ----------------------------------------------------------
CREATE TABLE Route
(
	RouteID INT NOT NULL AUTO_INCREMENT,
	DepartureLocation VARCHAR(50) NOT NULL,
	Destination VARCHAR(50) NOT NULL,
	Distance VARCHAR(20),
	PRIMARY KEY (RouteID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 6. Table: Bus
-- ----------------------------------------------------------
CREATE TABLE Bus
(
	BusID INT NOT NULL AUTO_INCREMENT,
	BusNumber VARCHAR(20) NOT NULL UNIQUE,
	BusType VARCHAR(20) NOT NULL, -- 'VIP (2+1)', 'Standard (2+2)', 'Scania VIP'
	TotalSeats INT NOT NULL,
	Status VARCHAR(20) NOT NULL DEFAULT 'Available',
	BusPhoto VARCHAR(100),
	PRIMARY KEY (BusID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 7. Table: Seat
-- ----------------------------------------------------------
CREATE TABLE Seat
(
	SeatID INT NOT NULL AUTO_INCREMENT,
	SeatNumber VARCHAR(10) NOT NULL,
	BusID INT NOT NULL,
	PRIMARY KEY (SeatID),
	UNIQUE KEY unique_bus_seat (BusID, SeatNumber)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 8. Table: ScheduleID
-- ----------------------------------------------------------
CREATE TABLE ScheduleID
(
	ScheduleID INT NOT NULL AUTO_INCREMENT,
	TravelDate DATE NOT NULL,
	DepartureTime TIME NOT NULL,
	ArrivalTime TIME NOT NULL,
	Price DECIMAL(10, 2) NOT NULL,
	RouteID INT NOT NULL,
	BusID INT NOT NULL,
	PRIMARY KEY (ScheduleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 9. Table: Booking
-- ----------------------------------------------------------
CREATE TABLE Booking
(
	BookingID INT NOT NULL AUTO_INCREMENT,
	BookingDate DATE NOT NULL,
	TotalAmount DECIMAL(10, 2) NOT NULL,
	ScheduleID INT NOT NULL,
	AccountID INT NOT NULL,
	PRIMARY KEY (BookingID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 10. Table: BookingSeat
-- ----------------------------------------------------------
CREATE TABLE BookingSeat
(
	BookingSeatID INT NOT NULL AUTO_INCREMENT,
	BookingID INT NOT NULL,
	SeatID INT NOT NULL,
	PRIMARY KEY (BookingSeatID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 11. Table: Passenger
-- ----------------------------------------------------------
CREATE TABLE Passenger
(
	PassengerID INT NOT NULL AUTO_INCREMENT,
	BusID INT NOT NULL,
	NRC VARCHAR(50),
	Name VARCHAR(50) NOT NULL,
	Phone VARCHAR(20),
	Address VARCHAR(100),
	PRIMARY KEY (PassengerID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- Foreign Key Constraints
-- ----------------------------------------------------------
ALTER TABLE Wallet
	ADD CONSTRAINT fk_wallet_account
	FOREIGN KEY (AccountID) REFERENCES Account (AccountID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE Transaction
	ADD CONSTRAINT fk_transaction_wallet
	FOREIGN KEY (WalletID) REFERENCES Wallet (WalletID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE Seat
	ADD CONSTRAINT fk_seat_bus
	FOREIGN KEY (BusID) REFERENCES Bus (BusID)
	ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ScheduleID
	ADD CONSTRAINT fk_schedule_route
	FOREIGN KEY (RouteID) REFERENCES Route (RouteID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ScheduleID
	ADD CONSTRAINT fk_schedule_bus
	FOREIGN KEY (BusID) REFERENCES Bus (BusID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE Booking
	ADD CONSTRAINT fk_booking_schedule
	FOREIGN KEY (ScheduleID) REFERENCES ScheduleID (ScheduleID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE Booking
	ADD CONSTRAINT fk_booking_account
	FOREIGN KEY (AccountID) REFERENCES Account (AccountID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE BookingSeat
	ADD CONSTRAINT fk_bookingseat_booking
	FOREIGN KEY (BookingID) REFERENCES Booking (BookingID)
	ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE BookingSeat
	ADD CONSTRAINT fk_bookingseat_seat
	FOREIGN KEY (SeatID) REFERENCES Seat (SeatID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE Passenger
	ADD CONSTRAINT fk_passenger_bus
	FOREIGN KEY (BusID) REFERENCES Bus (BusID)
	ON UPDATE CASCADE ON DELETE RESTRICT;

SET SESSION FOREIGN_KEY_CHECKS = 1;


-- ==========================================================
-- Sample Seed Data
-- ==========================================================

-- Admin User
INSERT INTO Admin (AdminID, Name, Username, Password) VALUES
(1, 'System Administrator', 'admin', 'admin123');

-- Routes
INSERT INTO Route (RouteID, DepartureLocation, Destination, Distance) VALUES
(1, 'Yangon', 'Mandalay', '630 km'),
(2, 'Yangon', 'Naypyitaw', '380 km'),
(3, 'Yangon', 'Bagan', '620 km'),
(4, 'Mandalay', 'Yangon', '630 km');

-- Buses
INSERT INTO Bus (BusID, BusNumber, BusType, TotalSeats, Status, BusPhoto) VALUES
(1, 'YGN-7A1234', 'VIP (2+1)', 27, 'Available', 'bus_scania_vip.jpg'),
(2, 'YGN-5B5678', 'Standard (2+2)', 45, 'Available', 'bus_hyundai_std.jpg');

-- Seats for Bus 1 (VIP 2+1 Layout)
INSERT INTO Seat (SeatNumber, BusID) VALUES
('A1', 1), ('A2', 1), ('A3', 1),
('B1', 1), ('B2', 1), ('B3', 1),
('C1', 1), ('C2', 1), ('C3', 1),
('D1', 1), ('D2', 1), ('D3', 1),
('E1', 1), ('E2', 1), ('E3', 1);

-- Seats for Bus 2 (Standard 2+2 Layout)
INSERT INTO Seat (SeatNumber, BusID) VALUES
('1A', 2), ('1B', 2), ('1C', 2), ('1D', 2),
('2A', 2), ('2B', 2), ('2C', 2), ('2D', 2),
('3A', 2), ('3B', 2), ('3C', 2), ('3D', 2);

-- Schedules
INSERT INTO ScheduleID (ScheduleID, TravelDate, DepartureTime, ArrivalTime, Price, RouteID, BusID) VALUES
(1, '2026-10-01', '08:00:00', '17:00:00', 25000.00, 1, 1),
(2, '2026-10-01', '20:00:00', '05:00:00', 25000.00, 1, 1),
(3, '2026-10-02', '09:00:00', '15:00:00', 18000.00, 2, 2),
(4, '2026-10-03', '19:30:00', '06:30:00', 28000.00, 3, 1);

-- Accounts
INSERT INTO Account (AccountID, FullName, Email, Password, Phone, Role, Status, CreateAt) VALUES
(1, 'Aung Aung', 'aungaung@gmail.com', 'aung1234', '09971234567', 'Passenger', 'Active', '2026-09-01 10:00:00'),
(2, 'Su Su', 'susu@gmail.com', 'susu5678', '09798765432', 'Passenger', 'Active', '2026-09-02 11:30:00');

-- Wallets
INSERT INTO Wallet (WalletID, Balance, PIN, AccountID) VALUES
(1, 100000.00, 1234, 1),
(2, 50000.00, 5678, 2);

-- Transactions
INSERT INTO Transaction (TransactionID, WalletID, TransactionType, Amount, TransactionNo, CreatedAt) VALUES
(1, 1, 'Deposit', 150000.00, 100001, '2026-09-01 10:15:00'),
(2, 1, 'Payment', 50000.00, 100002, '2026-09-10 14:30:00'),
(3, 2, 'Deposit', 50000.00, 100003, '2026-09-02 12:00:00');

-- Bookings (Aung Aung booked 2 seats on Schedule 1)
INSERT INTO Booking (BookingID, BookingDate, TotalAmount, ScheduleID, AccountID) VALUES
(1, '2026-09-10', 50000.00, 1, 1);

-- Booking Seats (Seats A1 and A2 booked for Booking #1)
INSERT INTO BookingSeat (BookingSeatID, BookingID, SeatID) VALUES
(1, 1, 1),
(2, 1, 2);

-- Passengers
INSERT INTO Passenger (PassengerID, BusID, NRC, Name, Phone, Address) VALUES
(1, 1, '12/YAGANA(N)123456', 'Aung Aung', '09971234567', 'No. 12, Main Road, Kamayut, Yangon'),
(2, 1, '12/DAGAMA(N)654321', 'Kyaw Kyaw', '09450001122', 'No. 45, Bogyoke St, Dagon, Yangon');


-- ==========================================================
-- Sample Verification Queries
-- ==========================================================

-- 1. Search Active Schedules with Route and Bus Information
SELECT 
    s.ScheduleID,
    r.DepartureLocation,
    r.Destination,
    s.TravelDate,
    s.DepartureTime,
    s.ArrivalTime,
    s.Price,
    b.BusNumber,
    b.BusType
FROM ScheduleID s
JOIN Route r ON s.RouteID = r.RouteID
JOIN Bus b ON s.BusID = b.BusID
ORDER BY s.TravelDate, s.DepartureTime;

-- 2. View Booking Details with Passenger and Seat Info
SELECT 
    bk.BookingID,
    acc.FullName AS BookedBy,
    r.DepartureLocation,
    r.Destination,
    s.TravelDate,
    s.DepartureTime,
    st.SeatNumber,
    bk.TotalAmount
FROM Booking bk
JOIN Account acc ON bk.AccountID = acc.AccountID
JOIN ScheduleID s ON bk.ScheduleID = s.ScheduleID
JOIN Route r ON s.RouteID = r.RouteID
JOIN BookingSeat bs ON bk.BookingID = bs.BookingID
JOIN Seat st ON bs.SeatID = st.SeatID
WHERE bk.BookingID = 1;
