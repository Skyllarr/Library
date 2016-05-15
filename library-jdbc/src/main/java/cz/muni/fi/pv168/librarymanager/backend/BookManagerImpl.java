package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.ServiceFailureException;
import cz.muni.fi.pv168.librarymanager.common.IllegalEntityException;
import cz.muni.fi.pv168.librarymanager.common.ValidationException;
import java.sql.Connection;
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
public class BookManagerImpl implements BookManager {

    private static final Logger logger = Logger.getLogger(
            BookManagerImpl.class.getName());
    
    private DataSource dataSource;

    private final Clock clock;

    public BookManagerImpl(Clock clock) {
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
    
    @Override
    public List<Book> findAllBooks() {
        checkDataSource();
        try (Connection conn = dataSource.getConnection();
            PreparedStatement st = conn.prepareStatement(
                    "SELECT id, author, title, yearofpublication FROM Book");
            ResultSet rs = st.executeQuery()) {
            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToBook(rs));
            }
            return result;
        } catch (SQLException ex) {
            String msg = "Error when getting all books from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }          
    }

    @Override
    public void createBook(Book book) throws ServiceFailureException {
        checkDataSource();
        validate(book);
        if (book.getId() != null) {
            throw new IllegalEntityException("book id is already set");
        }

        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "INSERT INTO BOOK (author,title,yearofpublication) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {

            st.setString(1, book.getAuthor());
            st.setString(2, book.getTitle());
            st.setInt(3, book.getYearOfPublication());
            int addedRows = st.executeUpdate();
            if (addedRows != 1) {
                throw new ServiceFailureException("Internal Error: More rows ("
                        + addedRows + ") inserted when trying to insert book " + book);
            }

            ResultSet keyRS = st.getGeneratedKeys();
            book.setId(getKey(keyRS, book));

        } catch (SQLException ex) {
            String msg = "Error when inserting book " + book;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    private void validate(Book book) throws IllegalArgumentException {
        if (book == null) {
            throw new IllegalArgumentException("book is null");
        }
        if (book.getTitle() == null) {
            throw new ValidationException("book title is null");
        }
        if (book.getAuthor() == null) {
            throw new ValidationException("book author is null");
        }
        if (book.getTitle().isEmpty()) {
            throw new ValidationException("book title is empty");
        }
        if (book.getAuthor().isEmpty()) {
            throw new ValidationException("book author is empty");
        }
        if (book.getYearOfPublication() < 0) {
            throw new ValidationException("book yearofpublication is negative number");
        }
        LocalDate today = LocalDate.now(clock);
        if (book.getYearOfPublication() > today.getYear()) {
            throw new ValidationException("book yearofpublication is in future");
        }
    }

    private Long getKey(ResultSet keyRS, Book book) throws ServiceFailureException, SQLException {
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert book " + book
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            Long result = keyRS.getLong(1);
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert book " + book
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert book " + book
                    + " - no key found");
        }
    }


    @Override
    public void updateBook(Book book) {
        checkDataSource();
        validate(book);
        if (book.getId() == null) {
            throw new IllegalEntityException("book id is null");
        }
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "UPDATE Book SET author = ?, title = ?, yearofpublication = ? WHERE id = ?")) {

            st.setString(1, book.getAuthor());
            st.setString(2, book.getTitle());
            st.setInt(3, book.getYearOfPublication());
            st.setLong(4, book.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new IllegalEntityException("Book " + book + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid updated rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when updating book " + book;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
            
        }
    }

    @Override
    public void deleteBook(Book book) {
        checkDataSource();
        if (book == null) {
            throw new IllegalArgumentException("book is null");
        }
        if (book.getId() == null) {
            throw new IllegalEntityException("book id is null");
        }
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "DELETE FROM book WHERE id = ?")) {

            st.setLong(1, book.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new IllegalEntityException("Book " + book + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid deleted rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            String msg = "Error when deleting book " + book;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        }
    }

    @Override
    public List<Book> findBooksByAuthor(String author) {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT id,author,title,yearofpublication FROM book WHERE author = ?")) {

            st.setString(1, author);
            ResultSet rs = st.executeQuery();

            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToBook(rs));
            }
            return result;

        } catch (SQLException ex) {
            String msg = "Error when retrieving all books by author "+author;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);            
        }
    }

    @Override
    public List<Book> findBooksByTitle(String title) {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT id,author,title,yearofpublication FROM book WHERE title = ?")) {

            st.setString(1, title);
            ResultSet rs = st.executeQuery();

            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToBook(rs));
            }
            return result;

        } catch (SQLException ex) {
            String msg = "Error when retrieving all books with title "+title;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex); 
        }
    }

    @Override
    public Book getBook(Long id) throws ServiceFailureException {
        checkDataSource();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement st = connection.prepareStatement(
                "SELECT id,author,title,yearofpublication FROM book WHERE id = ?")) {

            st.setLong(1, id);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Book book = resultSetToBook(rs);

                if (rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + book + " and " + resultSetToBook(rs));
                }
                return book;
            } else {
                return null;
            }

        } catch (SQLException ex) {
            String msg = "Error when retrieving book with id " + id;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex); 
        }
    }
    
    public static Book resultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setAuthor(rs.getString("author"));
        book.setTitle(rs.getString("title"));
        book.setYearOfPublication(rs.getInt("yearofpublication"));
        return book;
    }
}
