/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.librarymanager.backend;

import java.time.LocalDate;

/**
 *
 * @author skylar
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
    
    public RentBuilder startDay(LocalDate startDay) {
        this.startDay = startDay;
        return this;
    }
    
    public RentBuilder endDay(LocalDate endDay) {
        this.endDay = endDay;
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
    
    public RentBuilder surname(Book book) {
        this.book = book;
        return this;
    }
    
    public Rent build() {
        Rent rent = new Rent();
        rent.setId(id);
        rent.setClient(client);
        rent.setStartDay(startDay);
        rent.setEndDay(endDay);
        return rent;
    }
}
