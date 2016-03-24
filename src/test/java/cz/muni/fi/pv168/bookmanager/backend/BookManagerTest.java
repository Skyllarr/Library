package cz.muni.fi.pv168.bookmanager.backend;


import java.util.List;
import cz.muni.fi.pv168.bookmanager.backend.Book;
import cz.muni.fi.pv168.bookmanager.backend.BookManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Diana Vilkolakova
 */
public class BookManagerTest {

    private Book bookSalinger;
    private Book bookBlissCarman;
    private BookManager bookManager;
    private List<Book> foundBooks;

    @Before
    public void setUp() {
        bookSalinger = new Book();
        String author;
        String title;
        title = "Franny and Zooey";
        author = "J. D. Salinger";
        int year = 1961;
        bookSalinger.setAuthor(author);
        bookSalinger.setTitle(title);
        bookSalinger.setYear(year);
        bookBlissCarman = new Book();
        bookBlissCarman = new Book();
        bookBlissCarman.setAuthor("Bliss Carman");
        bookBlissCarman.setTitle("Sappho: One Hundred Lyrics");
        bookBlissCarman.setYear(2001);
        bookManager.createBook(bookSalinger);
        bookManager.createBook(bookBlissCarman);
    }

    @Test
    public void findBookByAuthorTest() {
        foundBooks = bookManager.findBooksByAuthor("J. D. Salinger");
        Assert.assertNotNull(foundBooks);
        Assert.assertTrue(foundBooks.contains(bookSalinger));
        Assert.assertEquals(foundBooks.size(), 1);
    }

    @Test
    public void findAllTest() {
        foundBooks = bookManager.findAllBooks();
        Assert.assertNotNull(foundBooks);
        Assert.assertTrue(foundBooks.contains(bookBlissCarman));
    }

    @Test
    public void updateTest() {
        bookSalinger.setTitle("Franny");
        bookManager.updateBook(bookSalinger);
        foundBooks = bookManager.findBooksByTitle("Franny");
        Assert.assertNotNull(foundBooks);
        Assert.assertEquals(foundBooks.size(), 1);
        Assert.assertTrue(foundBooks.get(0).getYear() == 1961);
        Assert.assertTrue(foundBooks.get(0).getAuthor().contentEquals("J. D. Salinger"));
        foundBooks = bookManager.findBooksByTitle("Franny and Zooey");
        Assert.assertNull(foundBooks);
    }
}
