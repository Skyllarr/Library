package cz.muni.fi.pv168.librarymanager.backend;

import cz.muni.fi.pv168.librarymanager.common.ServiceFailureException;
import java.util.List;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public interface ClientManager {
    
    /**
     * Method create new client
     * @param client new client
     */
    public void createClient(Client client);
    
    /**
     * Method change existing client
     * @param client client to change
     */
    public void updateClient(Client client);
    
    /**
     * Method delete existing client
     * @param client client to delete
     */
    public void deleteClient(Client client);
    
    public List<Client> findClientsBySurname(String surname);
    public List<Client> findClientsByName(String surname);
    /**
     * Method list all existing clients
     * @return all clients as list of Client
     */
    public List<Client> findAllClients();
    
    /**
     * Method find client by his id number
     * @param id number
     * @return Client or null if for input id number client doesn't exist
     */
    public Client findClientById(Long id);

}
