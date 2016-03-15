/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.List;
import library.Book;
import library.BookManager;
import library.Client;
import library.ClientManager;
import library.RentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author Diana Vilkolakova
 */
public class RentManagerTest {

    private Book bookSalinger;
    private Book bookBlissCarman;
    private Client client;
    private BookManager bookManager;
    private ClientManager clientManager;
    private RentManager rentManager;

    @BeforeMethod
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
        client = new Client();
        clientManager.createClient(client);
        rentManager.createRent(client, bookSalinger);
        rentManager.createRent(client, bookBlissCarman);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void cannotRentRentedBookTest() {
        Client client2 = new Client();
        rentManager.createRent(client2, bookSalinger);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void cannotDeleteRentedBookTest() {
        bookManager.deleteBook(bookSalinger);
    }

    @Test
    public void findRentedBooksByClientTest() {
        List<Book> foundBooks;
        foundBooks = rentManager.findRentBooksByClient(client);
        Assert.assertNotNull(foundBooks);
        Assert.assertTrue(foundBooks.contains(bookBlissCarman) && foundBooks.contains(bookSalinger));
        Assert.assertEquals(foundBooks.size(), 2);
    }

    @Test
    public void findClientByRentedBookTest() {
        Client foundClient = rentManager.findClientByRentBook(bookSalinger);
        Assert.assertNotNull(foundClient);
        Assert.assertEquals(foundClient, client);
    }
}
