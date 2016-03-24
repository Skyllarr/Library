/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.bookmanager.backend;

import cz.muni.fi.pv168.bookmanager.common.ServiceFailureException;
import cz.muni.fi.pv168.bookmanager.common.EntityNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;

/**
 *
 * @author skylar
 */
public class BookManagerImpl implements BookManager {

    private final DataSource dataSource;

    public BookManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createBook(Book book) throws ServiceFailureException {

        validate(book);
        if (book.getId() != null) {
            throw new IllegalArgumentException("book id is already set");
        }

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "INSERT INTO BOOK (author,title,year) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

            st.setString(1, book.getAuthor());
            st.setString(2, book.getTitle());
            st.setInt(3, book.getYear());
            int addedRows = st.executeUpdate();
            if (addedRows != 1) {
                throw new ServiceFailureException("Internal Error: More rows ("
                        + addedRows + ") inserted when trying to insert book " + book);
            }

            ResultSet keyRS = st.getGeneratedKeys();
            book.setId(getKey(keyRS, book));

        } catch (SQLException ex) {
            throw new ServiceFailureException("Error when inserting book " + book, ex);
        }
    }

    private void validate(Book book) throws IllegalArgumentException {
        if (book == null) {
            throw new IllegalArgumentException("book is null");
        }
        if (book.getTitle().isEmpty()) {
            throw new IllegalArgumentException("book title is empty");
        }
        if (book.getAuthor().isEmpty()) {
            throw new IllegalArgumentException("book author is empty");
        }
        if (book.getYear() <= 0) {
            throw new IllegalArgumentException("book year is negative number");
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

    private Book resultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setAuthor(rs.getString("author"));
        book.setTitle(rs.getString("title"));
        book.setYear(rs.getInt("year"));
        return book;
    }

    @Override
    public void updateBook(Book book) {
        validate(book);
        if (book.getId() == null) {
            throw new IllegalArgumentException("book id is null");
        }
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "UPDATE Book SET author = ?, title = ?, year = ? WHERE id = ?")) {

            st.setString(1, book.getAuthor());
            st.setString(2, book.getTitle());
            st.setInt(3, book.getYear());
            st.setLong(4, book.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new EntityNotFoundException("Book " + book + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid updated rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when updating book " + book, ex);
        }
    }

    @Override
    public void deleteBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("book is null");
        }
        if (book.getId() == null) {
            throw new IllegalArgumentException("book id is null");
        }
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "DELETE FROM book WHERE id = ?")) {

            st.setLong(1, book.getId());

            int count = st.executeUpdate();
            if (count == 0) {
                throw new EntityNotFoundException("Book " + book + " was not found in database!");
            } else if (count != 1) {
                throw new ServiceFailureException("Invalid deleted rows count detected (one row should be updated): " + count);
            }
        } catch (SQLException ex) {
            throw new ServiceFailureException(
                    "Error when updating book " + book, ex);
        }
    }

    @Override
    public List<Book> findAllBooks() {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,author,title,year FROM book")) {

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

    @Override
    public List<Book> findBooksByAuthor(String author) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,author,title,year FROM book WHERE author = ?")) {

            st.setString(1, author);
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

    @Override
    public List<Book> findBooksByTitle(String title) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,author,title,year FROM book WHERE title = ?")) {

            st.setString(1, title);
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

    @Override
    public Book getBook(Long id) throws ServiceFailureException {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        "SELECT id,author,title,year FROM book WHERE id = ?")) {

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
            throw new ServiceFailureException(
                    "Error when retrieving book with id " + id, ex);
        }
    }
}
