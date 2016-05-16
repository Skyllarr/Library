package cz.muni.fi.pv168.librarymanager.gui;

import cz.muni.fi.pv168.librarymanager.backend.Client;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * @author Josef Pavelec <jospavelec@gmail.com>
 */
public class ClientWindow extends javax.swing.JPanel {
    
    private String action;
    private Client client;
    private ResourceBundle bundle;
    private JTable table;

    /** Creates new form ClientAddWindow */
    public ClientWindow(String action,Client client,ResourceBundle bundle,
            JTable table) {
        this.action = action;
        this.client = client;
        this.bundle = bundle;
        this.table = table;
        initComponents();
        switch(action){
            case "add":
                jLabelClientWindow.setText("Add a new client");
                //jLabelClientWindow.setText(texts.getString("addCustomer"));
                break;
            case "edit":
                jLabelClientWindow.setText("Edit selected client");
                //jLabel1.setText(texts.getString("editCustomer"));
                setTextsFromClients(client);
                break;
            /*case "filter": 
                jLabel1.setText(texts.getString("filterCustomers"));
                setTextsFromCustomer(customer);
                break;                */
        }
    }
    
    private void setTextsFromClients(Client client) {
        jTextFieldName.setText(client.getName());
        jTextFieldSurname.setText(client.getSurname());
    }
    
    private void addClient() {
        client = new Client();
        List<String> incorrectInputs = checkInputs();
        if(!incorrectInputs.isEmpty()){
            JOptionPane.showMessageDialog(null, "Fill up all fields correctly" +": "+
                            incorrectInputs.toString(),"Incorrect input",
                    JOptionPane.DEFAULT_OPTION);
            return;
        }
        
        fillUpClientFromTextInput();
        
        ((ClientTableModel)table.getModel()).addClient(client);
        Window win = SwingUtilities.getWindowAncestor(this);
        win.dispose(); 
    }
    
    
    private void editClient() {
        List<String> incorrectInputs = checkInputs();
        if(!incorrectInputs.isEmpty()){
            JOptionPane.showMessageDialog(null, "Fill up all fields correctly" +": "+
                            incorrectInputs.toString(),"Incorrect input",
                    JOptionPane.DEFAULT_OPTION);
            return;
        }
        
        fillUpClientFromTextInput();
        int selectedRow = table.getSelectedRow();
        ((ClientTableModel)table.getModel()).updateClient(client, selectedRow);
        Window win = SwingUtilities.getWindowAncestor(this);
        win.dispose(); 
        
    }
    
    private List<String> checkInputs() {
        List<String> incorrectInputs = new ArrayList<>();
        if (jTextFieldName.getText().isEmpty()) {
            incorrectInputs.add(jLabelName.getText());
        }
        if (jTextFieldSurname.getText().isEmpty()) {
            incorrectInputs.add(jLabelSurname.getText());
        }
        return incorrectInputs;
        
    }
    
    private void fillUpClientFromTextInput() {
        client.setName(jTextFieldName.getText());
        client.setSurname(jTextFieldSurname.getText());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabelClientWindow = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jTextFieldSurname = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabelName = new javax.swing.JLabel();
        jLabelSurname = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();

        jLabelClientWindow.setText("Add a new client");

        jTextFieldName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldNameActionPerformed(evt);
            }
        });

        jButton1.setText("OK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabelName.setText("Name");

        jLabelSurname.setText("Surname");

        jButton2.setText("Cancel");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(57, 57, 57)
                            .addComponent(jLabelClientWindow))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(22, 22, 22)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabelName)
                                .addComponent(jLabelSurname))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextFieldSurname, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                                .addComponent(jTextFieldName)))))
                .addContainerGap(14, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelClientWindow)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldSurname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelSurname))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        switch(action){
            case "add":
                addClient();
                break;
            case "edit":
                editClient();
                break;
//            case "filter": 
  //              filterCustomer();
    //            break;
        }
        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextFieldNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldNameActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        Window win = SwingUtilities.getWindowAncestor(this);
        win.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabelClientWindow;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JLabel jLabelSurname;
    private javax.swing.JTextField jTextFieldName;
    private javax.swing.JTextField jTextFieldSurname;
    // End of variables declaration//GEN-END:variables

}
