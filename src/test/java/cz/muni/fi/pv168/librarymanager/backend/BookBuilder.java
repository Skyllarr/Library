/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author skylar
 */
package cz.muni.fi.pv168.librarymanager.backend;

public class BookBuilder {
    private Long id;
    private String author;
    private String title;
    private int yearOfPublication;

    public BookBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public BookBuilder author(String author) {
        this.author = author;
        return this;
    }

    public BookBuilder title(String title) {
        this.title = title;
        return this;
    }

    public BookBuilder yearOfPublication(int yearOfPublication) {
        this.yearOfPublication = yearOfPublication;
        return this;
    }

    public Book build() {
        Book grave = new Book();
        grave.setId(id);
        grave.setAuthor(author);
        grave.setTitle(title);
        grave.setYearOfPublication(yearOfPublication);
        return grave;
    }
}
