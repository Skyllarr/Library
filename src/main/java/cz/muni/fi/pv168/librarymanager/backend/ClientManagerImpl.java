package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.DBUtils;
import cz.muni.fi.pv168.librarymanager.common.EntityNotFoundException;
import cz.muni.fi.pv168.librarymanager.common.ServiceFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class ClientManagerImpl implements ClientManager {
    
    private static final Logger logger = Logger.getLogger(
            ClientManagerImpl.class.getName());
    
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }

    @Override
    public Client findClientById(Long id) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,name,surname FROM client WHERE id = ?")) {

            st.setLong(1, id);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Client client = resultSetToClient(rs);

                if (rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + client + " and " + resultSetToClient(rs));
                }

                return client;
            } else {
                return null;
            }

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving book with id " + id, ex);
        }
    }

    @Override
    public List<Client> findAllClients() {
        checkDataSource();
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.prepareStatement(
                    "SELECT id, surname, name FROM Client");
            return executeQueryForMultipleClients(st);
        } catch (SQLException ex) {
            String msg = "Error when getting all clients from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(conn, st);
        }          
    }

    @Override
    public void createClient(Client client) throws ServiceFailureException {

        validate(client);
        if (client.getId() != null) {
            throw new IllegalArgumentException("client id is already set");
        }

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "INSERT INTO CLIENT (name, surname) VALUES (?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

            st.setString(1, client.getName());
            st.setString(2, client.getSurname());
            int addedRows = st.executeUpdate();
            if (addedRows != 1) {
                throw new ServiceFailureException("Internal Error: More rows ("
                        + addedRows + ") inserted when trying to insert book " + client);
            }

            ResultSet keyRS = st.getGeneratedKeys();
            client.setId(getKey(keyRS, client));

        } catch (SQLException ex) {
            throw new ServiceFailureException("Error when inserting book " + client, ex);
        }
    }

    private void validate(Client client) throws IllegalArgumentException {
        if (client == null) {
            throw new IllegalArgumentException("book is null");
        }
        if (client.getName().isEmpty()) {
            throw new IllegalArgumentException("book title is empty");
        }
        if (client.getSurname().isEmpty()) {
            throw new IllegalArgumentException("book author is empty");
        }
    }

    private Long getKey(ResultSet keyRS, Client client) throws ServiceFailureException, SQLException {
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert book " + client
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            Long result = keyRS.getLong(1);
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert book " + client
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert book " + client
                    + " - no key found");
        }
    }

    private Client resultSetToClient(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getLong("id"));
        client.setName(rs.getString("name"));
        client.setSurname(rs.getString("surname"));
        return client;
    }

    @Override
    public void updateClient(Client client) {
        validate(client);
        if (client.getId() == null) {
            throw new IllegalArgumentException("client id is null");
        }
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "UPDATE Client SET name = ?, surname = ? WHERE id = ?")) {

            st.setString(1, client.getName());
            st.setString(2, client.getSurname());
            st.setLong(4, client.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new EntityNotFoundException("Book " + client + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid updated rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when updating book " + client, ex);
        }
    }

    @Override
    public void deleteClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("client is null");
        }
        if (client.getId() == null) {
            throw new IllegalArgumentException("client id is null");
        }
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "DELETE FROM client WHERE id = ?")) {

            st.setLong(1, client.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new EntityNotFoundException("Client " + client + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid deleted rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when updating book " + client, ex);
        }
    }

    @Override
    public List<Client> findClientsByName(String name) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,name, surname FROM book WHERE name = ?")) {

            st.setString(1, name);
            ResultSet rs = st.executeQuery();

            List<Client> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToClient(rs));
            }
            return result;

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving all clients", ex);
        }
    }

    @Override
    public List<Client> findClientsBySurname(String surname) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,name,surname FROM book WHERE surname = ?")) {

            st.setString(1, surname);
            ResultSet rs = st.executeQuery();

            List<Client> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToClient(rs));
            }
            return result;

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving all clients", ex);
        }
    }

    
    static List<Client> executeQueryForMultipleClients(PreparedStatement st) throws SQLException {
        ResultSet rs = st.executeQuery();
        List<Client> result = new ArrayList<Client>();
        while (rs.next()) {
            result.add(rowToClient(rs));
        }
        return result;
    }
    
    static Client executeQueryForSingleClient(PreparedStatement st) throws SQLException, ServiceFailureException {
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            Client result = rowToClient(rs);                
            if (rs.next()) {
                throw new ServiceFailureException(
                        "Internal integrity error: more clients with the same id found!");
            }
            return result;
        } else {
            return null;
        }
    }
    
    private static Client rowToClient(ResultSet rs) throws SQLException {
        Client result = new Client();
        result.setId(rs.getLong("id"));
        result.setName(rs.getString("name"));
        result.setSurname(rs.getString("surname"));
        return result;
    }
}
