package cz.muni.fi.pv168.librarymanager.backend;


/**
 *
 * @author Diana Vilkolakova
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 *
 */
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
        Book book = new Book();
        book.setId(id);
        book.setAuthor(author);
        book.setTitle(title);
        book.setYearOfPublication(yearOfPublication);
        return book;
    }
}
