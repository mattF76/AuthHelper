package burp;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class UserTableModel extends AbstractTableModel {
    // 用来提供表格中每行数据
    private final List<UserEntry> usersList = new ArrayList<UserEntry>();
//    private List<UserEntry> usersList = new ArrayList<UserEntry>(){{
//        add(new UserEntry("admin"));
//        add(new UserEntry("user1"));
//        add(new UserEntry("user2"));
//        add(new UserEntry("anonymous"));
//    }};

    @Override
    public int getRowCount() {
        return usersList.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        UserEntry userEntry = usersList.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return userEntry.getName();
            case 1:
                return userEntry.getCookies();
            case 2:
                return userEntry.getHeader();
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "User Name";
            case 1:
                return "Cookies";
            case 2:
                return "HTTP Header";
            default:
                return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if ( columnIndex == 0){         // 第1列不可以修改
            return false;
        }
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        UserEntry userEntry;
        String value = (String) aValue;
        userEntry = usersList.get(rowIndex);

        switch (columnIndex) {
            case 0:
                userEntry.setName(value);
                break;
            case 1:
                userEntry.setCookies(value);
                break;
            case 2:
                userEntry.setHeader(value);
                break;
            default:
                System.out.println("Something wrong in: Function setValueAt of Class UserTableModel ");  // 没有任何作用这一行
        }

        // Notifies all listeners that the value of the cell at [row, column] has been updated.
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public void addUserEntry(UserEntry userEntry) {
        usersList.add(userEntry);
        // Notifies all listeners that The number of rows may also have changed and the <code>JTable</code> should redraw the table from scratch.
        fireTableDataChanged();
    }

    public void deleteUserEntry(int rowIndex) {
        if (rowIndex < usersList.size()) {
            usersList.remove(rowIndex);
        }
        // Notifies all listeners that The number of rows may also have changed and the <code>JTable</code> should redraw the table from scratch.
        fireTableDataChanged();
//        fireTableRowsDeleted(0,usersList.size());       // 未测试
    }

    public UserEntry getUserEntry(int rowIndex){
        return usersList.get(rowIndex);
    }

    public void setUserEntry(int rowIndex, UserEntry userEntry){
        usersList.set(rowIndex, userEntry);
        fireTableDataChanged();
    }

    public List<UserEntry> getAllEntries(){
        return usersList;
    }
}
