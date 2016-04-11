package cz.muni.fi.pv168.librarymanager.backend;

import java.util.List;


/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public interface RentManager {
    
    /**
     * Method create new rent of a book for client
     * @param rent is new rent to create
     */
    public void createRent(Rent rent);
    
    /**
     * Method change existing rent (eg. extend of rent time)
     * @param rent is rent to update
     */
    public void updateRent(Rent rent);
    
    /**
     * Method delete existing rent
     * @param rent is rent to delete
     */
    public void deleteRent(Rent rent);
    
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
      
    /**
     * Method find rent by rent id
     * @param id rent id
     * @return book with input id or null when book doesn't exist
     */
    public Rent getRent(Long id);
    
    /**
     * Method return all rents
     * @return all rents
     */
    public List<Rent> findAllRents();
}
