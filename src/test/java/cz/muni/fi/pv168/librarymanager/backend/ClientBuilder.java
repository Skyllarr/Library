package cz.muni.fi.pv168.librarymanager.backend;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class ClientBuilder {
    
    private Long id;
    private String name;
    private String surname;
    
    public ClientBuilder id(Long id) {
        this.id = id;
        return this;
    }
    
    public ClientBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public ClientBuilder surname(String surname) {
        this.surname = surname;
        return this;
    }
    
    public Client build() {
        Client client = new Client();
        client.setId(id);
        client.setName(name);
        client.setSurname(surname);
        return client;
    }
    
}
