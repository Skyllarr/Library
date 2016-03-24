package cz.muni.fi.pv168.bookmanager.backend;

import java.util.List;


/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public interface RentManager {
    
    /**
     * Method create new rent of a book for client
     * @param client client who rent a book
     * @param book book what will be rent
     */
    public void createRent(Client client, Book book);
    
    /**
     * Method change existing rent (eg. extend of rent time)
     * @param client client
     * @param book book
     */
    public void updateRent(Client client, Book book);
    
    /**
     * Method delete existing rent
     * @param client client
     * @param book book
     */
    public void deleteRent(Client client, Book book);
    
    /**
     * Method find all rent what has delayed return
     * @return delayed returns as list of Rent
     */
    public List<Rent> findDelayedReturns();
    
    /**
     * Method find client who has rent specific book
     * @param book book to find
     * @return client who has rent input book
     */
    public Client findClientByRentBook(Book book);
    
    /**
     * Method find all rent books for specific client
     * @param client input client
     * @return rent books as list of Book
     */
    public List<Book> findRentBooksByClient(Client client);
           
    
}
