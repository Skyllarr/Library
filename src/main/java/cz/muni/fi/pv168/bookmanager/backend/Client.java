package cz.muni.fi.pv168.bookmanager.backend;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class Client {
    
    private Long id;
    private String name;
    private String surname;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}
