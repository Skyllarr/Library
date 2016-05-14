package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import static java.time.Month.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 *
 * @author Diana Vilkolakova
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 *
 */
public class RentManagerImplTest {
    private RentManagerImpl manager;
    private ClientManagerImpl clientManager;
    private BookManagerImpl bookManager;
    
    private DataSource dataSource;
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, APRIL, 7, 20, 00).atZone(ZoneId.of("UTC"));
    private final static ZonedDateTime NOW_PLUS_2_MONTHS
            = LocalDateTime.of(2016, JUNE, 7, 20, 00).atZone(ZoneId.of("UTC"));

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
        bookManager = new BookManagerImpl(prepareClockMock(NOW));
        bookManager.setDataSource(dataSource);
        prepareTestData();        
    }
    
    private Book bookSea, bookMorella, bookWild, bookSvejk, bookWithNullId, bookNotInDB;
    private Client clientBruce, clientSteve, clientDave, clientWithNullId, clientNotInDB;
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
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
        assertThat(manager.getRent(rentBruceId))
                .isEqualToComparingFieldByField(rentBruce)
                .isNotSameAs(rentBruce);
                
                
                
        Rent expectedRent = manager.getRent(rentBruceId);
        assertThat(expectedRent).isNotSameAs(rentBruce);
        assertThat(expectedRent.getId()).isEqualTo(rentBruce.getId());
        assertThat(expectedRent.getClient())
                .isEqualToComparingFieldByField(rentBruce.getClient());
        assertThat(expectedRent.getBook())
                .isEqualToComparingFieldByField(rentBruce.getBook());
        assertThat(expectedRent.getStartDay()).isEqualTo(rentBruce.getStartDay());
        assertThat(expectedRent.getEndDay()).isEqualTo(rentBruce.getEndDay());

        assertThat(manager.findAllRents())
                .isNotEmpty()
                .usingFieldByFieldElementComparator()
                .containsOnly(expectedRent); 
        
        assertThat(manager.findClientByRentBook(bookSvejk))
                .isNull();
        assertThat(manager.findRentBooksByClient(clientDave))
                .isEmpty();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void createNullRent() {
        manager.createRent(null);
    }
    
    @Test(expected = IllegalEntityException.class)
    public void createRentWithExistingId() {
        manager.createRent(sampleBruceRentsSea().id(1L).build());
    }
    
    @Test(expected = ValidationException.class)
    public void createRentWithNullClient() {
        manager.createRent(sampleSteveRentsMorella().client(null).build());
    }
    
    @Test(expected = ValidationException.class)
    public void createRentWithNullBook() {
        manager.createRent(sampleBruceRentsSea().book(null).build());
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
                        .endDay(2016, APRIL, 19)
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
    
    @Test(expected = ValidationException.class)
    public void updateRentWithNullClient() {
        manager.updateRent(sampleSteveRentsMorella().client(null).build());
    }
    
    @Test(expected = ValidationException.class)
    public void updateRentWithNullBook() {
        manager.updateRent(sampleBruceRentsSea().book(null).build());
    }
    
    @Test
    public void deleteRent() {
        Rent steveRent = sampleSteveRentsMorella().build();
        Rent bruceRent = sampleBruceRentsSea().build();
        manager.createRent(bruceRent);
        manager.createRent(steveRent);
        
        manager.deleteRent(steveRent);
        
        assertThat(manager.findAllRents())
                .isNotEmpty()
                .containsOnly(bruceRent);
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
                        .isEqualToComparingFieldByField(bruceRent.getClient())
                        .isNotNull();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void findClientByNullBook() {
        manager.findClientByRentBook(null);
    }
    
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
                    .containsOnly(bruceRentsSea, bruceRentsSvejk, steveRentsMorella, steveRentsWild);
    }
    
    @Test
    public void findDelayed() {
        Rent bruceRent = sampleBruceRentsSea().build();
        manager.createRent(bruceRent);
        manager.setClock(prepareClockMock(NOW_PLUS_2_MONTHS));
        
        assertThat(manager.findDelayedReturns())
                .contains(bruceRent);
        
    }
    
    @FunctionalInterface
    private static interface Operation<T> {
        void callOn(T subjectOfOperation);
    }
    
    private void testExpectedServiceFailureException(Operation<RentManager> operation) throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);
        assertThatThrownBy(() -> operation.callOn(manager))
                .isInstanceOf(ServiceFailureException.class)
                .hasCause(sqlException);
    }
    
    @Test
    public void createRentWithSqlExceptionThrown() throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);

        Rent rent = sampleBruceRentsSea().build();

        assertThatThrownBy(() -> manager.createRent(rent))
                .isInstanceOf(ServiceFailureException.class)
                .hasCause(sqlException);
    }

    @Test
    public void updateRentWithSqlExceptionThrown() throws SQLException {
        Rent rent = sampleBruceRentsSea().build();
        manager.createRent(rent);
        testExpectedServiceFailureException((rentManager) -> rentManager.updateRent(rent));
    }

    @Test
    public void getRentWithSqlExceptionThrown() throws SQLException {
        Rent rent = sampleBruceRentsSea().build();
        manager.createRent(rent);
        testExpectedServiceFailureException((rentManager) -> rentManager.getRent(rent.getId()));
    }

    @Test
    public void deleteRentWithSqlExceptionThrown() throws SQLException {
        Rent rent = sampleBruceRentsSea().build();
        manager.createRent(rent);
        testExpectedServiceFailureException((rentManager) -> rentManager.deleteRent(rent));
    }

    @Test
    public void findAllRentsWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException((rentManager) -> rentManager.findAllRents());
    }
    
    @Test
    public void findClientByBookWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException((rentManager) -> 
                rentManager.findClientByRentBook(bookSea));
    }
    
    @Test
    public void findBooksByClientWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException((rentManager) -> 
                rentManager.findRentBooksByClient(clientDave));
    }
    
    @Test
    public void findDelayedRentsWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException((rentManager) -> 
                rentManager.findDelayedReturns());
    }
            
    
    
    
   
}
