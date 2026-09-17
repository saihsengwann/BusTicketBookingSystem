package com.btbs.dao;

import com.btbs.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Schedule and Route operations.
 */
public class ScheduleDAO {

    public record ScheduleDTO(
            int scheduleId,
            String departureLocation,
            String destination,
            Date travelDate,
            Time departureTime,
            Time arrivalTime,
            double price,
            int busId,
            String busNumber,
            String busType,
            int totalSeats
    ) {
        public String getRouteSummary() {
            return departureLocation + " -> " + destination;
        }
    }

    /**
     * Retrieves all active bus schedules with route and bus details.
     */
    public List<ScheduleDTO> getAllSchedules() {
        List<ScheduleDTO> list = new ArrayList<>();
        String sql = "SELECT s.ScheduleID, r.DepartureLocation, r.Destination, s.TravelDate, " +
                     "s.DepartureTime, s.ArrivalTime, s.Price, b.BusID, b.BusNumber, b.BusType, b.TotalSeats " +
                     "FROM ScheduleID s " +
                     "JOIN Route r ON s.RouteID = r.RouteID " +
                     "JOIN Bus b ON s.BusID = b.BusID " +
                     "ORDER BY s.TravelDate, s.DepartureTime";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ScheduleDTO(
                        rs.getInt("ScheduleID"),
                        rs.getString("DepartureLocation"),
                        rs.getString("Destination"),
                        rs.getDate("TravelDate"),
                        rs.getTime("DepartureTime"),
                        rs.getTime("ArrivalTime"),
                        rs.getDouble("Price"),
                        rs.getInt("BusID"),
                        rs.getString("BusNumber"),
                        rs.getString("BusType"),
                        rs.getInt("TotalSeats")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching schedules: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves a single schedule by ID.
     */
    public ScheduleDTO getScheduleById(int scheduleId) {
        String sql = "SELECT s.ScheduleID, r.DepartureLocation, r.Destination, s.TravelDate, " +
                     "s.DepartureTime, s.ArrivalTime, s.Price, b.BusID, b.BusNumber, b.BusType, b.TotalSeats " +
                     "FROM ScheduleID s " +
                     "JOIN Route r ON s.RouteID = r.RouteID " +
                     "JOIN Bus b ON s.BusID = b.BusID " +
                     "WHERE s.ScheduleID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ScheduleDTO(
                            rs.getInt("ScheduleID"),
                            rs.getString("DepartureLocation"),
                            rs.getString("Destination"),
                            rs.getDate("TravelDate"),
                            rs.getTime("DepartureTime"),
                            rs.getTime("ArrivalTime"),
                            rs.getDouble("Price"),
                            rs.getInt("BusID"),
                            rs.getString("BusNumber"),
                            rs.getString("BusType"),
                            rs.getInt("TotalSeats")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching schedule by ID: " + e.getMessage());
        }
        return null;
    }
}
