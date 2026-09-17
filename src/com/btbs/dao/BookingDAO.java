package com.btbs.dao;

import com.btbs.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Data Access Object for Booking transactions and seat reservations.
 */
public class BookingDAO {

    private final WalletDAO walletDAO = new WalletDAO();

    public record BookingReceiptDTO(
            int bookingId,
            String customerName,
            String customerEmail,
            String route,
            java.util.Date travelDate,
            java.sql.Time departureTime,
            String busNumber,
            String busType,
            String bookedSeats,
            double totalAmount
    ) {}

    /**
     * Executes an atomic booking transaction with wallet payment.
     * Implements the exact workflow from the UML Sequence Diagram:
     * checkSeats -> verifyPIN -> deductBalance -> saveBooking -> saveBookingSeats.
     *
     * @return generated BookingID if successful, or -1 on failure
     */
    public int createBooking(int scheduleId, int accountId, int pin, List<Integer> seatIds, double totalAmount) throws Exception {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("No seats selected.");
        }

        // 1. Verify PIN
        if (!walletDAO.verifyPin(accountId, pin)) {
            throw new IllegalStateException("Invalid Wallet PIN. Please try again.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Begin Database Transaction

            try {
                // 2. Check seat availability inside transaction
                String checkSql = "SELECT bs.BookingSeatID FROM BookingSeat bs " +
                                  "JOIN Booking b ON bs.BookingID = b.BookingID " +
                                  "WHERE b.ScheduleID = ? AND bs.SeatID = ?";
                for (int seatId : seatIds) {
                    try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                        checkPs.setInt(1, scheduleId);
                        checkPs.setInt(2, seatId);
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next()) {
                                throw new IllegalStateException("Seat ID " + seatId + " is already booked.");
                            }
                        }
                    }
                }

                // 3. Deduct balance & record wallet transaction
                boolean paymentSuccess = walletDAO.processPayment(conn, accountId, totalAmount);
                if (!paymentSuccess) {
                    throw new IllegalStateException("Insufficient wallet balance for this booking.");
                }

                // 4. Insert master Booking record
                int newBookingId = -1;
                String bookingSql = "INSERT INTO Booking (BookingDate, TotalAmount, ScheduleID, AccountID) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(bookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setDate(1, new Date(System.currentTimeMillis()));
                    ps.setDouble(2, totalAmount);
                    ps.setInt(3, scheduleId);
                    ps.setInt(4, accountId);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            newBookingId = rs.getInt(1);
                        }
                    }
                }

                if (newBookingId <= 0) {
                    throw new SQLException("Failed to generate Booking record.");
                }

                // 5. Insert BookingSeat junction records
                String seatSql = "INSERT INTO BookingSeat (BookingID, SeatID) VALUES (?, ?)";
                try (PreparedStatement seatPs = conn.prepareStatement(seatSql)) {
                    for (int seatId : seatIds) {
                        seatPs.setInt(1, newBookingId);
                        seatPs.setInt(2, seatId);
                        seatPs.addBatch();
                    }
                    seatPs.executeBatch();
                }

                // 6. Commit transaction
                conn.commit();
                return newBookingId;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves full booking receipt by BookingID.
     */
    public BookingReceiptDTO getReceipt(int bookingId) {
        String sql = "SELECT bk.BookingID, acc.FullName, acc.Email, " +
                     "CONCAT(r.DepartureLocation, ' -> ', r.Destination) AS Route, " +
                     "s.TravelDate, s.DepartureTime, b.BusNumber, b.BusType, bk.TotalAmount, " +
                     "GROUP_CONCAT(st.SeatNumber ORDER BY st.SeatNumber SEPARATOR ', ') AS BookedSeats " +
                     "FROM Booking bk " +
                     "JOIN Account acc ON bk.AccountID = acc.AccountID " +
                     "JOIN ScheduleID s ON bk.ScheduleID = s.ScheduleID " +
                     "JOIN Route r ON s.RouteID = r.RouteID " +
                     "JOIN Bus b ON s.BusID = b.BusID " +
                     "JOIN BookingSeat bs ON bk.BookingID = bs.BookingID " +
                     "JOIN Seat st ON bs.SeatID = st.SeatID " +
                     "WHERE bk.BookingID = ? " +
                     "GROUP BY bk.BookingID, acc.FullName, acc.Email, r.DepartureLocation, r.Destination, " +
                     "s.TravelDate, s.DepartureTime, b.BusNumber, b.BusType, bk.TotalAmount";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BookingReceiptDTO(
                            rs.getInt("BookingID"),
                            rs.getString("FullName"),
                            rs.getString("Email"),
                            rs.getString("Route"),
                            rs.getDate("TravelDate"),
                            rs.getTime("DepartureTime"),
                            rs.getString("BusNumber"),
                            rs.getString("BusType"),
                            rs.getString("BookedSeats"),
                            rs.getDouble("TotalAmount")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching booking receipt: " + e.getMessage());
        }
        return null;
    }
}
