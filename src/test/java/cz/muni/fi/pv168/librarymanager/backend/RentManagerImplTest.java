/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.DBUtils;
import cz.muni.fi.pv168.librarymanager.common.EntityNotFoundException;
import cz.muni.fi.pv168.librarymanager.common.IllegalEntityException;
import cz.muni.fi.pv168.librarymanager.common.ServiceFailureException;
import cz.muni.fi.pv168.librarymanager.common.ValidationException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static java.time.Month.FEBRUARY;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author diana vilkolakova
 */
public class RentManagerImplTest {
    private RentManagerImpl manager;
    private ClientManagerImpl clientManager;
    private BookManagerImpl bookManager;
    private Client client;
    private Book book;
    
    private DataSource dataSource;
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, FEBRUARY, 29, 14, 00).atZone(ZoneId.of("UTC"));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private static Clock prepareClockMock(ZonedDateTime now) {
        // We don't need to use Mockito, because java already contais
        // implementation of Clock which returns fixed time.
        return Clock.fixed(now.toInstant(), now.getZone());
    }
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        DBUtils.executeSqlScript(dataSource,RentManager.class.getResource("createTables.sql"));
        manager = new RentManagerImpl(prepareClockMock(NOW));
        manager.setDataSource(dataSource);      
        clientManager = new ClientManagerImpl();
        clientManager.setDataSource(dataSource); 
        bookManager = new BookManagerImpl();
        bookManager.setDataSource(dataSource); 
        
        client = new Client();
        client.setName("name");
        client.setSurname("surname");
        book = new Book();
        book.setAuthor("author");
        book.setTitle("title");
        book.setYearOfPublication(1997);
        bookManager.createBook(book);
        clientManager.createClient(client);
    }
    
    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(dataSource,RentManager.class.getResource("dropTables.sql"));
    }
    
    private RentBuilder sampleRentBuilder() {
        Client client = new Client();
        client.setName("name");
        client.setSurname("surname");
        Book book = new Book();
        book.setAuthor("author");
        book.setTitle("title");
        book.setYearOfPublication(1997);
        return new RentBuilder()
                .id(null)
                .startDay(LocalDate.of(1995, 2, 20))
                .endDay(LocalDate.of(1995, 4, 25))
                .client(client)
                .book(book);
    }
    
    @Test(expected = ValidationException.class)
    public void testCreateRentWithNegativeBookId() throws Exception {
        Client client = new Client();
        client.setName("name");
        client.setSurname("surname");
        Rent rent = new Rent();
        rent.setBookId(null);
        rent.setClient(client);
        manager.createRent(rent);
    }
    
    @Test(expected = ValidationException.class)
    public void testCreateRentWithNullClient() throws Exception {
        Book book = new Book();
        book.setAuthor("author");
        Rent rent = new Rent();
        rent.setBook(book);
        rent.setClient(null);
        manager.createRent(rent);
    }
    
    @Test
    public void createRent() {
        Rent rent = sampleRentBuilder().build();
        rent.setBook(book);
        rent.setClient(client);
        manager.createRent(rent);
        
        Long rentId = rent.getId();
        assertThat(rentId).isNotNull();
    }
    
    @Test
    public void createRentWithExistingId() {
        Rent rent = sampleRentBuilder().id(1L).build();
        rent.setBook(book);
        rent.setClient(client);
        manager.createRent(rent);
        expectedException.expect(IllegalEntityException.class);
        manager.createRent(rent);
    }

    @Test(expected = NullPointerException.class)
    public void updateNullRent() {
        manager.updateRent(null);
    }

    @Test
    public void updateRentWithNullId() {
        
        Rent rent = new Rent();
        rent.setBook(book);
        rent.setClient(client);
        rent.setStartDay(LocalDate.of(2016, 3, 29));
        rent.setEndDay(LocalDate.of(2016, 4, 20));
        manager.createRent(rent);
        book.setId(null);
        expectedException.expect(IllegalEntityException.class);
        manager.updateRent(rent);
    }

    @Test
    public void updateRentWithNonExistingId() {
        Rent rent = new Rent();
        rent.setBook(book);
        rent.setClient(client);
        rent.setStartDay(LocalDate.of(2016, 3, 29));
        rent.setEndDay(LocalDate.of(2016, 4, 20));
        manager.createRent(rent);
        rent.setId(rent.getId() + 1);
        expectedException.expect(EntityNotFoundException.class);
        manager.updateRent(rent);
    }
    
    @Test(expected = NullPointerException.class)
    public void deleteNullRent() {
        manager.deleteRent(null);
    }

    @Test
    public void deleteRentWithNullId() {
        Rent rent = new Rent();
        rent.setBook(book);
        rent.setClient(client);
        rent.setStartDay(LocalDate.of(2016, 3, 29));
        rent.setEndDay(LocalDate.of(2016, 4, 20));
        rent.setId(null);
        expectedException.expect(NullPointerException.class);
        manager.deleteRent(rent);
    }

    @Test
    public void deleteRentWithNonExistingId() {
        Rent rent = new Rent();
        rent.setBook(book);
        rent.setClient(client);
        rent.setStartDay(LocalDate.of(2016, 3, 29));
        rent.setEndDay(LocalDate.of(2016, 4, 20));
        rent.setId(1L);
        expectedException.expect(EntityNotFoundException.class);
        manager.deleteRent(rent);
    }
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
    private static Rent newRent(long clientId, long bookId, LocalDate startDay, LocalDate endDay) {
        Rent rent = new Rent();
        rent.setBookId(bookId);
        rent.setClientId(clientId);
        rent.setStartDay(LocalDate.of(2014, 12, 25));
        rent.setEndDay(LocalDate.of(2015, 1, 20));
        return rent;
    }
}
