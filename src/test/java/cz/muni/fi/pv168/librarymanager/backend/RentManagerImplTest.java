package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import static java.time.Month.*;
import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * @author diana vilkolakova
 */
public class RentManagerImplTest {
    private RentManagerImpl manager;
    private ClientManagerImpl clientManager;
    private BookManagerImpl bookManager;
    
    private DataSource dataSource;
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, APRIL, 7, 20, 00).atZone(ZoneId.of("UTC"));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private static Clock prepareClockMock(ZonedDateTime now) {
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
        prepareTestData();        
    }
    
    private Book bookSea, bookMorella, bookWild, bookSvejk, bookWithNullId, bookNotInDB;
    private Client clientBruce, clientSteve, clientDave, clientWithNullId, clientNotInDB;
    
    private void prepareTestData() {
        clientBruce = new ClientBuilder().id(null).name("Bruce").surname("Dickinson").build();
        clientSteve = new ClientBuilder().id(null).name("Steve").surname("Harris").build();
        clientDave = new ClientBuilder().id(null).name("Dave").surname("Murray").build();
        
        bookSea = new BookBuilder().id(null).title("The Old Man and the Sea").author("Ernest Hemingway").yearOfPublication(1952).build();
        bookMorella = new BookBuilder().id(null).title("Morella").author("Edgar Allan Poe").yearOfPublication(1835).build();
        bookWild = new BookBuilder().id(null).title("Into the Wild").author("Jon Krauker").yearOfPublication(1996).build();
        bookSvejk = new BookBuilder().id(null).title("The Good Soldier Švejk").author("Jaroslav Hašek").yearOfPublication(1985).build();
        
        clientManager.createClient(clientBruce);
        clientManager.createClient(clientSteve);
        clientManager.createClient(clientDave);
        
        bookManager.createBook(bookMorella);
        bookManager.createBook(bookSea);
        bookManager.createBook(bookWild);
        bookManager.createBook(bookSvejk);
        
        bookWithNullId = new BookBuilder().id(null).title("Book with null id").author("Lexa").build();
        bookNotInDB = new BookBuilder().id(bookMorella.getId()+100).title("Not in DB").author("Fantom").build();
        assertThat(bookManager.getBook(bookNotInDB.getId())).isNull();
        
        clientWithNullId = new ClientBuilder().id(null).name("Client with null id").surname("Lexa").build();
        clientWithNullId = new ClientBuilder().id(clientBruce.getId()+100).name("Not in DB").surname("Fantom").build();
//        assertThat(clientManager.getClient(clientNotInDB.getId())).isNull();
        
    }
    
    private RentBuilder sampleBruceRentsSea() {
        return new RentBuilder().id(null)
                                .client(clientBruce)
                                .book(bookSea)
                                .startDay(2016, APRIL, 7)
                                .endDay(2016, MAY, 7);
    }
    
    private RentBuilder sampleSteveRentsMorella() {
        return new RentBuilder().id(null)
                                .client(clientSteve)
                                .book(bookMorella)
                                .startDay(2016, APRIL, 10)
                                .endDay(2016, APRIL, 28);
    }
    
    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(dataSource,RentManager.class.getResource("dropTables.sql"));
    }
    
    @Test
    public void createRent() {
        
        assertThat(manager.findAllRents()).isEmpty();
        
        Rent rentBruce = sampleBruceRentsSea().build();
        manager.createRent(rentBruce);
        
        Long rentBruceId = rentBruce.getId();
        assertThat(rentBruceId).isNotNull();
        
        /* ??? isEqualToComparingFieldByField method doesn't allow "deep comparsion"?
        assertThat(manager.getRent(rentBruceId))
                .isNotSameAs(rentBruce);
                .isEqualToComparingFieldByField(rentBruce);*/
                
                
        Rent expectedRent = manager.getRent(rentBruceId);
        assertThat(expectedRent).isNotSameAs(rentBruce);
        assertThat(expectedRent.getId()).isEqualTo(expectedRent.getId());
        assertThat(expectedRent.getClient())
                .isEqualToComparingFieldByField(rentBruce.getClient());
        assertThat(expectedRent.getBook())
                .isEqualToComparingFieldByField(rentBruce.getBook());
        assertThat(expectedRent.getStartDay()).isEqualTo(rentBruce.getStartDay());
        assertThat(expectedRent.getEndDay()).isEqualTo(rentBruce.getEndDay());

        /* similar problem as above
        assertThat(manager.findAllRents())
                .isNotEmpty()
                .usingFieldByFieldElementComparator()
                .containsOnly(expectedRent); */
        
        assertThat(manager.findAllRents()).hasSize(1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void createNullRent() {
        manager.createRent(null);
    }
    
    @Test(expected = IllegalEntityException.class)
    public void createRentWithExistingId() {
        manager.createRent(sampleBruceRentsSea().id(1L).build());
    }
    
    @Test
    public void createRentInPast() {
        Rent rent = sampleBruceRentsSea()
                        .startDay(2016, MARCH, 7)
                        .endDay(2016, APRIL, 6)
                        .build();
        expectedException.expect(ValidationException.class);
        manager.createRent(rent);
    }
    
    @Test
    public void createRentWithEndDayBeforeStartDay() {
        Rent rent = sampleBruceRentsSea()
                        .startDay(2016, APRIL, 20)
                        .endDay(2016, APRIL, 8)
                        .build();
        expectedException.expect(ValidationException.class);
        manager.createRent(rent);
    }
    
    @Test
    public void createRentWithStartDayInPast() {
        Rent rent = sampleBruceRentsSea()
                        .startDay(2016, APRIL, 6)
                        .build();
        expectedException.expect(ValidationException.class);
        manager.createRent(rent);
    }
    
    @Test
    public void createRentWithEndDayInPast() {
        Rent rent = sampleBruceRentsSea()
                        .endDay(2016, APRIL, 6)
                        .build();
        expectedException.expect(ValidationException.class);
        manager.createRent(rent);
    }
    
    @Test
    public void createRentWithAlreadyRentedBook() {
        Rent goodRent = sampleBruceRentsSea().build();
        manager.createRent(goodRent);
        Rent rentWithRentedBook = sampleSteveRentsMorella()
                                    .book(goodRent.getBook())
                                    .build();
                
        expectedException.expect(IllegalEntityException.class);
        manager.createRent(rentWithRentedBook);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void updateNullRent() {
        manager.updateRent(null);
    }

    @Test(expected = IllegalEntityException.class)
    public void updateRentWithNullId() {
        manager.updateRent(sampleBruceRentsSea().id(null).build());
    }

    @Test
    public void updateRentWithNonExistingId() {
        Rent rent = sampleBruceRentsSea().id(1L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.updateRent(rent);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void deleteNullRent() {
        manager.deleteRent(null);
    }
    
    @Test(expected = IllegalEntityException.class)
    public void deleteRentWithNullId() {
        manager.deleteRent(sampleBruceRentsSea().id(null).build());
    }

    @Test
    public void deleteRentWithNonExistingId() {
        Rent rent = sampleBruceRentsSea().id(1L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.deleteRent(rent);
    }
    
    @Test
    public void findClientByRentBook() {
        Rent bruceRent = sampleBruceRentsSea().build();
        manager.createRent(bruceRent);
        manager.createRent(sampleSteveRentsMorella().build());
        
        Client foundClient = manager.findClientByRentBook(bruceRent.getBook());
        assertThat(foundClient)
                        .isEqualToComparingFieldByField(bruceRent.getClient());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void findClientByNullBook() {
        manager.findClientByRentBook(null);
    }
    
    /* ??? When we cannot find client expect an exception or null 
    @Test(expected = IllegalArgumentException.class)
    public void findClientByNonExistingBook() {
        manager.findClientByRentBook(bookNotInDB);*/
    
    @Test
    public void findClientByNonExistingBook() {
        assertThat(manager.findClientByRentBook(bookNotInDB)).isNull();
    }
    
    @Test
    public void findClientByNonRentedBook() {
        manager.createRent(sampleBruceRentsSea().build());
        assertThat(manager.findClientByRentBook(bookMorella)).isNull();
    }
    
    @Test
    public void findBooksByClient() {
        assertThat(manager.findRentBooksByClient(clientBruce)).isEmpty();
        Rent bruceRentsSea = sampleBruceRentsSea().build();
        Rent bruceRentsSvejk = sampleBruceRentsSea().book(bookSvejk).build();
        manager.createRent(bruceRentsSea);
        manager.createRent(bruceRentsSvejk);
        manager.createRent(sampleSteveRentsMorella().build());
        
        assertThat(manager.findRentBooksByClient(clientBruce))
                                .isNotEmpty()
                                .containsOnly(bookSea, bookSvejk);
    }
    
    @Test
    public void findAllRents() {
        assertThat(manager.findAllRents()).isEmpty();
        Rent bruceRentsSea = sampleBruceRentsSea().build();
        Rent bruceRentsSvejk = sampleBruceRentsSea().book(bookSvejk).build();
        Rent steveRentsMorella = sampleSteveRentsMorella().build();
        Rent steveRentsWild = sampleSteveRentsMorella().book(bookWild).build();
        manager.createRent(bruceRentsSea);
        manager.createRent(bruceRentsSvejk);
        manager.createRent(steveRentsMorella);
        manager.createRent(steveRentsWild);
        
        assertThat(manager.findAllRents())
                    .isNotEmpty()
                    /* ??? Why it doesn't work
                    .containsOnly(bruceRentsSea, bruceRentsSvejk, steveRentsMorella, steveRentsWild);
                    */
                    .hasSize(4);
    }
            
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
   
}
