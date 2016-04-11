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
import java.sql.ResultSetMetaData;
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
    
    /* ??? We use it for extract client and book entity from DB (see 
           rowToRent method) */
    /*private ClientManagerImpl clientManager = new ClientManagerImpl();
    private BookManagerImpl bookManager = new BookManagerImpl();*/
    
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
                        + "retriving failed when trying to insert book " + rent
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            Long result = keyRS.getLong(1);
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert book " + rent
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert book " + rent
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
              
        Connection connection = null;
        PreparedStatement st = null;
        try {
            connection = dataSource.getConnection();
            checkIfBookIsNotRent(connection, rent.getBook());

            st = connection.prepareStatement(
                    "INSERT INTO Rent (clientid,bookid,startday,endday) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
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
        } catch (SQLException ex) {
            //String msg = "Error when inserting rent into db";
            //logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException("Error when inserting rent into db", ex);
        } /*finally {
            DBUtils.doRollbackQuietly(connection);
            DBUtils.closeQuietly(connection, st);
        }*/
    }

    @Override
    public void updateRent(Rent rent) throws ServiceFailureException {
        checkDataSource();
        validate(rent);
        
        if (rent.getId() == null) {
            throw new IllegalEntityException("rent id is null");
        }
        
        Connection conn = null;
        PreparedStatement updateSt = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            updateSt = conn.prepareStatement(
                    "UPDATE Rent SET bookid = ?, startday = ?, endday = ? WHERE id = ?");
            updateSt.setLong(1, rent.getBook().getId());
            updateSt.setDate(2, toSqlDate(rent.getStartDay()));
            updateSt.setDate(3, toSqlDate(rent.getEndDay()));
            updateSt.setLong(4, rent.getId());
            
            int count = updateSt.executeUpdate();
            if (count == 0) {
                throw new IllegalEntityException("Rent " + rent + " was not found in database!");
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
        
        if (rent.getId() == null) {
            throw new IllegalEntityException("rent id is null");
        }
        
        Connection conn = null;
        // ??? difference between try ( and try {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "DELETE FROM rent WHERE id = ?")) {

            st.setLong(1, rent.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new IllegalEntityException("Rent " + rent + " was not found in database!");
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
                result.add(rowToRent(rs));
            }
            return result;

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving delayed books", ex);
        }
    }

    
    @Override
    public Rent getRent(Long id) {
        checkDataSource();
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        Connection connection = null;
        PreparedStatement st = null;
        try {
            connection = dataSource.getConnection();
            st = connection.prepareStatement(
                "SELECT rent.id, client.id, client.name, client.surname, "+
                "book.id, book.title, book.author, book.yearofpublication, "+
                "startday, endday  FROM rent INNER JOIN client ON rent.clientid"+
                "=client.id INNER JOIN book ON rent.bookid=book.id WHERE client.id=?");

            st.setLong(1, id);
            System.out.println(st);
            return executeQueryForSingleRent(st);
        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving rent with id " + id, ex);
        }
    }
    
    @Override
    public Client findClientByRentBook(Book book) {
        checkDataSource();
        if (book == null) {
            throw new IllegalArgumentException("book is null");
        }
        Connection connection = null;
        PreparedStatement st = null;
        try {
            connection = dataSource.getConnection();
            st = connection.prepareStatement(
                        "SELECT client.id,name,surname FROM rent INNER JOIN client "+
                        "ON rent.clientid=client.id WHERE rent.bookid=?");

            st.setLong(1, book.getId());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                //Client client = resultSetToClient(rs);
                Client client = ClientManagerImpl.resultSetToClient(rs);
                if (rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + book.getId() + ", found " + client + 
                            " and " + ClientManagerImpl.resultSetToClient(rs));
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
        checkDataSource();
        if (client == null) {
            throw new IllegalArgumentException("client is null");
        }
        Connection connection = null;
        PreparedStatement st = null;
        try {
                connection = dataSource.getConnection();
                st = connection.prepareStatement(
                        "SELECT book.id,author,title,yearofpublication "+
                        "FROM rent INNER JOIN book ON rent.bookid=book.id "+
                        "WHERE clientid = ?");

            st.setLong(1, client.getId());
            ResultSet rs = st.executeQuery();

            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                //result.add(resultSetToBook(rs));
                result.add(BookManagerImpl.resultSetToBook(rs));
            }
            return result;

        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when retrieving books by client", ex);
        }
    }
    
    /* ??? These two method aren't necessary because of duplicity of code.
           I made method BookManagerImpl.resultSetToBook() and 
           ClientManagerImpl.resultSetToClient() public static, but I'm not sure
           it's good approach
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
    */
    
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
    public List<Rent> findAllRents() {
        checkDataSource();
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.prepareStatement(
                    "SELECT rent.id, client.id, client.name, client.surname, "+
                "book.id, book.title, book.author, book.yearofpublication, "+
                "startday, endday  FROM rent INNER JOIN client ON rent.clientid"+
                "=client.id INNER JOIN book ON rent.bookid=book.id");
            return executeQueryForMultipleRents(st);
        } catch (SQLException ex) {
            String msg = "Error when getting all clients from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(conn, st);
        }          
    }
    
    private static Rent executeQueryForSingleRent(PreparedStatement st) throws SQLException, ServiceFailureException {
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            Rent result = rowToRent(rs);                
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
        List<Rent> result = new ArrayList<Rent>();
        while (rs.next()) {
            result.add(rowToRent(rs));
        }
        return result;
    }
    
    private static Rent rowToRent(ResultSet rs) throws SQLException {
        Rent result = new Rent();
        Client client = new Client();
        Book book = new Book();
        
        /* ??? Why I can't get values through dot notation 
        result.setId(rs.getLong("rent.id"));
        client.setId(rs.getLong("client.id"));
        client.setName(rs.getString("client.name"));
        client.setSurname(rs.getString("client.surname"));
        book.setId(rs.getLong("book.id"));
        book.setTitle(rs.getString("book.title"));
        book.setAuthor(rs.getString("book.author"));
        book.setYearOfPublication(rs.getInt("book.yearofpublication"));
        result.setStartDay(toLocalDate(rs.getDate("startday")));
        result.setEndDay(toLocalDate(rs.getDate("endday")));*/
        
        // Numbering of columns starts at 1
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