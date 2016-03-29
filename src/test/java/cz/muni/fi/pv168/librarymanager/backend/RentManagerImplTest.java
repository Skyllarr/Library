/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.DBUtils;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author skylar
 */
public class RentManagerImplTest {
    private RentManagerImpl manager;
    private DataSource dataSource;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        DBUtils.executeSqlScript(dataSource,RentManager.class.getResource("createTables.sql"));
        manager = new RentManagerImpl();
        manager.setDataSource(dataSource);      
    }
    
    @After
    public void tearDown() throws SQLException {
        // Drop tables after each test
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
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRentWithNullBook() throws Exception {
        Client client = new Client();
        client.setName("name");
        client.setSurname("surname");
        manager.createRent(client, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRentWithNullClient() throws Exception {
        Book book = new Book();
        book.setAuthor("author");
        book.setTitle("title");
        book.setYearOfPublication(1997);
        manager.createRent(null, book);
    }
    
    @Test
    public void createRent() {
        Client client = new Client();
        client.setName("name");
        client.setSurname("surname");
        Book book = new Book();
        book.setAuthor("author");
        book.setTitle("title");
        book.setYearOfPublication(1997);
        Rent rent = sampleRentBuilder().build();
        manager.createRent(client, book);
        
        Long rentId = rent.getId();
        assertThat(rentId).isNotNull();
        
        assertThat(manager.findRentById(rentId))
                .isNotSameAs(rent)
                .isEqualToComparingFieldByField(rent);
    }
    
    @Test
    public void createRentWithExistingId() {
        Client client = new Client();
        client.setName("name");
        client.setSurname("surname");
        Book book = new Book();
        book.setAuthor("author");
        book.setTitle("title");
        book.setYearOfPublication(1997);
        Rent rent = sampleRentBuilder().id(1L).build();
        expectedException.expect(IllegalArgumentException.class);
        manager.createRent(client, book);
    }
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
    private static Rent newRent(String name, String surname) {
        Client client = new Client();
        client.setName("name");
        client.setSurname("surname");
        Book book = new Book();
        book.setAuthor("author");
        book.setTitle("title");
        book.setYearOfPublication(1997);
        Rent rent = new Rent();
        rent.setBook(book);
        rent.setClient(client);
        rent.setStartDay(LocalDate.of(2014, 12, 25));
        rent.setEndDay(LocalDate.of(2015, 1, 20));
        return rent;
    }
}
