package com.btbs.dao;

import com.btbs.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Seat availability and layout.
 */
public class SeatDAO {

    public record SeatStatusDTO(
            int seatId,
            String seatNumber,
            int busId,
            boolean isBooked
    ) {}

    /**
     * Gets all seats for the bus assigned to a schedule, with live booked status.
     */
    public List<SeatStatusDTO> getSeatsForSchedule(int scheduleId) {
        List<SeatStatusDTO> seats = new ArrayList<>();
        String sql = "SELECT st.SeatID, st.SeatNumber, st.BusID, " +
                     "CASE WHEN bs.BookingSeatID IS NOT NULL THEN 1 ELSE 0 END AS isBooked " +
                     "FROM ScheduleID s " +
                     "JOIN Bus b ON s.BusID = b.BusID " +
                     "JOIN Seat st ON b.BusID = st.BusID " +
                     "LEFT JOIN ( " +
                     "    SELECT bs.SeatID, b.ScheduleID, bs.BookingSeatID " +
                     "    FROM BookingSeat bs " +
                     "    JOIN Booking b ON bs.BookingID = b.BookingID " +
                     "    WHERE b.ScheduleID = ? " +
                     ") bs ON st.SeatID = bs.SeatID " +
                     "WHERE s.ScheduleID = ? " +
                     "ORDER BY st.SeatNumber";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ps.setInt(2, scheduleId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seats.add(new SeatStatusDTO(
                            rs.getInt("SeatID"),
                            rs.getString("SeatNumber"),
                            rs.getInt("BusID"),
                            rs.getInt("isBooked") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching seats for schedule: " + e.getMessage());
        }
        return seats;
    }
}
