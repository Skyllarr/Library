package library;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class Book {
    
    private Long id;
    private String title;    
    private String author;
    private int year;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setYear(int year) {
        this.year = year;
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

    public int getYear() {
        return year;
    }

}
