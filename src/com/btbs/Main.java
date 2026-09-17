package com.btbs;

import com.btbs.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Main demonstration console application for Bus Ticket Booking System (BTBS).
 * Verifies system integration, database connectivity, and runs sample booking queries.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("             BUS TICKET BOOKING SYSTEM (BTBS) - 2025/2026                      ");
        System.out.println("===============================================================================\n");

        System.out.print("[1/4] Checking MySQL Database Connection... ");
        if (!DBConnection.testConnection()) {
            System.out.println("FAILED!");
            System.out.println("\nPlease ensure:");
            System.out.println("  1. MySQL Server is running on localhost:3306.");
            System.out.println("  2. The 'btbs' database is created using 'BTBS(ERD).sql'.");
            System.out.println("  3. Credentials in com.btbs.util.DBConnection are correct.\n");
            return;
        }
        System.out.println("CONNECTED (OK)\n");

        displayRoutes();
        displaySchedules();
        displaySeatAvailability(1); // Check Schedule #1
        displaySampleBooking(1);     // Check Booking #1
    }

    private static void displayRoutes() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" AVAILABLE ROUTES");
        System.out.println("-------------------------------------------------------------------------------");
        String sql = "SELECT RouteID, DepartureLocation, Destination, Distance FROM Route ORDER BY RouteID";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.printf("%-8s | %-18s | %-18s | %-10s%n", "RouteID", "From", "To", "Distance");
            System.out.println("---------+--------------------+--------------------+-----------");
            while (rs.next()) {
                System.out.printf("%-8d | %-18s | %-18s | %-10s%n",
                        rs.getInt("RouteID"),
                        rs.getString("DepartureLocation"),
                        rs.getString("Destination"),
                        rs.getString("Distance"));
            }
            System.out.println();
        } catch (SQLException e) {
            System.err.println("Error querying routes: " + e.getMessage());
        }
    }

    private static void displaySchedules() {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" ACTIVE TRAVEL SCHEDULES");
        System.out.println("-------------------------------------------------------------------------------");
        String sql = "SELECT s.ScheduleID, r.DepartureLocation, r.Destination, s.TravelDate, " +
                     "s.DepartureTime, s.ArrivalTime, s.Price, b.BusNumber, b.BusType " +
                     "FROM ScheduleID s " +
                     "JOIN Route r ON s.RouteID = r.RouteID " +
                     "JOIN Bus b ON s.BusID = b.BusID " +
                     "ORDER BY s.TravelDate, s.DepartureTime";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.printf("%-4s | %-18s | %-10s | %-8s | %-8s | %-12s | %-12s | %-12s%n",
                    "ID", "Route", "Date", "Dep.", "Arr.", "Price (MMK)", "Bus No.", "Type");
            System.out.println("-----+--------------------+------------+----------+----------+--------------+--------------+-------------");
            while (rs.next()) {
                String routeStr = rs.getString("DepartureLocation") + " -> " + rs.getString("Destination");
                System.out.printf("%-4d | %-18s | %-10s | %-8s | %-8s | %,12.2f | %-12s | %-12s%n",
                        rs.getInt("ScheduleID"),
                        routeStr,
                        rs.getDate("TravelDate"),
                        rs.getTime("DepartureTime"),
                        rs.getTime("ArrivalTime"),
                        rs.getDouble("Price"),
                        rs.getString("BusNumber"),
                        rs.getString("BusType"));
            }
            System.out.println();
        } catch (SQLException e) {
            System.err.println("Error querying schedules: " + e.getMessage());
        }
    }

    private static void displaySeatAvailability(int scheduleId) {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" SEAT AVAILABILITY & LAYOUT (Schedule #" + scheduleId + ")");
        System.out.println("-------------------------------------------------------------------------------");
        String sql = "SELECT st.SeatNumber, " +
                     "CASE WHEN bs.BookingSeatID IS NOT NULL THEN 'BOOKED' ELSE 'AVAILABLE' END AS SeatStatus " +
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
                int count = 0;
                while (rs.next()) {
                    String seat = rs.getString("SeatNumber");
                    String status = rs.getString("SeatStatus");
                    String badge = "BOOKED".equals(status) ? "[X] " + seat : "[O] " + seat;
                    System.out.printf("%-10s", badge);
                    count++;
                    if (count % 5 == 0) {
                        System.out.println();
                    }
                }
                if (count % 5 != 0) {
                    System.out.println();
                }
                System.out.println("\nLegend: [O] Available | [X] Booked\n");
            }
        } catch (SQLException e) {
            System.err.println("Error checking seat layout: " + e.getMessage());
        }
    }

    private static void displaySampleBooking(int bookingId) {
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println(" SAMPLE BOOKING CONFIRMATION DETAILS (Booking #" + bookingId + ")");
        System.out.println("-------------------------------------------------------------------------------");
        String sql = "SELECT bk.BookingID, acc.FullName, acc.Email, r.DepartureLocation, r.Destination, " +
                     "s.TravelDate, s.DepartureTime, b.BusNumber, bk.TotalAmount, " +
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
                     "s.TravelDate, s.DepartureTime, b.BusNumber, bk.TotalAmount";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(" Booking ID    : #" + rs.getInt("BookingID"));
                    System.out.println(" Passenger     : " + rs.getString("FullName") + " (" + rs.getString("Email") + ")");
                    System.out.println(" Route         : " + rs.getString("DepartureLocation") + " -> " + rs.getString("Destination"));
                    System.out.println(" Departure     : " + rs.getDate("TravelDate") + " at " + rs.getTime("DepartureTime"));
                    System.out.println(" Bus Assigned  : " + rs.getString("BusNumber"));
                    System.out.println(" Seats Booked  : " + rs.getString("BookedSeats"));
                    System.out.printf (" Total Paid    : %,.2f MMK (via Wallet)%n", rs.getDouble("TotalAmount"));
                }
            }
            System.out.println("===============================================================================\n");
        } catch (SQLException e) {
            System.err.println("Error querying booking: " + e.getMessage());
        }
    }
}
