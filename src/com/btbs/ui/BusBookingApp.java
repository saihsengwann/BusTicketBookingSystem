package com.btbs.ui;

import com.btbs.dao.BookingDAO;
import com.btbs.dao.ScheduleDAO;
import com.btbs.dao.SeatDAO;
import com.btbs.dao.WalletDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Desktop GUI Application for Bus Ticket Booking System (BTBS).
 * Built with Java Swing, implementing interactive visual seat selection and wallet payments.
 */
public class BusBookingApp extends JFrame {

    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    // Current active passenger context (Aung Aung, ID=1)
    private final int currentAccountId = 1;
    private final String currentUserName = "Aung Aung";

    private JLabel headerUserInfoLabel;
    private JTable scheduleTable;
    private DefaultTableModel scheduleTableModel;
    private JPanel seatGridPanel;
    private JLabel seatSummaryLabel;
    private JButton bookButton;

    private List<ScheduleDAO.ScheduleDTO> scheduleList = new ArrayList<>();
    private ScheduleDAO.ScheduleDTO selectedSchedule = null;
    private final Set<Integer> selectedSeatIds = new HashSet<>();
    private final List<String> selectedSeatNumbers = new ArrayList<>();

    public BusBookingApp() {
        setTitle("Bus Ticket Booking System (BTBS) - 2025/2026");
        setSize(1100, 680);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        loadSchedules();
        refreshUserHeader();
    }

    private void initUI() {
        getContentPane().setLayout(new BorderLayout(0, 0));

        // 1. Top Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 44, 97));
        headerPanel.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel titleLabel = new JLabel("🚌 BUS TICKET BOOKING SYSTEM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        headerUserInfoLabel = new JLabel("Passenger: Aung Aung | Balance: 100,000 MMK");
        headerUserInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        headerUserInfoLabel.setForeground(new Color(220, 230, 245));
        headerPanel.add(headerUserInfoLabel, BorderLayout.EAST);

        getContentPane().add(headerPanel, BorderLayout.NORTH);

        // 2. Main Content Split View
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);

        // Left: Schedules Panel
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBorder(new EmptyBorder(15, 15, 15, 10));

        JLabel schedTitle = new JLabel("1. Select a Travel Schedule");
        schedTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        leftPanel.add(schedTitle, BorderLayout.NORTH);

        String[] columns = {"ID", "Route", "Date", "Departure", "Arrival", "Price (MMK)", "Bus", "Type"};
        scheduleTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        scheduleTable = new JTable(scheduleTableModel);
        scheduleTable.setRowHeight(28);
        scheduleTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        scheduleTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scheduleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = scheduleTable.getSelectedRow();
                if (row >= 0 && row < scheduleList.size()) {
                    selectedSchedule = scheduleList.get(row);
                    loadSeats(selectedSchedule.scheduleId());
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(scheduleTable);
        leftPanel.add(tableScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // Right: Seat Layout & Booking Controls
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBorder(new EmptyBorder(15, 10, 15, 15));

        JPanel rightHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel seatTitle = new JLabel("2. Interactive Seat Selection");
        seatTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        rightHeader.add(seatTitle);

        // Legend bar
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legendPanel.add(createLegendBadge(new Color(46, 204, 113), "Available"));
        legendPanel.add(createLegendBadge(new Color(231, 76, 60), "Booked"));
        legendPanel.add(createLegendBadge(new Color(52, 152, 219), "Selected"));
        rightHeader.add(legendPanel);

        rightPanel.add(rightHeader, BorderLayout.NORTH);

        // Seat Grid Area
        seatGridPanel = new JPanel();
        seatGridPanel.setLayout(new GridLayout(0, 3, 10, 10));
        seatGridPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        seatGridPanel.setBackground(Color.WHITE);

        JScrollPane seatScroll = new JScrollPane(seatGridPanel);
        seatScroll.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 220)));
        rightPanel.add(seatScroll, BorderLayout.CENTER);

        // Right Bottom Action Bar
        JPanel rightBottom = new JPanel(new BorderLayout(10, 10));
        rightBottom.setBorder(new EmptyBorder(10, 0, 0, 0));

