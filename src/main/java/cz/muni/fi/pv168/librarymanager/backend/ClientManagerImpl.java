package cz.muni.fi.pv168.librarymanager.backend;

import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class ClientManagerImpl implements ClientManager {
    
    private static final Logger logger = Logger.getLogger(
            BookManagerImpl.class.getName());
    
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }
    

    @Override
    public void createClient(Client client) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateClient(Client client) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteClient(Client client) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Client> findAllPerson() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Client findPersonById(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
