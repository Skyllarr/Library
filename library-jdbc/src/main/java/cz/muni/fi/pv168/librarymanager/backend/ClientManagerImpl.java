package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Diana Vilkolakova
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 *
 */
public class ClientManagerImpl implements ClientManager {
    
    private static final Logger logger = LoggerFactory.getLogger(
            ClientManagerImpl.class.getName());
    
    private DataSource dataSource;
    
    public ClientManagerImpl() {}
    
    public ClientManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }

    @Override
    public Client getClient(Long id) {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT id,name,surname FROM client WHERE id = ?")) {

            st.setLong(1, id);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Client client = resultSetToClient(rs);

                if (rs.next()) {
                    logger.error("More clients was found when retrieving client with id"+client.getId());
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + client + 
                            " and " + resultSetToClient(rs));
                }
                logger.info("Retriving client with id "+id);
                return client;
            } else {
                logger.info("Client with id "+id+" was not found");
                return null;
            }

        } catch (SQLException ex) {
            String msg = "Error when retrieving client with id " + id;
            logger.error(msg);
            throw new ServiceFailureException(msg, ex); 
        }
    }

    @Override
    public List<Client> findAllClients() {
        checkDataSource();
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement st = conn.prepareStatement(
                "SELECT id, name, surname FROM Client");
            ResultSet rs = st.executeQuery()) {
            
            List<Client> clients = new ArrayList<>();
            while (rs.next()) {
                clients.add(resultSetToClient(rs));
            }
            logger.info("Retrived all clients");
            return clients;
        } catch (SQLException ex) {
            String msg = "Error when retrivering all clients from DB";
            logger.error(msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    @Override
    public void createClient(Client client) throws ServiceFailureException {
        checkDataSource();
        validate(client);
        if (client.getId() != null) {
            throw new IllegalEntityException("client id is already set");
        }

        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "INSERT INTO CLIENT (name, surname) VALUES (?,?)",
                        Statement.RETURN_GENERATED_KEYS)) { 

            st.setString(1, client.getName());
            st.setString(2, client.getSurname());
            int addedRows = st.executeUpdate();
            if (addedRows != 1) {
                logger.error("Error while creating client - more rows affected");
                throw new ServiceFailureException("Internal Error: More rows ("
                        + addedRows + ") inserted when trying to insert client " + client);
            }

            ResultSet keyRS = st.getGeneratedKeys();
            client.setId(getKey(keyRS, client));
            logger.info("Client was created. Generated id "+client.getId());

        } catch (SQLException ex) {
            String msg = "Error when inserting client " + client;
            logger.error(msg, ex);
            throw new ServiceFailureException(msg, ex); 
        }
    }

    private void validate(Client client) throws IllegalArgumentException {
        if (client == null) {
            throw new IllegalArgumentException("client is null");
        }
        if (client.getName() == null) {
            throw new ValidationException("client name is null");
        }
        if (client.getSurname() == null) {
            throw new ValidationException("client surname is null");
        }
        if (client.getName().isEmpty()) {
            throw new ValidationException("client name is empty");
        }
        if (client.getSurname().isEmpty()) {
            throw new ValidationException("client surname is empty");
        }
    }

    private Long getKey(ResultSet keyRS, Client client) throws ServiceFailureException, SQLException {
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert client " + client
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            Long result = keyRS.getLong(1);
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert client " + client
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert client " + client
                    + " - no key found");
        }
    }

    public static Client resultSetToClient(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getLong("id"));
        client.setName(rs.getString("name"));
        client.setSurname(rs.getString("surname"));
        return client;
    }

    @Override
    public void updateClient(Client client) throws IllegalEntityException {
        checkDataSource();
        validate(client);
        if (client.getId() == null) {
            logger.error("Error while updating client - null id given");
            throw new IllegalEntityException("client id is null");
        }
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "UPDATE Client SET name = ?, surname = ? WHERE id = ?")) {

            st.setString(1, client.getName());
            st.setString(2, client.getSurname());
            st.setLong(3, client.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                logger.error("Client with id "+client.getId()+" not found");
                throw new IllegalEntityException("Client " + client + " was not found in database!");
            } else if (count != 1) {
                logger.error("Error while updating client with id "+client.getId()+" - more rows affected");
                throw new ServiceFailureException("Invalid updated rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when updating client " + client;
            logger.error(msg, ex);
            throw new ServiceFailureException(msg, ex); 
        }
    }

    @Override
    public void deleteClient(Client client) {
        checkDataSource();
        if (client == null) {
            logger.error("Client to delete is null");
            throw new IllegalArgumentException("client is null");
        }
        if (client.getId() == null) {
            logger.error("Client to delete has null id");
            throw new IllegalEntityException("client id is null");
        }
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "DELETE FROM client WHERE id = ?")) {

            st.setLong(1, client.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                logger.error("Client with id "+client.getId()+" to delete has not found");
                throw new IllegalEntityException("Client " + client + " was not found in database!");
            } else if (count != 1) {
                logger.error("Error while deleting client with id "+client.getId()+" - more rows affected");
                throw new ServiceFailureException("Invalid deleted rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when deleting client " + client;
            logger.error(msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    @Override
    public List<Client> findClientsByName(String name) {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT id, name, surname FROM client WHERE name = ?")) {

            st.setString(1, name);
            ResultSet rs = st.executeQuery();

            List<Client> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToClient(rs));
            }
            logger.info("Retriving client with name "+name);
            return result;

        } catch (SQLException ex) {
            String msg = "Error when retrieving client with name "+name;
            logger.error(msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    @Override
    public List<Client> findClientsBySurname(String surname) {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT id,name,surname FROM client WHERE surname = ?")) {

            st.setString(1, surname);
            ResultSet rs = st.executeQuery();

            List<Client> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToClient(rs));
            }
            return result;

        } catch (SQLException ex) {
            String msg = "Error when retrieving clients with surname "+surname;
            logger.error(msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }
    
}