        seatSummaryLabel = new JLabel("Please select a schedule on the left.");
        seatSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rightBottom.add(seatSummaryLabel, BorderLayout.NORTH);

        bookButton = new JButton("Book & Pay via Wallet 💳");
        bookButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bookButton.setBackground(new Color(24, 44, 97));
        bookButton.setForeground(Color.WHITE);
        bookButton.setFocusPainted(false);
        bookButton.setPreferredSize(new Dimension(0, 42));
        bookButton.setEnabled(false);
        bookButton.addActionListener(e -> initiateBookingFlow());
        rightBottom.add(bookButton, BorderLayout.SOUTH);

        rightPanel.add(rightBottom, BorderLayout.SOUTH);
        splitPane.setRightComponent(rightPanel);

        getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLegendBadge(Color color, String text) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badge.setOpaque(false);
        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(14, 14));
        dot.setBackground(color);
        dot.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        badge.add(dot);
        badge.add(label);
        return badge;
    }

    private void refreshUserHeader() {
        double balance = walletDAO.getBalance(currentAccountId);
        headerUserInfoLabel.setText(String.format("👤 %s | 💳 Wallet Balance: %,.2f MMK", currentUserName, balance));
    }

    private void loadSchedules() {
        scheduleList = scheduleDAO.getAllSchedules();
        scheduleTableModel.setRowCount(0);
        for (ScheduleDAO.ScheduleDTO s : scheduleList) {
            scheduleTableModel.addRow(new Object[]{
                    s.scheduleId(),
                    s.getRouteSummary(),
                    s.travelDate().toString(),
                    s.departureTime().toString(),
                    s.arrivalTime().toString(),
                    String.format("%,.2f", s.price()),
                    s.busNumber(),
                    s.busType()
            });
        }
        if (!scheduleList.isEmpty()) {
            scheduleTable.setRowSelectionInterval(0, 0);
            selectedSchedule = scheduleList.get(0);
            loadSeats(selectedSchedule.scheduleId());
        }
    }

    private void loadSeats(int scheduleId) {
        selectedSeatIds.clear();
        selectedSeatNumbers.clear();
        seatGridPanel.removeAll();

        List<SeatDAO.SeatStatusDTO> seats = seatDAO.getSeatsForSchedule(scheduleId);
        for (SeatDAO.SeatStatusDTO seat : seats) {
            JButton seatBtn = new JButton(seat.seatNumber());
            seatBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            seatBtn.setFocusPainted(false);
            seatBtn.setPreferredSize(new Dimension(70, 45));

            if (seat.isBooked()) {
                seatBtn.setBackground(new Color(231, 76, 60)); // Red
                seatBtn.setForeground(Color.WHITE);
                seatBtn.setEnabled(false);
                seatBtn.setToolTipText("Seat " + seat.seatNumber() + " is already booked");
            } else {
                seatBtn.setBackground(new Color(46, 204, 113)); // Green
                seatBtn.setForeground(Color.WHITE);
                seatBtn.addActionListener(e -> toggleSeatSelection(seatBtn, seat.seatId(), seat.seatNumber()));
            }
            seatGridPanel.add(seatBtn);
        }

        updateSelectionSummary();
        seatGridPanel.revalidate();
        seatGridPanel.repaint();
    }

    private void toggleSeatSelection(JButton btn, int seatId, String seatNumber) {
        if (selectedSeatIds.contains(seatId)) {
            selectedSeatIds.remove(seatId);
            selectedSeatNumbers.remove(seatNumber);
            btn.setBackground(new Color(46, 204, 113)); // back to Green
        } else {
            selectedSeatIds.add(seatId);
            selectedSeatNumbers.add(seatNumber);
            btn.setBackground(new Color(52, 152, 219)); // Blue
        }
        updateSelectionSummary();
    }

    private void updateSelectionSummary() {
        if (selectedSchedule == null) return;

        int count = selectedSeatIds.size();
        double total = count * selectedSchedule.price();

        if (count == 0) {
            seatSummaryLabel.setText("Select seats above to continue.");
            bookButton.setEnabled(false);
        } else {
            seatSummaryLabel.setText(String.format("Selected (%d): %s | Total: %,.2f MMK",
                    count, String.join(", ", selectedSeatNumbers), total));
            bookButton.setEnabled(true);
        }
    }

    private void initiateBookingFlow() {
        if (selectedSchedule == null || selectedSeatIds.isEmpty()) return;

        double totalAmount = selectedSeatIds.size() * selectedSchedule.price();
        double currentBalance = walletDAO.getBalance(currentAccountId);

        // Step 1: Check balance
        if (currentBalance < totalAmount) {
            JOptionPane.showMessageDialog(this,
                    String.format("Insufficient Wallet Balance!\n\nRequired: %,.2f MMK\nYour Balance: %,.2f MMK\nPlease deposit funds to continue.",
                            totalAmount, currentBalance),
                    "Insufficient Balance", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Step 2: Prompt for Wallet PIN (as modeled in Sequence Diagram)
        JPasswordField pinField = new JPasswordField(10);
        JPanel pinPanel = new JPanel(new GridLayout(0, 1, 4, 6));
        pinPanel.add(new JLabel(String.format("Booking %d Seat(s): %s", selectedSeatIds.size(), String.join(", ", selectedSeatNumbers))));
        pinPanel.add(new JLabel(String.format("Route: %s (%s)", selectedSchedule.getRouteSummary(), selectedSchedule.travelDate())));
        pinPanel.add(new JLabel(String.format("Total Charge: %,.2f MMK", totalAmount)));
        pinPanel.add(new JSeparator());
        pinPanel.add(new JLabel("Enter 4-Digit Wallet Security PIN:"));
        pinPanel.add(pinField);

        int option = JOptionPane.showConfirmDialog(this, pinPanel, "Authorize Wallet Payment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String pinStr = new String(pinField.getPassword()).trim();
        int pin;
        try {
            pin = Integer.parseInt(pinStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "PIN must be numeric.", "Invalid PIN", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Step 3: Process transaction via BookingDAO
        try {
            int newBookingId = bookingDAO.createBooking(
                    selectedSchedule.scheduleId(),
                    currentAccountId,
                    pin,
                    new ArrayList<>(selectedSeatIds),
                    totalAmount
            );

            // Step 4: Show Ticket Confirmation Voucher
            BookingDAO.BookingReceiptDTO receipt = bookingDAO.getReceipt(newBookingId);
            showReceiptDialog(receipt);

            // Refresh UI
            refreshUserHeader();
            loadSeats(selectedSchedule.scheduleId());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showReceiptDialog(BookingDAO.BookingReceiptDTO r) {
        if (r == null) return;

        JTextArea receiptText = new JTextArea();
        receiptText.setFont(new Font("Consolas", Font.PLAIN, 13));
        receiptText.setEditable(false);
        receiptText.setText(String.format("""
            ==================================================
                      OFFICIAL BUS TICKET RECEIPT             
            ==================================================
             Booking Reference : #%06d
             Passenger Name    : %s
             Contact Email     : %s
            --------------------------------------------------
             Route             : %s
             Travel Date       : %s
             Departure Time    : %s
             Bus Vehicle       : %s (%s)
            --------------------------------------------------
             Reserved Seats    : %s
             Total Amount Paid : %,.2f MMK
             Payment Method    : In-App Digital Wallet
            ==================================================
                 Thank you for booking with BTBS Express!      
            ==================================================
            """,
                r.bookingId(), r.customerName(), r.customerEmail(),
                r.route(), r.travelDate(), r.departureTime(),
                r.busNumber(), r.busType(),
                r.bookedSeats(), r.totalAmount()
        ));

        JScrollPane scroll = new JScrollPane(receiptText);
        scroll.setPreferredSize(new Dimension(460, 320));

        JOptionPane.showMessageDialog(this, scroll, "Booking Confirmed! 🎉", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            BusBookingApp app = new BusBookingApp();
            app.setVisible(true);
        });
    }
}
