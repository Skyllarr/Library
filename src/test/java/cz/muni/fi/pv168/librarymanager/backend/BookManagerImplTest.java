package cz.muni.fi.pv168.librarymanager.backend;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.muni.fi.pv168.librarymanager.common.DBUtils;
import cz.muni.fi.pv168.librarymanager.common.EntityNotFoundException;
import java.util.function.Consumer;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author diana vilkolakova
 */
public class BookManagerImplTest {

    private BookManagerImpl manager;
    private DataSource dataSource;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }

    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        DBUtils.executeSqlScript(dataSource, BookManager.class.getResource("createTables.sql"));
        manager = new BookManagerImpl();
        manager.setDataSource(dataSource);
    }

    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(dataSource, BookManager.class.getResource("dropTables.sql"));
    }

    private BookBuilder sampleBookBuilder() {
        return new BookBuilder()
                .id(null)
                .author("sampleAuthor")
                .title("sampleTitle")
                .yearOfPublication(1);
    }

    @Test
    public void createBook() {
        Book book = newBook("jean", "Good title", 1992);
        manager.createBook(book);

        Long bookId = book.getId();
        assertThat(bookId).isNotNull();

        assertThat(manager.getBook(bookId))
                .isNotSameAs(book)
                .isEqualToComparingFieldByField(book);
    }

    @Test
    public void getAllBooks() {
        assertThat(manager.findAllBooks()).isEmpty();

        Book g1 = newBook("jean", "Good title", 1992);
        Book g2 = newBook("jeanA", "Haha", 1995);

        manager.createBook(g1);
        manager.createBook(g2);

        assertThat(manager.findAllBooks())
                .usingFieldByFieldElementComparator()
                .containsOnly(g1, g2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNullBook() {
        manager.createBook(null);
    }

    @Test
    public void createBookWithExistingId() {
        Book book = newBook("jean", "Good title", 1992);
        book.setId(1l);
        expectedException.expect(IllegalArgumentException.class);
        manager.createBook(book);
    }

    @Test
    public void createBookWithEmptyAuthor() {
        Book book = newBook("", "Good title", 1992);
        assertThatThrownBy(() -> manager.createBook(book))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void createBookWithNegativeYear() {
        Book book = newBook("jean", "Good title", -5);
        expectedException.expect(IllegalArgumentException.class);
        manager.createBook(book);
    }

    private void testUpdate(Consumer<Book> updateOperation) {
        Book sourceBook = newBook("jean", "Good title", 1992);
        Book anotherBook = newBook("jeanA", "Good title2", 1995);
        manager.createBook(sourceBook);
        manager.createBook(anotherBook);

        updateOperation.accept(sourceBook);
        manager.updateBook(sourceBook);

        assertThat(manager.getBook(sourceBook.getId()))
                .isEqualToComparingFieldByField(sourceBook);
        // Check if updates didn't affected other records
        assertThat(manager.getBook(anotherBook.getId()))
                .isEqualToComparingFieldByField(anotherBook);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNullBook() {
        manager.updateBook(null);
    }

    @Test
    public void updateBookWithNullId() {
        Book book = newBook("jean", "Good title", 1992);
        manager.createBook(book);
        book.setId(null);
        expectedException.expect(IllegalArgumentException.class);
        manager.updateBook(book);
    }

    @Test
    public void updateBookWithNonExistingId() {
        Book book = newBook("jean", "Good title", 1992);
        manager.createBook(book);
        book.setId(book.getId() + 1);
        expectedException.expect(EntityNotFoundException.class);
        manager.updateBook(book);
    }

    @Test
    public void updateBookWithEmptyTitle() {
        Book book = newBook("jean", "title", 1992);
        manager.createBook(book);
        book.setTitle("");
        expectedException.expect(IllegalArgumentException.class);
        manager.updateBook(book);
    }

    @Test
    public void updateBookWithNegativeyearofpublication() {
        Book book = newBook("jean", "Good title", 1992);
        manager.createBook(book);
        book.setYearOfPublication(-1);
        expectedException.expect(IllegalArgumentException.class);
        manager.updateBook(book);
    }

    @Test
    public void deleteBook() {

        Book g1 = newBook("jean", "Good title", 1992);
        Book g2 = newBook("detto", "Good title2", 1997);
        manager.createBook(g1);
        manager.createBook(g2);

        assertThat(manager.getBook(g1.getId())).isNotNull();
        assertThat(manager.getBook(g2.getId())).isNotNull();

        manager.deleteBook(g1);

        assertThat(manager.getBook(g1.getId())).isNull();
        assertThat(manager.getBook(g2.getId())).isNotNull();

    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNullBook() {
        manager.deleteBook(null);
    }

    @Test
    public void deleteBookWithNullId() {
        Book book = newBook("jean", "Good title", 1992);
        book.setId(null);
        expectedException.expect(IllegalArgumentException.class);
        manager.deleteBook(book);
    }

    @Test
    public void deleteBookWithNonExistingId() {
        Book book = newBook("jean", "Good title", 1992);
        book.setId(1L);
        expectedException.expect(EntityNotFoundException.class);
        manager.deleteBook(book);
    }

    private static Book newBook(String author, String title, int yearofpublication) {
        Book book = new Book();
        book.setAuthor(author);
        book.setTitle(title);
        book.setYearOfPublication(yearofpublication);
        return book;
    }
}