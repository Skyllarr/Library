package cz.muni.fi.pv168.librarymanager.backend;

import java.time.LocalDate;
import java.time.Month;

/**
 *
 * @author Diana Vilkolakova
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 *
 */
public class RentBuilder {
    private Long id;
    private LocalDate startDay;
    private LocalDate endDay;
    private Client client;
    private Book book;
    
    public RentBuilder id(Long id) {
        this.id = id;
        return this;
    }
    
    public RentBuilder startDay(int year, Month month, int day) {
        this.startDay = LocalDate.of(year, month, day);
        return this;
    }
    
    public RentBuilder endDay(int year, Month month, int day) {
        this.endDay = LocalDate.of(year, month, day);
        return this;
    }
    
    public RentBuilder surname(LocalDate endDay) {
        this.endDay = endDay;
        return this;
    }
    
    public RentBuilder client(Client client) {
        this.client = client;
        return this;
    }
    
    public RentBuilder book(Book book) {
        this.book = book;
        return this;
    }
    
    
    public Rent build() {
        Rent rent = new Rent();
        rent.setId(id);
        rent.setClient(client);
        rent.setBook(book);
        rent.setStartDay(startDay);
        rent.setEndDay(endDay);
        return rent;
    }
}
