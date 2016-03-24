package cz.muni.fi.pv168.librarymanager.backend;

import java.util.List;
import javax.sql.DataSource;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class ClientManagerImpl implements ClientManager {
    
    private final DataSource dataSource;

    public ClientManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
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
