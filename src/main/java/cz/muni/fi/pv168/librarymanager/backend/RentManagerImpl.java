package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;
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
 * @author Diana Vilkolakova
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 *
 */
public class RentManagerImpl implements RentManager {
    
    private static final Logger logger = Logger.getLogger(
            RentManagerImpl.class.getName());
    
    private DataSource dataSource;
    
    //private final Clock clock;
    private Clock clock;
    
    public RentManagerImpl(Clock clock) {
        this.clock = clock;
    }
    
    protected void setClock(Clock clock) {
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
        if (rent == null) {
            throw new IllegalArgumentException("rent is null");
        }
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
        }
    }

    private Long getKey(ResultSet keyRS, Rent rent) throws ServiceFailureException, SQLException {
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert rent " + rent
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            Long result = keyRS.getLong(1);
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert rent " + rent
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert rent " + rent
                    + " - no key found");
        }
    }

    @Override
    public void createRent(Rent rent) throws ServiceFailureException {
        checkDataSource();
        validate(rent);
        
        if (rent.getId() != null) {
            throw new IllegalEntityException("rent id is already set");
        }
              
        try (Connection connection = dataSource.getConnection()) {
            checkIfBookIsNotRent(connection, rent.getBook()); 
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT INTO Rent (clientid,bookid,startday,endday) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                
                st.setLong(1, rent.getClient().getId());
                st.setLong(2, rent.getBook().getId());
                st.setDate(3, toSqlDate(rent.getStartDay()));
                st.setDate(4, toSqlDate(rent.getEndDay()));

                int addedRows = st.executeUpdate();
                if (addedRows != 1) {
                    throw new ServiceFailureException("Internal Error: More rows ("
                            + addedRows + ") inserted when trying to insert rent " + rent);
                }

                ResultSet keyRS = st.getGeneratedKeys();
                rent.setId(getKey(keyRS,rent));
            }
        } catch (SQLException ex) {
            String msg = "Error when inserting rent into db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } 
    }

    @Override
    public void updateRent(Rent rent) throws ServiceFailureException {
        checkDataSource();
        validate(rent);
        
        if (rent.getId() == null) {
            throw new IllegalEntityException("rent id is null");
        }
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement updateSt = conn.prepareStatement(
                "UPDATE Rent SET bookid = ?, startday = ?, endday = ? WHERE id = ?")) {
            
            updateSt.setLong(1, rent.getBook().getId());
            updateSt.setDate(2, toSqlDate(rent.getStartDay()));
            updateSt.setDate(3, toSqlDate(rent.getEndDay()));
            updateSt.setLong(4, rent.getId());
            
            int count = updateSt.executeUpdate();
            if (count == 0) {
                throw new IllegalEntityException("Rent " + rent + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid updated rows count "
                        + "detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when updating rent in db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } 
    }

    @Override
    public void deleteRent(Rent rent) {
        checkDataSource();
        validate(rent);
        if (rent.getId() == null) {
            throw new IllegalEntityException("rent id is null");
        }
        
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "DELETE FROM rent WHERE id = ?")) {

            st.setLong(1, rent.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new IllegalEntityException("Rent " + rent + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid deleted rows count "
                        + "detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when deleting rent in db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    @Override
    public List<Rent> findDelayedReturns() {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT rent.id, client.id, client.name, client.surname, "+
                "book.id, book.title, book.author, book.yearofpublication, "+
                "startday, endday  FROM rent INNER JOIN client ON rent.clientid"+
                "=client.id INNER JOIN book ON rent.bookid=book.id WHERE rent.endday < ?")) {

            LocalDate today = LocalDate.now(clock);
            st.setDate(1, toSqlDate(today));
            ResultSet rs = st.executeQuery();

            List<Rent> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToRent(rs));
            }
            return result;

        } catch (SQLException ex) {
            String msg = "Error when retrieving delayed rents";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    
    @Override
    public Rent getRent(Long id) {
        checkDataSource();
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT rent.id, client.id, client.name, client.surname, "+
                "book.id, book.title, book.author, book.yearofpublication, "+
                "startday, endday  FROM rent INNER JOIN client ON rent.clientid"+
                "=client.id INNER JOIN book ON rent.bookid=book.id WHERE client.id=?")) {

            st.setLong(1, id);
            System.out.println(st);
            return executeQueryForSingleRent(st);
        } catch (SQLException ex) {
            String msg = "Error when retrieving rent with id " + id;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }
    
    @Override
    public Client findClientByRentBook(Book book) {
        checkDataSource();
        if (book == null) {
            throw new IllegalArgumentException("book is null");
        }

        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                        "SELECT client.id,name,surname FROM rent INNER JOIN client "+
                        "ON rent.clientid=client.id WHERE rent.bookid=?")) {

            st.setLong(1, book.getId());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                Client client = ClientManagerImpl.resultSetToClient(rs);
                if (rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source book id: " + book.getId() + ", found " + client + 
                            " and " + ClientManagerImpl.resultSetToClient(rs));
                }
                return client;
            } else {
                return null;
            }

        } catch (SQLException ex) {
            String msg = "Error when retrieving client with rent book id " + book.getId();
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    @Override
    public List<Book> findRentBooksByClient(Client client) {
        checkDataSource();
        if (client == null) {
            throw new IllegalArgumentException("client is null");
        }
        try (Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT book.id,author,title,yearofpublication "+
                        "FROM rent INNER JOIN book ON rent.bookid=book.id "+
                        "WHERE clientid = ?")) {

            st.setLong(1, client.getId());
            ResultSet rs = st.executeQuery();

            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                result.add(BookManagerImpl.resultSetToBook(rs));
            }
            return result;

        } catch (SQLException ex) {
            String msg = "Error when retrieving books by client " + client;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }
    
    private static void checkIfBookIsNotRent(Connection conn, Book book) throws IllegalEntityException, SQLException {
        try (PreparedStatement checkSt = conn.prepareStatement(
                "SELECT COUNT(bookid) as rentsCount " +
                "FROM rent " +
                "WHERE bookid = ? ")) {
                checkSt.setLong(1, book.getId());
                ResultSet rs = checkSt.executeQuery();
                if (rs.next()) {
                    if (rs.getInt("rentsCount")>0) {
                        throw new IllegalEntityException("Book " + book + " is already rent");
                    }
                } else {
                    throw new IllegalEntityException("Book " + book + " does not exist in the database");
            }
        } 
    }
    
    private static Date toSqlDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    @Override
    public List<Rent> findAllRents() {
        checkDataSource();
        try (Connection conn = dataSource.getConnection();
            PreparedStatement st = conn.prepareStatement(
                "SELECT rent.id, client.id, client.name, client.surname, "+
                "book.id, book.title, book.author, book.yearofpublication, "+
                "startday, endday  FROM rent INNER JOIN client ON rent.clientid"+
                "=client.id INNER JOIN book ON rent.bookid=book.id")) {
            return executeQueryForMultipleRents(st);
        } catch (SQLException ex) {
            String msg = "Error when getting all rents";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }         
    }
    
    private static Rent executeQueryForSingleRent(PreparedStatement st) throws SQLException, ServiceFailureException {
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            Rent result = resultSetToRent(rs);                
            if (rs.next()) {
                throw new ServiceFailureException(
                        "Internal integrity error: more rents with the same id found!");
            }
            return result;
        } else {
            return null;
        }
    }
    
    private static List<Rent> executeQueryForMultipleRents(PreparedStatement st) throws SQLException {
        ResultSet rs = st.executeQuery();
        List<Rent> result = new ArrayList<>();
        while (rs.next()) {
            result.add(resultSetToRent(rs));
        }
        return result;
    }
    
    private static Rent resultSetToRent(ResultSet rs) throws SQLException {
        Rent result = new Rent();
        Client client = new Client();
        Book book = new Book();
        
        result.setId(rs.getLong(1));
        client.setId(rs.getLong(2));
        client.setName(rs.getString(3));
        client.setSurname(rs.getString(4));
        result.setClient(client);
        book.setId(rs.getLong(5));
        book.setTitle(rs.getString(6));
        book.setAuthor(rs.getString(7));
        book.setYearOfPublication(rs.getInt(8));
        result.setBook(book);
        result.setStartDay(toLocalDate(rs.getDate(9)));
        result.setEndDay(toLocalDate(rs.getDate(10)));
        return result;
    }


}