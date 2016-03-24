package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.*;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.*;

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
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("CREATE TABLE CLIENT ("
                    + "id bigint primary key generated always as identity,"
                    + "name var(20),"
                    + "surname var(20))").executeUpdate();
        }
        manager = new ClientManagerImpl(dataSource);        
    }
    
    @After
    public void tearDown() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("DROP TABLE GRAVE").executeUpdate();
        }
    }
    
    private ClientBuilder sampleFemaleClientBuilder() {
        return new ClientBuilder()
                .id(null)
                .name("Jana")
                .surname("Nováková");
    }
    
    private ClientBuilder sampleMaleClientBuilder() {
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
        Client client = sampleFemaleClientBuilder().build();
        manager.createClient(client);
        
        Long clientId = client.getId();
        assertThat(clientId).isNotNull();
        
        assertThat(manager.findPersonById(clientId))
                .isNotSameAs(client)
                .isEqualToComparingFieldByField(client);
    }
    
    @Test
    public void createClientWithExistingId() {
        Client client = sampleFemaleClientBuilder().id(1L).build();
        expectedException.expect(IllegalEntityException.class);
        manager.createClient(client);
    }
    
    /* We may use assertThatThrownBy()
        Client client = sampleFemaleClientBuilder().name("").build();
        assertThatThrownBy(() -> manager.createClient(client))
                .isInstanceOf(ValidationException.class);
    */
    @Test
    public void createClientWithEmptyName() {
        Client client = sampleFemaleClientBuilder().name("").build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }

    @Test
    public void createClientWithEmptySurame() {
        Client client = sampleFemaleClientBuilder().surname("").build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }
    
    @Test
    public void createClientWithNullName() {
        Client client = sampleFemaleClientBuilder().name(null).build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }
    
    @Test
    public void createClientWithNullSurname() {
        Client client = sampleFemaleClientBuilder().surname(null).build();
        expectedException.expect(ValidationException.class);
        manager.createClient(client);
    }
    
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:gravemgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
}
