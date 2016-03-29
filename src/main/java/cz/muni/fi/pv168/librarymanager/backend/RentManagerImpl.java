package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.DBUtils;
import cz.muni.fi.pv168.librarymanager.common.EntityNotFoundException;
import cz.muni.fi.pv168.librarymanager.common.IllegalEntityException;
import cz.muni.fi.pv168.librarymanager.common.ServiceFailureException;
import cz.muni.fi.pv168.librarymanager.common.ValidationException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class RentManagerImpl implements RentManager {
    
    private static final Logger logger = Logger.getLogger(
            RentManagerImpl.class.getName());
    
    private DataSource dataSource;
    
    private final Clock clock;
    
    public RentManagerImpl(Clock clock) {
        this.clock = clock;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }
    
    private void validate(Rent rent) throws IllegalArgumentException, IllegalEntityException{
        if (rent.getClient() == null) {
            throw new ValidationException("client is null");
        }
        if (rent.getBook() == null) {
            throw new ValidationException("book is null");
        }
        if (rent.getClient().getId() == null) {
            throw new IllegalEntityException("client id is null");
        }
        if (rent.getBook().getId() == null) {
            throw new IllegalEntityException("book id is null");
        }
        /*if (rent == null) {
            throw new IllegalArgumentException("rent is null");
        }
        if (rent.getBook() == null) {
            throw new ValidationException("book is null");
        }
        if (rent.getClient() == null) {
            throw new ValidationException("client is null");
        }
        if (rent.getStartDay() == null) {
            throw new ValidationException("startDay is null");
        }
        if (rent.getEndDay() == null) {
            throw new ValidationException("endDay is null");
        }
        
        LocalDate today = LocalDate.now(clock);
        if (rent.getStartDay() != null && rent.getStartDay().isBefore(today)) {
            throw new ValidationException("rent starts in past");
        }
        if (rent.getEndDay() != null && rent.getEndDay().isBefore(today)) {
            throw new ValidationException("rent ends in past");
        }
        if (rent.getStartDay() != null && rent.getEndDay() != null && rent.getEndDay().isBefore(rent.getStartDay())) {
            throw new ValidationException("rent end is before rent start");
        }*/     
    }


    @Override
    public void createRent(Rent rent) throws ServiceFailureException {
        checkDataSource();
        validate(rent);
              
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            checkIfBookIsNotRent(conn, rent.getBook());

            st = conn.prepareStatement(
                    "INSERT INTO Rent (clientid,bookid,startday,endday) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            st.setLong(1, rent.getClient().getId());
            st.setLong(2, rent.getBook().getId());

            
            st.setDate(3, toSqlDate(rent.getStartDay()));
            st.setDate(4, toSqlDate(rent.getEndDay()));

            int count = st.executeUpdate();
            DBUtils.checkUpdatesCount(count, rent, true);

            Long id = DBUtils.getId(st.getGeneratedKeys());
            rent.setId(id);
            conn.commit();
        } catch (SQLException ex) {
            String msg = "Error when inserting rent into db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(conn);
            DBUtils.closeQuietly(conn, st);
        }
    }

    @Override
    public void updateRent(Rent rent) throws ServiceFailureException {
        checkDataSource();
        validate(rent);
        
        Connection conn = null;
        PreparedStatement updateSt = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            updateSt = conn.prepareStatement(
                    "UPDATE Rent SET startday = ?, endday = ? WHERE id = ?");
            updateSt.setDate(1, toSqlDate(rent.getStartDay()));
            updateSt.setDate(2, toSqlDate(rent.getEndDay()));
            updateSt.setLong(3, rent.getId());
            
            
            int count = updateSt.executeUpdate();
            if (count == 0) {
                throw new EntityNotFoundException("Rent " + rent + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid updated rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when updating rent in db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(conn);
            DBUtils.closeQuietly(conn, updateSt);
        }
    }

    @Override
    public void deleteRent(Rent rent) {
        checkDataSource();
        validate(rent);
        
        Connection conn = null;
        // difference between try ( and try {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "DELETE FROM rent WHERE id = ?")) {

            st.setLong(1, rent.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new EntityNotFoundException("Rent " + rent + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid deleted rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when updating book " + rent, ex);
        }
    }

    @Override
    public List<Rent> findDelayedReturns() {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,clientid,bookid,startday,endday FROM rent WHERE endday > ?")) {

            LocalDate today = LocalDate.now(clock);
            st.setDate(1, toSqlDate(today));
            ResultSet rs = st.executeQuery();

            List<Rent> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToRent(rs));
            }
            return result;

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving delayed books", ex);
        }
    }

    @Override
    public Client findClientByRentBook(Book book) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT client.id,name,surname FROM rent JOIN client WHERE bookid = ?")) {

            st.setLong(1, book.getId());
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Client client = resultSetToClient(rs);

                if (rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + book.getId() + ", found " + client + " and " + resultSetToClient(rs));
                }

                return client;
            } else {
                return null;
            }

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving client with id " + book.getId(), ex);
        }
    }

    @Override
    public List<Book> findRentBooksByClient(Client client) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT book.id,author,title,yearofpublication "+
                        "FROM rent JOIN book"+
                        "WHERE clientid = ?")) {

            st.setLong(1, client.getId());
            ResultSet rs = st.executeQuery();

            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToBook(rs));
            }
            return result;

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving all books", ex);
        }
    }
    
    private Client resultSetToClient(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getLong("id"));
        client.setName(rs.getString("name"));
        client.setSurname(rs.getString("surname"));
        return client;
    }
    
    private Book resultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setAuthor(rs.getString("author"));
        book.setTitle(rs.getString("title"));
        book.setYearOfPublication(rs.getInt("yearofpublication"));
        return book;
    }
    
    private Rent resultSetToRent(ResultSet rs) throws SQLException {
        Rent rent = new Rent();
        rent.setId(rs.getLong("id"));
        rent.setClientId(rs.getLong("clientid"));
        rent.setBookId(rs.getLong("bookid"));
        rent.setStartDay(toLocalDate(rs.getDate("startday")));
        rent.setEndDay(toLocalDate(rs.getDate("endday")));
        return rent;
    }
    
    private static void checkIfBookIsNotRent(Connection conn, Book book) throws IllegalEntityException, SQLException {
    PreparedStatement checkSt = null;
        try {
            checkSt = conn.prepareStatement(
                    "SELECT COUNT(bookid) as rentsCount " +
                    "FROM rent " +
                    "WHERE bookid = ? ");
            checkSt.setLong(1, book.getId());
            ResultSet rs = checkSt.executeQuery();
            if (rs.next()) {
                if (rs.getInt("rentsCount")>0) {
                    throw new IllegalEntityException("Book " + book + " is already rent");
                }
            } else {
                throw new IllegalEntityException("Book " + book + " does not exist in the database");
            }
        } finally {
            DBUtils.closeQuietly(null, checkSt);
        }
    }
    
    private static Date toSqlDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    @Override
    public Rent findRentById(Long id) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT clientid, bookid, startday, endday FROM rent WHERE id = ?")) {

            st.setLong(1, id);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Rent rent = resultSetToRent(rs);

                if (rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + rent + " and " + resultSetToRent(rs));
                }

                return rent;
            } else {
                return null;
            }

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving rent with id " + id, ex);
        }
    }

}