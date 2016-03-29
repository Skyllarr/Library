package cz.muni.fi.pv168.librarymanager.backend;

import java.time.LocalDate;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class Rent {
    
    private Long id;
    private LocalDate startDay;
    private LocalDate endDay;
    private Client client;
    private Book book;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDate getStartDay() {
        return startDay;
    }

    public void setStartDay(LocalDate startDay) {
        this.startDay = startDay;
    }

    public LocalDate getEndDay() {
        return endDay;
    }

    public void setEndDay(LocalDate endDay) {
        this.endDay = endDay;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }
    

}
