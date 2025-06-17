package Controller;

import Model.History;
import Model.Logs;
import Model.Product;
import Model.User;

import java.sql.*;
import java.util.ArrayList;

public class SQLite {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    public int DEBUG_MODE = 0;
    String driverURL = "jdbc:sqlite:" + "database.db";

    public void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection(driverURL)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("Database database.db created.");
            }
        } catch (SQLException ex) {
            System.err.println("Error creating database: " + ex.getMessage());
        }
    }

    public void createHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS history (" +
                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " username TEXT NOT NULL," +
                " name TEXT NOT NULL," +
                " stock INTEGER DEFAULT 0," +
                " timestamp TEXT NOT NULL" +
                ");";
        executeDDL(sql);
    }

    public void createLogsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS logs (" +
                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " event TEXT NOT NULL," +
                " username TEXT NOT NULL," +
                " desc TEXT NOT NULL," +
                " timestamp TEXT NOT NULL" +
                ");";
        executeDDL(sql);
    }

    public void createProductTable() {
        String sql = "CREATE TABLE IF NOT EXISTS product (" +
                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " name TEXT NOT NULL UNIQUE," +
                " stock INTEGER DEFAULT 0," +
                " price REAL DEFAULT 0.00" +
                ");";
        executeDDL(sql);
    }

    public void createUserTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " username TEXT NOT NULL UNIQUE," +
                " password TEXT NOT NULL," +
                " role INTEGER DEFAULT 2," +
                " locked INTEGER DEFAULT 0," +
                " failed_attempts INTEGER DEFAULT 0" +
                ");";
        executeDDL(sql);
    }

    public void dropHistoryTable() {
        executeDDL("DROP TABLE IF EXISTS history;");
    }

    public void dropLogsTable() {
        executeDDL("DROP TABLE IF EXISTS logs;");
    }

    public void dropProductTable() {
        executeDDL("DROP TABLE IF EXISTS product;");
    }

    public void dropUserTable() {
        executeDDL("DROP TABLE IF EXISTS users;");
    }

    private void executeDDL(String sql) {
        try (Connection conn = DriverManager.getConnection(driverURL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ex) {
            System.err.println("DDL error: " + ex.getMessage());
        }
    }

    public void addHistory(String username, String name, int stock, String timestamp) {
        validateString(username, "username");
        validateString(name, "history name");
        validateNonNegative(stock, "stock");
        validateString(timestamp, "timestamp");
        String sql = "INSERT INTO history(username,name,stock,timestamp) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setInt(3, stock);
            pstmt.setString(4, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error adding history: " + ex.getMessage());
        }
    }

    public void addLogs(String event, String username, String desc, String timestamp) {
        validateString(event, "event");
        validateString(username, "username");
        validateString(desc, "desc");
        validateString(timestamp, "timestamp");
        String sql = "INSERT INTO logs(event,username,desc,timestamp) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, event);
            pstmt.setString(2, username);
            pstmt.setString(3, desc);
            pstmt.setString(4, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error adding log: " + ex.getMessage());
        }
    }

    public void addProduct(String name, int stock, double price) {
        validateString(name, "product name");
        validateNonNegative(stock, "stock");
        validateNonNegative(price, "price");
        String sql = "INSERT INTO product(name,stock,price) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, stock);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error adding product: " + ex.getMessage());
        }
    }

    public void addUser(String username, String password) {

        String userNorm = username.trim().toLowerCase();

        validateString(userNorm, "username");
        validateString(password, "password");

        if (isUsernameTaken(userNorm)) {
            throw new IllegalArgumentException("Username \"" + userNorm + "\" is already taken");
        }

        String hashedPw = PasswordUtil.hash(password);
        String sql = "INSERT INTO users(username,password) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userNorm);
            pstmt.setString(2, hashedPw);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error adding user: " + ex.getMessage());
        }
    }


    public void addUser(String username, String password, int role) {
        validateString(username, "username");
        validateString(password, "password");
        if (role < 1 || role > 5) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        String hashedPw = PasswordUtil.hash(password);
        String sql = "INSERT INTO users(username,password,role) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPw);
            pstmt.setInt(3, role);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error adding user with role: " + ex.getMessage());
        }
    }

    public void removeUser(String username) {
        validateString(username, "username");
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error removing user: " + ex.getMessage());
        }
    }

    public ArrayList<History> getHistory() {
        String sql = "SELECT id, username, name, stock, timestamp FROM history";
        ArrayList<History> histories = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                histories.add(new History(rs.getInt("id"), rs.getString("username"), rs.getString("name"), rs.getInt("stock"), rs.getString("timestamp")));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching history: " + ex.getMessage());
        }
        return histories;
    }

    public ArrayList<Logs> getLogs() {
        String sql = "SELECT id, event, username, desc, timestamp FROM logs";
        ArrayList<Logs> logs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                logs.add(new Logs(rs.getInt("id"), rs.getString("event"), rs.getString("username"), rs.getString("desc"), rs.getString("timestamp")));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching logs: " + ex.getMessage());
        }
        return logs;
    }

    public ArrayList<Product> getProduct() {
        String sql = "SELECT id, name, stock, price FROM product";
        ArrayList<Product> products = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                products.add(new Product(rs.getInt("id"), rs.getString("name"), rs.getInt("stock"), rs.getFloat("price")));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching products: " + ex.getMessage());
        }
        return products;
    }

    public Product getProduct(String name) {
        validateString(name, "product name");
        String sql = "SELECT name, stock, price FROM product WHERE name = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(rs.getString("name"), rs.getInt("stock"), rs.getFloat("price"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching product: " + ex.getMessage());
        }
        return null;
    }

    public ArrayList<User> getUsers() {
        String sql = "SELECT id, username, password, role, locked FROM users";
        ArrayList<User> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getInt("role"), rs.getInt("locked")));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching users: " + ex.getMessage());
        }
        return users;
    }

    public boolean isUsernameTaken(String username) {
        validateString(username, "username");
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.err.println("Error checking username: " + ex.getMessage());
            return true;
        }
    }

    public boolean authenticate(String username, String plainPassword) {
        validateString(username, "username");
        validateString(plainPassword, "password");
        String selectSql = "SELECT password, locked, failed_attempts FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) return false;
                if (rs.getInt("locked") == 1) return false;
                String storedHash = rs.getString("password");
                boolean ok = PasswordUtil.verify(plainPassword, storedHash);
                if (ok) {
                    try (PreparedStatement upd = conn.prepareStatement("UPDATE users SET failed_attempts = 0 WHERE username = ?")) {
                        upd.setString(1, username);
                        upd.executeUpdate();
                    }
                } else {
                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE users SET failed_attempts = failed_attempts + 1, locked = CASE WHEN failed_attempts + 1 >= ? THEN 1 ELSE locked END WHERE username = ?")) {
                        upd.setInt(1, MAX_FAILED_ATTEMPTS);
                        upd.setString(2, username);
                        upd.executeUpdate();
                    }
                }
                return ok;
            }
        } catch (SQLException ex) {
            System.err.println("Authentication error: " + ex.getMessage());
            return false;
        }
    }

    public int getUserRole(String username) {
        validateString(username, "username");
        String sql = "SELECT role FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("role");
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching role: " + ex.getMessage());
        }
        return -1;
    }

    public boolean isAccountLocked(String username) {
        validateString(username, "username");
        String sql = "SELECT locked FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("locked") == 1;
            }
        } catch (SQLException ex) {
            System.err.println("Error checking lock: " + ex.getMessage());
        }
        return false;
    }

    // Input validation helpers
    private void validateString(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    private void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    private void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
