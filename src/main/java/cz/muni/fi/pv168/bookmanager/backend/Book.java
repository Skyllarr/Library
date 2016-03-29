package cz.muni.fi.pv168.bookmanager.backend;

import java.util.Objects;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class Book {
    
    private Long id;
    private String title;    
    private String author;
    private int yearofpublication;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setyearofpublication(int yearofpublication) {
        this.yearofpublication = yearofpublication;
    }

    public Long setId(Long id) {
        return this.id = id;
    }
    
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getyearofpublication() {
        return yearofpublication;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.title);
        hash = 37 * hash + Objects.hashCode(this.author);
        hash = 37 * hash + this.yearofpublication;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Book other = (Book) obj;
        if (this.yearofpublication != other.yearofpublication) {
            return false;
        }
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        if (!Objects.equals(this.author, other.author)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

}
