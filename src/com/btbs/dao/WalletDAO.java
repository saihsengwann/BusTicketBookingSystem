package com.btbs.dao;

import com.btbs.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for Wallet and Payment Transactions.
 */
public class WalletDAO {

    /**
     * Retrieves the current balance for an account.
     */
    public double getBalance(int accountId) {
        String sql = "SELECT Balance FROM Wallet WHERE AccountID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("Balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching wallet balance: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Validates the security PIN for an account's wallet.
     */
    public boolean verifyPin(int accountId, int pin) {
        String sql = "SELECT WalletID FROM Wallet WHERE AccountID = ? AND PIN = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, pin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error verifying PIN: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deducts balance and records a Payment transaction.
     */
    public boolean processPayment(Connection conn, int accountId, double amount) throws SQLException {
        // 1. Check and deduct balance
        String updateSql = "UPDATE Wallet SET Balance = Balance - ? WHERE AccountID = ? AND Balance >= ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, accountId);
            ps.setDouble(3, amount);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                return false; // Insufficient balance
            }
        }

        // 2. Fetch WalletID for transaction log
        int walletId = 0;
        String getWalletSql = "SELECT WalletID FROM Wallet WHERE AccountID = ?";
        try (PreparedStatement ps = conn.prepareStatement(getWalletSql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    walletId = rs.getInt("WalletID");
                }
            }
        }

        // 3. Record transaction
        int txNo = (int) (System.currentTimeMillis() % 1000000);
        String txSql = "INSERT INTO Transaction (WalletID, TransactionType, Amount, TransactionNo, CreatedAt) " +
                       "VALUES (?, 'Payment', ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(txSql)) {
            ps.setInt(1, walletId);
            ps.setDouble(2, amount);
            ps.setInt(3, txNo);
            ps.executeUpdate();
        }

        return true;
    }
}
