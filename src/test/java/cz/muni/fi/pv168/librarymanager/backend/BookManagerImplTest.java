package cz.muni.fi.pv168.librarymanager.backend;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.muni.fi.pv168.librarymanager.common.DBUtils;
import cz.muni.fi.pv168.librarymanager.common.IllegalEntityException;
import cz.muni.fi.pv168.librarymanager.common.ServiceFailureException;
import cz.muni.fi.pv168.librarymanager.common.ValidationException;
import java.time.Clock;
import java.time.LocalDateTime;
import static java.time.Month.MARCH;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author Diana Vilkolakova
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 *
 */
public class BookManagerImplTest {

    private BookManagerImpl manager;
    private DataSource dataSource;
    
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, MARCH, 13, 22, 00).atZone(ZoneId.of("UTC"));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }

    private static Clock prepareClockMock(ZonedDateTime now) {
        return Clock.fixed(now.toInstant(), now.getZone());
    }
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        DBUtils.executeSqlScript(dataSource, BookManager.class.getResource("createTables.sql"));
        manager = new BookManagerImpl(prepareClockMock(NOW));
        manager.setDataSource(dataSource);
    }

    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(dataSource, BookManager.class.getResource("dropTables.sql"));
    }

    private BookBuilder samplePoeBookBuilder() {
        return new BookBuilder()
                .id(null)
                .author("Edgar Allan Poe")
                .title("Havran")
                .yearOfPublication(1995);
    }
    
    private BookBuilder sampleHemBookBuilder() {
        return new BookBuilder()
                .id(null)
                .author("Ernest Hemingway")
                .title("Stařec a moře")
                .yearOfPublication(2005);
    }

    @Test
    public void createBook() {
        Book book = samplePoeBookBuilder().build();
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

        Book bookPoe = samplePoeBookBuilder().build();
        Book bookHem = sampleHemBookBuilder().build();

        manager.createBook(bookPoe);
        manager.createBook(bookHem);

        assertThat(manager.findAllBooks())
                .usingFieldByFieldElementComparator()
                .containsOnly(bookPoe, bookHem);
    }
    
    @Test
    public void findBooksByAuthor() {
        Book bookPoe = samplePoeBookBuilder().build();
        assertThat(manager.findBooksByAuthor(bookPoe.getAuthor())).isEmpty();
        
        manager.createBook(bookPoe);
        
        assertThat(manager.findBooksByAuthor(bookPoe.getAuthor()))
                .usingFieldByFieldElementComparator()
                .containsOnly(bookPoe);
    }

    @Test
    public void findBooksByTitle() {
        Book bookPoe = samplePoeBookBuilder().build();
        assertThat(manager.findBooksByTitle(bookPoe.getTitle())).isEmpty();
        
        manager.createBook(bookPoe);
        
        assertThat(manager.findBooksByTitle(bookPoe.getTitle()))
                .usingFieldByFieldElementComparator()
                .containsOnly(bookPoe);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void createNullBook() {
        manager.createBook(null);
    }

    @Test
    public void createBookWithExistingId() {
        Book book = samplePoeBookBuilder().id(1L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.createBook(book);
    }
    
    @Test
    public void createBookWithEmptyTitle() {
        Book book = sampleHemBookBuilder().title("").build();
        assertThatThrownBy(() -> manager.createBook(book))
                .isInstanceOf(ValidationException.class);
    }
    
    @Test
    public void createBookWithNullTitle() {
        Book book = sampleHemBookBuilder().title(null).build();
        expectedException.expect(ValidationException.class);
        manager.createBook(book);
    }

    @Test
    public void createBookWithEmptyAuthor() {
        Book book = sampleHemBookBuilder().author("").build();
        assertThatThrownBy(() -> manager.createBook(book))
                .isInstanceOf(ValidationException.class);
    }
    
    @Test
    public void createBookWithNullAuthor() {
        Book book = sampleHemBookBuilder().author(null).build();
        expectedException.expect(ValidationException.class);
        manager.createBook(book);
    }

    @Test
    public void createBookWithNegativeYear() {
        Book book = samplePoeBookBuilder().yearOfPublication(-1).build();
        expectedException.expect(ValidationException.class);
        manager.createBook(book);
    }
    
    @Test
    public void createBookWithYearInFuture() {
        Book book = samplePoeBookBuilder().yearOfPublication(2017).build();
        expectedException.expect(ValidationException.class);
        manager.createBook(book);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNullBook() {
        manager.updateBook(null);
    }

    @Test
    public void updateBookWithNullId() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setId(null);
        expectedException.expect(IllegalEntityException.class);
        manager.updateBook(book);
    }

    @Test
    public void updateBookWithNonExistingId() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setId(book.getId() + 1);
        expectedException.expect(IllegalEntityException.class);
        manager.updateBook(book);
    }

    @Test
    public void updateBookWithEmptyTitle() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setTitle("");
        expectedException.expect(ValidationException.class);
        manager.updateBook(book);
    }
    
    @Test
    public void updateBookWithNullTitle() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setTitle(null);
        expectedException.expect(ValidationException.class);
        manager.updateBook(book);
    }

    @Test
    public void updateBookWithEmptyAuthor() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setAuthor("");
        expectedException.expect(ValidationException.class);
        manager.updateBook(book);
    }
    
    @Test
    public void updateBookWithNullAuthor() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setAuthor(null);
        expectedException.expect(ValidationException.class);
        manager.updateBook(book);
    }
    
    @Test
    public void updateBookWithNegativeYearOfPublication() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setYearOfPublication(-1);
        expectedException.expect(ValidationException.class);
        manager.updateBook(book);
    }
    
    @Test
    public void updateBookWithYearOfPublicationInFuture() {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        book.setYearOfPublication(2017);
        expectedException.expect(ValidationException.class);
        manager.updateBook(book);
    }

    @Test
    public void deleteBook() {

        Book book1 = sampleHemBookBuilder().build();
        Book book2 = samplePoeBookBuilder().build();
        manager.createBook(book1);
        manager.createBook(book2);

        assertThat(manager.getBook(book1.getId())).isNotNull();
        assertThat(manager.getBook(book2.getId())).isNotNull();

        manager.deleteBook(book1);

        assertThat(manager.getBook(book1.getId())).isNull();
        assertThat(manager.getBook(book2.getId())).isNotNull();

    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNullBook() {
        manager.deleteBook(null);
    }

    @Test
    public void deleteBookWithNullId() {
        Book book = sampleHemBookBuilder().build();
        book.setId(null);
        expectedException.expect(IllegalEntityException.class);
        manager.deleteBook(book);
    }

    @Test
    public void deleteBookWithNonExistingId() {
        Book book = sampleHemBookBuilder().build();
        book.setId(1L);
        expectedException.expect(IllegalEntityException.class);
        manager.deleteBook(book);
    }

    
    @FunctionalInterface
    private static interface Operation<T> {
        void callOn(T subjectOfOperation);
    }
    
    private void testExpectedServiceFailureException(Operation<BookManager> operation) throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);
        assertThatThrownBy(() -> operation.callOn(manager))
                .isInstanceOf(ServiceFailureException.class)
                .hasCause(sqlException);
    }
    
    @Test
    public void createBookWithSqlExceptionThrown() throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);

        Book book = sampleHemBookBuilder().build();

        assertThatThrownBy(() -> manager.createBook(book))
                .isInstanceOf(ServiceFailureException.class)
                .hasCause(sqlException);
    }

    @Test
    public void updateBookWithSqlExceptionThrown() throws SQLException {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        testExpectedServiceFailureException((bookManager) -> bookManager.updateBook(book));
    }

    @Test
    public void getBookWithSqlExceptionThrown() throws SQLException {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        testExpectedServiceFailureException((bookManager) -> bookManager.getBook(book.getId()));
    }

    @Test
    public void deleteBookWithSqlExceptionThrown() throws SQLException {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        testExpectedServiceFailureException((bookManager) -> bookManager.deleteBook(book));
    }

    @Test
    public void findAllBooksWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException((bookManager) -> bookManager.findAllBooks());
    }
    
    @Test
    public void findBooksByTitleWithSqlExceptionThrown() throws SQLException {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        testExpectedServiceFailureException((bookManager) -> 
                bookManager.findBooksByTitle(book.getTitle()));
    }
    
    @Test
    public void findBooksByAuthorWithSqlExceptionThrown() throws SQLException {
        Book book = sampleHemBookBuilder().build();
        manager.createBook(book);
        testExpectedServiceFailureException((bookManager) -> 
                bookManager.findBooksByAuthor(book.getAuthor()));
    }
}
