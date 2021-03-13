package burp;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class OriginalRequestTableModel extends AbstractTableModel {
    private ArrayList<ORequestEntry> rows = new ArrayList<>();       // 原始的请求，用来维持Cookie用

    @Override
    public int getRowCount() {
        return 6;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= rows.size()){
            return null;
        }
        ORequestEntry oRequestEntry = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return oRequestEntry.getMethod();
            case 2:
                return oRequestEntry.getURL();
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "#";
            case 1:
                return "Method";
            case 2:
                return "URL";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    // 添加原始请求
    public void addORequestEntry(ORequestEntry entry) {
        rows.add(entry);
        // Notifies all listeners that The number of rows may also have changed and the <code>JTable</code> should redraw the table from scratch.
        fireTableDataChanged();
    }

    public void deleteORequestEntry(int rowIndex) {
        if (rowIndex < rows.size()) {
            rows.remove(rowIndex);
        }
        fireTableDataChanged();
    }

    // 返回保存再OriginalRequestTable表中的请求
    public ArrayList<IHttpRequestResponse> getOriginalMessages() {
        ArrayList<IHttpRequestResponse> httpRequestResponses = new ArrayList<>();
        for(ORequestEntry row: rows){
            httpRequestResponses.add(row.getiHttpRequestResponse());
        }
        return  httpRequestResponses;
    }

}
