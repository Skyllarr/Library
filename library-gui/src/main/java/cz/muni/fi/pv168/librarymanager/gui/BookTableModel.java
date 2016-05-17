/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.librarymanager.gui;

import cz.muni.fi.pv168.librarymanager.backend.Book;
import cz.muni.fi.pv168.librarymanager.backend.BookManager;
import cz.muni.fi.pv168.librarymanager.backend.BookManagerImpl;
import java.time.Clock;
import java.time.LocalDateTime;
import static java.time.Month.MARCH;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Skylar
 */
public class BookTableModel extends AbstractTableModel{
    
    private final List<Book> books = new ArrayList<>();
    private final BookManager bookManager;
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, MARCH, 13, 22, 00).atZone(ZoneId.of("UTC"));

    public BookTableModel(DataSource dataSource) {
        bookManager = new BookManagerImpl(prepareClockMock(NOW));
        bookManager.setDataSource(dataSource);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                books.addAll(bookManager.findAllBooks());
                return null;
            }
        };
        worker.execute();
    }
    
    private static Clock prepareClockMock(ZonedDateTime now) {
        return Clock.fixed(now.toInstant(), now.getZone());
    }
    
    public void addBook(Book book) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                bookManager.createBook(book);
                System.out.println(book);
                return null;
            }

            @Override
            protected void done() {
                books.add(book);
                int lastRow = books.size() - 1;
                fireTableRowsInserted(lastRow, lastRow);
            }
        };
        worker.execute();
    }
    
    public void updateBook(Book book, int selectedRow){
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                bookManager.updateBook(book);
                return null;
            }

            @Override
            protected void done() {
                fireTableRowsUpdated(selectedRow,selectedRow);
            }
        };
        worker.execute();
    }
    
    public void deleteBook(int row){
        SwingWorker<Void,Void> worker;
        worker = new SwingWorker<Void, Void>() {
            
            @Override
            protected Void doInBackground() throws Exception {
                bookManager.deleteBook(books.get(row));
                return null;
            }
            @Override
            protected void done() {
                books.remove(row);
                fireTableDataChanged();
            }
        };
        worker.execute();
    }

    @Override
    public int getRowCount() {
        return books.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Book book = books.get(rowIndex);
        switch (columnIndex) {
            case 0: 
                return book.getTitle();
            case 1:
                return book.getAuthor();
            case 2:
                return book.getYearOfPublication();
            default:
                throw new IndexOutOfBoundsException("Column index must be "
                        + "greater or equal to 0 and less than 3: " + columnIndex);
        }
            
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0: 
                return "Title";
            case 1:
                return "Author";
            case 2:
                return "Year of publication";
            default:
                throw new IndexOutOfBoundsException("Column index must be "
                        + "greater or equal to 0 and less than 3: " + columnIndex);
        }
    }
    
    public Book getSelectedBook(int row){
        if(row>=books.size() || row<0)
            return null;
        return books.get(row);
    }
}
