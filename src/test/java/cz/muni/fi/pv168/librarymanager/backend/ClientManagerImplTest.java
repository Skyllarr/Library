package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * 
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 * @author Diana Vilkolakova
 */
public class ClientManagerImplTest {
    
    private ClientManagerImpl manager;
    private DataSource dataSource;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        DBUtils.executeSqlScript(dataSource,ClientManager.class.getResource("createTables.sql"));
        manager = new ClientManagerImpl();
        manager.setDataSource(dataSource);      
    }
    
    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(dataSource,ClientManager.class.getResource("dropTables.sql"));
    }
    
    private ClientBuilder sampleJaneClientBuilder() {
        return new ClientBuilder()
                .id(null)
                .name("Jane")
                .surname("Blažková");
    }
    
    private ClientBuilder samplePhilipClientBuilder() {
        return new ClientBuilder()
                .id(null)
                .name("Philip")
                .surname("Březňák");
    }
        
    @Test
    public void createClient() {
        Client client = samplePhilipClientBuilder().build();
        manager.createClient(client);
        
        Long clientId = client.getId();
        assertThat(clientId).isNotNull();
        
        assertThat(manager.getClient(clientId))
                .isNotSameAs(client)
                .isEqualToComparingFieldByField(client);
    }
    
    @Test
    public void createNullClient() {
        expectedException.expect(IllegalArgumentException.class);
        manager.createClient(null);
    }
    
    @Test
    public void createClientWithExistingId() {
        Client client = samplePhilipClientBuilder().id(1L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.createClient(client);
    }

    @Test
    public void createClientWithEmptyName() {
        Client client = sampleJaneClientBuilder().name("")
                                                   .build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }

    @Test
    public void createClientWithEmptySurname() {        
        Client client = sampleJaneClientBuilder().surname("")
                                                   .build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }
    
    @Test
    public void createClientWithNullName() {
        Client client = sampleJaneClientBuilder().name(null)
                                                   .build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }
    
    @Test
    public void createClientWithNullSurname() {
        Client client = sampleJaneClientBuilder().surname(null)
                                                   .build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }
    
    @Test
    public void updateClientName() {
        Client clientForUpdate = samplePhilipClientBuilder().build();
        Client anotherClient = sampleJaneClientBuilder().build();
        manager.createClient(clientForUpdate);
        manager.createClient(anotherClient);

        clientForUpdate.setName("New Name");

        manager.updateClient(clientForUpdate);

        assertThat(manager.getClient(clientForUpdate.getId()))
                .isEqualToComparingFieldByField(clientForUpdate);
        assertThat(manager.getClient(anotherClient.getId()))
                .isEqualToComparingFieldByField(anotherClient);
    }
    
    @Test
    public void updateClientSurname() {
        Client clientForUpdate = samplePhilipClientBuilder().build();
        Client anotherClient = sampleJaneClientBuilder().build();
        manager.createClient(clientForUpdate);
        manager.createClient(anotherClient);

        clientForUpdate.setSurname("New Surname");

        manager.updateClient(clientForUpdate);

        assertThat(manager.getClient(clientForUpdate.getId()))
                .isEqualToComparingFieldByField(clientForUpdate);
        assertThat(manager.getClient(anotherClient.getId()))
                .isEqualToComparingFieldByField(anotherClient);
    }
            
    @Test(expected = IllegalArgumentException.class)
    public void updateNullClient() {
        manager.updateClient(null);
    }
    
    @Test
    public void upadateClientNullId() {
        Client client = sampleJaneClientBuilder().id(null).build();
        expectedException.expect(IllegalEntityException.class);
        manager.updateClient(client);
    }
    
    @Test
    public void upadateClientNullName() {
        Client client = sampleJaneClientBuilder().name(null).build();
        expectedException.expect(ValidationException.class);
        manager.updateClient(client);
    }
    
    @Test
    public void upadateClientNullSurname() {
        Client client = sampleJaneClientBuilder().surname(null).build();
        expectedException.expect(ValidationException.class);
        manager.updateClient(client);
    }
    
    @Test
    public void updateNonExistingClient() {
        Client client = sampleJaneClientBuilder().id(100L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.updateClient(client);
    }
    
    @Test
    public void updateClientWithExistingId() {
        Client client = sampleJaneClientBuilder().id(1L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.updateClient(client);
    }
    
    @Test
    public void deleteClient() {
        Client client = sampleJaneClientBuilder().build();
        Client clientToDelete = samplePhilipClientBuilder().build();
        
        manager.createClient(clientToDelete);
        manager.createClient(client);
        
        assertThat(manager.getClient(clientToDelete.getId())).isNotNull();
        assertThat(manager.getClient(client.getId())).isNotNull();
        
        manager.deleteClient(clientToDelete);
        
        assertThat(manager.getClient(client.getId())).isNotNull();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void deleteNullClient() {
        manager.deleteClient(null);
    }
    
    @Test
    public void deleteNonExistingClient() {
        Client client = samplePhilipClientBuilder().id(1L).build();
        expectedException.expect(EntityNotFoundException.class);
        manager.deleteClient(client);
    }
    
    @Test
    public void findAllClients() {
        assertThat(manager.findAllClients()).isEmpty();
        
        Client clientFemale = sampleJaneClientBuilder().build();
        Client clientMale = samplePhilipClientBuilder().build();
        
        manager.createClient(clientFemale);
        manager.createClient(clientMale);
        
        assertThat(manager.findAllClients())
                .usingFieldByFieldElementComparator()
                .containsOnly(clientFemale, clientMale);
    }
    
    @Test
    public void findCLientsByName() {
        Client client = samplePhilipClientBuilder().name("Jan").build();
        assertThat(manager.findClientsByName(client.getName())).isEmpty();
        
        manager.createClient(client);
        
        assertThat(manager.findClientsByName(client.getName()))
                .usingFieldByFieldElementComparator()
                .containsOnly(client);
    }
    
    @Test
    public void findCLientsBySurname() {
        Client client = samplePhilipClientBuilder().surname("Krakonoš").build();
        assertThat(manager.findClientsBySurname(client.getSurname())).isEmpty();
        
        manager.createClient(client);
        
        assertThat(manager.findClientsBySurname(client.getSurname()))
                .usingFieldByFieldElementComparator()
                .containsOnly(client);
    }
    
    @FunctionalInterface
    private static interface Operation<T> {
        void callOn(T subjectOfOperation);
    }
    
    private void testExpectedServiceFailureException(Operation<ClientManager> operation) throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);
        assertThatThrownBy(() -> operation.callOn(manager))
                .isInstanceOf(ServiceFailureException.class)
                .hasCause(sqlException);
    }

    @Test
    public void updateClientWithSqlExceptionThrown() throws SQLException {
        Client client = sampleJaneClientBuilder().build();
        manager.createClient(client);
        testExpectedServiceFailureException((clientManager) -> clientManager.updateClient(client));
    }

    @Test
    public void getClientWithSqlExceptionThrown() throws SQLException {
        Client client = sampleJaneClientBuilder().build();
        manager.createClient(client);
        testExpectedServiceFailureException((clientManager) -> clientManager.getClient(client.getId()));
    }

    @Test
    public void deleteClientWithSqlExceptionThrown() throws SQLException {
        Client client = sampleJaneClientBuilder().build();
        manager.createClient(client);
        testExpectedServiceFailureException((clientManager) -> clientManager.deleteClient(client));
    }

    @Test
    public void findAllBodiesWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException((clientManager) -> clientManager.findAllClients());
    }
    
    
    
}
