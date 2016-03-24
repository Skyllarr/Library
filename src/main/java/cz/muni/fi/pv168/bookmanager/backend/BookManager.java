package cz.muni.fi.pv168.bookmanager.backend;

import cz.muni.fi.pv168.bookmanager.common.ServiceFailureException;
import java.util.List;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public interface BookManager {
    
    /**
     * Method create new book
     * @param book new book
     */
    public void createBook(Book book);
    
    /**
     * Method change existing book
     * @param book book to change
     */
    public void updateBook(Book book);
    
    /**
     * Method delete existing book
     * @param book book to delete
     */
    public void deleteBook(Book book);
    
    /**
     * Method returns book by id
     * @param id book to get
     * @return book
     */
    public Book getBook(Long id) throws ServiceFailureException;
    
    /**
     * Method list all books -- whole library
     * @return all books as list of Book
     */
    public List<Book> findAllBooks();
    
    /**
     * For input author find and list all his books
     * @param author input author as String
     * @return all books of input author as list of Book
     */
    public List<Book> findBooksByAuthor(String author);
    
    /**
     * For input title find and list all books with this title
     * @param title input title as String
     * @return all books of input title as list of Book
     */
    public List<Book> findBooksByTitle(String title);

}
