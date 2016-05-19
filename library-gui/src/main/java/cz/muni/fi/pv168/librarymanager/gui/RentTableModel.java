/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.librarymanager.gui;

import cz.muni.fi.pv168.librarymanager.backend.Rent;
import cz.muni.fi.pv168.librarymanager.backend.RentManager;
import cz.muni.fi.pv168.librarymanager.backend.RentManagerImpl;
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
public class RentTableModel extends AbstractTableModel{
    
    private final List<Rent> rents = new ArrayList<>();
    private final RentManager rentManager;
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, MARCH, 13, 22, 00).atZone(ZoneId.of("UTC"));

    public RentTableModel(DataSource dataSource) {
        rentManager = new RentManagerImpl(prepareClockMock(NOW));
        rentManager.setDataSource(dataSource);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                rents.addAll(rentManager.findAllRents());
                return null;
            }
        };
        worker.execute();
    }
    
    private static Clock prepareClockMock(ZonedDateTime now) {
        return Clock.fixed(now.toInstant(), now.getZone());
    }
    
    public void addRent(Rent rent) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                rentManager.createRent(rent);
                System.out.println(rent);
                return null;
            }

            @Override
            protected void done() {
                rents.add(rent);
                int lastRow = rents.size() - 1;
                fireTableRowsInserted(lastRow, lastRow);
            }
        };
        worker.execute();
    }
    
    public void updateRent(Rent rent, int selectedRow){
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                rentManager.updateRent(rent);
                return null;
            }

            @Override
            protected void done() {
                fireTableRowsUpdated(selectedRow,selectedRow);
            }
        };
        worker.execute();
    }
    
    public void deleteRent(int row){
        SwingWorker<Void,Void> worker;
        worker = new SwingWorker<Void, Void>() {
            
            @Override
            protected Void doInBackground() throws Exception {
                rentManager.deleteRent(rents.get(row));
                return null;
            }
            @Override
            protected void done() {
                rents.remove(row);
                fireTableDataChanged();
            }
        };
        worker.execute();
    }

    @Override
    public int getRowCount() {
        return rents.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Rent rent = rents.get(rowIndex);
        switch (columnIndex) {
            case 0: 
                return rent.getBook().getTitle();
            case 1:
                return rent.getClient().getSurname();
            case 2:
                return rent.getStartDay();
            case 3:
                return rent.getEndDay();
            default:
                throw new IndexOutOfBoundsException("Column index must be "
                        + "greater or equal to 0 and less than 4: " + columnIndex);
        }
            
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0: 
                return "Book title";
            case 1:
                return "Client last name";
            case 2:
                return "Start day";
            case 3:
                return "End day";
            default:
                throw new IndexOutOfBoundsException("Column index must be "
                        + "greater or equal to 0 and less than 4: " + columnIndex);
        }
    }
    
    public Rent getSelectedRent(int row){
        if(row>=rents.size() || row<0)
            return null;
        return rents.get(row);
    }
}
