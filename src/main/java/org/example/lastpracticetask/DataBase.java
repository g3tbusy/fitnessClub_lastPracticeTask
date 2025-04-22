package org.example.lastpracticetask;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DataBase
{
    private static final String host = "localhost";
    private static final String port = "5432";
    private static final String db_name = "fitlife";
    private static final String login = "first";
    private static final String password = "";

    private static Connection dbConn;

    static Connection getDBConnection() throws ClassNotFoundException, SQLException
    {
        String str = "jdbc:postgresql://" + host + ":" + port + "/" + db_name;
        Class.forName("org.postgresql.Driver");
        dbConn = DriverManager.getConnection(str, login, password);
        return dbConn;
    }

    public void isConnection() throws SQLException, ClassNotFoundException
    {
        dbConn = getDBConnection();
        System.out.println(dbConn.isValid(1000));
    }

    public void createTables() throws SQLException {
        Statement stmt = dbConn.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS trainers (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                specialization VARCHAR(100),
                phone VARCHAR(20),
                email VARCHAR(100)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS clients (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                phone VARCHAR(20),
                email VARCHAR(100)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS services (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                price DECIMAL(10,2) NOT NULL,
                is_group BOOLEAN DEFAULT false,
                is_active BOOLEAN DEFAULT true,
                is_subscription BOOLEAN DEFAULT false
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS schedule (
                id SERIAL PRIMARY KEY,
                trainer_id INTEGER REFERENCES trainers(id),
                client_id INTEGER REFERENCES clients(id) NOT NULL,
                service_id INTEGER REFERENCES services(id) NOT NULL,
                training_date DATE NOT NULL,
                training_time TIME NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'scheduled',
                subscription_start_date DATE,
                subscription_end_date DATE,
                termination_reason TEXT,
                CONSTRAINT valid_status CHECK (status IN ('scheduled', 'completed', 'cancelled', 'active', 'terminated')),
                CONSTRAINT time_check CHECK (
                    (subscription_start_date IS NULL AND training_time >= '09:00' AND training_time <= '22:00')
                    OR subscription_start_date IS NOT NULL
                )
            )
        """);

        try {
            ResultSet rs = stmt.executeQuery("SELECT termination_reason FROM schedule LIMIT 1");
            rs.close();
        } catch (SQLException e) {
            stmt.execute("ALTER TABLE schedule ADD COLUMN termination_reason TEXT");
        }

        try {
            ResultSet rs = stmt.executeQuery("SELECT is_subscription FROM services LIMIT 1");
            rs.close();
        } catch (SQLException e) {
            stmt.execute("ALTER TABLE services ADD COLUMN is_subscription BOOLEAN DEFAULT false");
        }

        try {
            ResultSet rs = stmt.executeQuery("SELECT subscription_start_date FROM schedule LIMIT 1");
            rs.close();
        } catch (SQLException e) {
            stmt.execute("ALTER TABLE schedule ADD COLUMN subscription_start_date DATE");
            stmt.execute("ALTER TABLE schedule ADD COLUMN subscription_end_date DATE");
        }

        try {
            stmt.execute("ALTER TABLE schedule DROP CONSTRAINT IF EXISTS time_check");
        } catch (SQLException e) {
        }

        stmt.execute("""
            ALTER TABLE schedule ADD CONSTRAINT time_check CHECK (
                (subscription_start_date IS NULL AND training_time >= '09:00' AND training_time <= '22:00')
                OR subscription_start_date IS NOT NULL
            )
        """);
        
        stmt.close();
    }

    public void recreateServicesTable() throws SQLException {
        Statement stmt = dbConn.createStatement();
        
        stmt.execute("DROP TABLE IF EXISTS schedule");
        stmt.execute("DROP TABLE IF EXISTS services");
        
        String createServicesTable = """
            CREATE TABLE services (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                price DECIMAL(10,2),
                is_group BOOLEAN DEFAULT false
            )
        """;
        
        String createScheduleTable = """
            CREATE TABLE schedule (
                id SERIAL PRIMARY KEY,
                trainer_id INTEGER REFERENCES trainers(id),
                client_id INTEGER REFERENCES clients(id),
                service_id INTEGER REFERENCES services(id),
                training_date DATE NOT NULL,
                training_time TIME NOT NULL,
                status VARCHAR(20) DEFAULT 'scheduled',
                CONSTRAINT time_check CHECK (training_time >= '09:00' AND training_time <= '22:00')
            )
        """;
        
        stmt.execute(createServicesTable);
        stmt.execute(createScheduleTable);
        stmt.close();
    }
}
