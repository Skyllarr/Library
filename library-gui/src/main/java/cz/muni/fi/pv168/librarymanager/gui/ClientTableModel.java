package cz.muni.fi.pv168.librarymanager.gui;

import cz.muni.fi.pv168.librarymanager.backend.Client;
import cz.muni.fi.pv168.librarymanager.backend.ClientManager;
import cz.muni.fi.pv168.librarymanager.backend.ClientManagerImpl;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/**
 * @author Josef Pavelec <jospavelec@gmail.com>
 */
public class ClientTableModel extends AbstractTableModel {
    
    private final List<Client> clients = new ArrayList<>();
    private final ClientManager clientManager;

    public ClientTableModel(DataSource dataSource) {
        clientManager = new ClientManagerImpl(dataSource);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                clients.addAll(clientManager.findAllClients());
                return null;
            }
        };
        worker.execute();
    }
    
    public void addClient(Client client) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                clientManager.createClient(client);
                System.out.println(client);
                return null;
            }

            @Override
            protected void done() {
                clients.add(client);
                int lastRow = clients.size() - 1;
                fireTableRowsInserted(lastRow, lastRow);
            }
        };
        worker.execute();
    }
    
    public void updateClient(Client client, int selectedRow){
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                clientManager.updateClient(client);
                return null;
            }

            @Override
            protected void done() {
                fireTableRowsUpdated(selectedRow,selectedRow);
            }
        };
        worker.execute();
    }
    
    public void deleteClient(int row){
        SwingWorker<Void,Void> worker;
        worker = new SwingWorker<Void, Void>() {
            
            @Override
            protected Void doInBackground() throws Exception {
                clientManager.deleteClient(clients.get(row));
                return null;
            }
            @Override
            protected void done() {
                clients.remove(row);
                fireTableDataChanged();
            }
        };
        worker.execute();
    }

    @Override
    public int getRowCount() {
        return clients.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Client client = clients.get(rowIndex);
        switch (columnIndex) {
            case 0: 
                return client.getName();
            case 1:
                return client.getSurname();
            default:
                throw new IndexOutOfBoundsException("Column index must be "
                        + "greater or equal to 0 and less than 2: " + columnIndex);
        }
            
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0: 
                return "First Name";
            case 1:
                return "Last Name";
            default:
                throw new IndexOutOfBoundsException("Column index must be "
                        + "greater or equal to 0 and less than 2: " + columnIndex);
        }
    }
    
    public Client getSelectedClient(int row){
        if(row>=clients.size() || row<0)
            return null;
        return clients.get(row);
    }
    

}
