package burp;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class HistoryTableModel extends AbstractTableModel {

    private ArrayList<HistoryEntry> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "#";
            case 1:
                return "URL";
            case 2:
                return "Method";
            case 3:
                return "User";
            case 4:
                return "Http Code";
            case 5:
                return "Response Length";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        HistoryEntry entry = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return entry.getUrl().toString();
            case 2:
                return entry.getMethod();
            case 3:
                return entry.getUser();
            case 4:
                return entry.getHttpCode();
            case 5:
                return entry.getHttpSize();
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public void addEntry(HistoryEntry entry){
        rows.add(entry);
        fireTableDataChanged();
    }

    public HistoryEntry getEntry(int rowIndex){
        return rows.get(rowIndex);
    }

    public void deleteEntry(int rowIndex) {
        if (rowIndex < rows.size()) {
            rows.remove(rowIndex);
        }
        fireTableDataChanged();
    }

    public void deleteAllEntry() {
        rows = new ArrayList<>();
        fireTableDataChanged();
    }

}
