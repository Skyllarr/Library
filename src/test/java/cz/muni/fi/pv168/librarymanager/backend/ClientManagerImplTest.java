package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.*;

// add more tests of ClientManager implementation

/**
 * 
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class ClientManagerImplTest {
    
    private ClientManagerImpl manager;
    private DataSource dataSource;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        DBUtils.executeSqlScript(dataSource,ClientManager.class.getResource("createTables.sql"));
        manager = new ClientManagerImpl();
        manager.setDataSource(dataSource);      
    }
    
    @After
    public void tearDown() throws SQLException {
        // Drop tables after each test
        DBUtils.executeSqlScript(dataSource,ClientManager.class.getResource("dropTables.sql"));
    }
    
    private ClientBuilder sampleClientBuilder() {
        return new ClientBuilder()
                .id(null)
                .name("Filip")
                .surname("Březňák");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithNull() throws Exception {
        manager.createClient(null);
    }
    
    @Test
    public void createClient() {
        Client client = sampleClientBuilder().build();
        manager.createClient(client);
        
        Long clientId = client.getId();
        assertThat(clientId).isNotNull();
        
        assertThat(manager.findClientById(clientId))
                .isNotSameAs(client)
                .isEqualToComparingFieldByField(client);
    }
    
    @Test
    public void createClientWithExistingId() {
        Client client = sampleClientBuilder().id(1L).build();
        expectedException.expect(IllegalArgumentException.class);
        manager.createClient(client);
    }

    @Test
    public void createClientWithEmptyName() {
        Client client = newClient("", "surname");
        assertThatThrownBy(() -> manager.createClient(client))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void createClientWithEmptySurame() {        
        Client client = newClient("name", "");
        assertThatThrownBy(() -> manager.createClient(client))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void createClientWithNullName() {
        Client client = newClient(null, "surname");
        assertThatThrownBy(() -> manager.createClient(client))
                .isInstanceOf(NullPointerException.class);
    }
    
    @Test
    public void createClientWithNullSurname() {
        Client client = newClient("name", null);
        assertThatThrownBy(() -> manager.createClient(client))
                .isInstanceOf(NullPointerException.class);
    }
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:librarymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
    private static Client newClient(String name, String surname) {
        Client client = new Client();
        client.setName(name);
        client.setSurname(surname);
        return client;
    }
}
