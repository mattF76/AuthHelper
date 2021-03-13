package burp;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

public class BurpExtender implements IBurpExtender, ITab, IContextMenuFactory, IHttpListener, IMessageEditorController{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JTabbedPane tabbedPane;     // 主面板
    private JPanel usersPanel;          // 左侧用户面板
    private JSplitPane optionsPanel;        // 右侧选项面板
    private UserTableModel userTableModel = new UserTableModel();  // UserTable表的TableModel
    private ArrayList<Integer> userTableSelectedRows = new ArrayList<>();         // UserTable表被用户选中的行，可能是一行，也可能是多行
    private ArrayList<Integer> originalRequestTableSelectedRows = new ArrayList<>();         // originalRequestTable表被用户选中的行，可能是一行，也可能是多行
    private ArrayList<Integer> historyTableSelectedRows = new ArrayList<>();
    private PrintWriter stdout;     // 输出调试用
    private HistoryTableModel historyTableModel = new HistoryTableModel();
    private OriginalRequestTableModel originalRequestTableModel = new OriginalRequestTableModel();  // 存放用户发送到该插件的初始请求
    private Timer timer = new Timer();      // 运行定时任务
    private TimerTask timerTask;            // 当前定时任务
    private IHttpRequestResponse currentlyDisplayedItem;    // History表中选中行对应的请求与响应
    private IMessageEditor reqMessageEditor;
    private IMessageEditor respMessageEditor;

    //
    // implement IBurpExtender
    //
    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks) {
        // keep a reference to our callbacks object
        this.callbacks = callbacks;

        // obtain an extension helpers object
        helpers = callbacks.getHelpers();

        // set our extension name
        callbacks.setExtensionName("Auth Helper");

        stdout = new PrintWriter(callbacks.getStdout(), true);      // 输出调试信息


        // create our UI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // main tab pane
                tabbedPane = new JTabbedPane();

                optionsPanel = getOptionsPanel();
                usersPanel = getUsersPanel();

                tabbedPane.addTab("Users", usersPanel);
                tabbedPane.addTab("Persistent Session", optionsPanel);

                // customize our UI components
                callbacks.customizeUiComponent(tabbedPane);
                callbacks.customizeUiComponent(optionsPanel);
                callbacks.customizeUiComponent(usersPanel);

                // add the custom tab to Burp's UI
                callbacks.addSuiteTab(BurpExtender.this);

                // add the context menu to Burp Repeater
                callbacks.registerContextMenuFactory(BurpExtender.this);

                // register ourselves as an HTTP listener
                callbacks.registerHttpListener(BurpExtender.this);
                showBanner();
            }
        });

    }

    //
    // implement ITab
    //

    @Override
    public String getTabCaption() {
        return "Auth Helper";
    }

    @Override
    public Component getUiComponent() {
        return tabbedPane;
    }

    // 构建Options面板
    private JSplitPane getOptionsPanel() {
        // 上面板
        // 各种选项与参数设置
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        JPanel jPanel_1 = new JPanel();     // 利用嵌套的边框布局管理器
        topPanel.add(jPanel_1, BorderLayout.CENTER);
        jPanel_1.setLayout(new FlowLayout(FlowLayout.LEFT));
        // 发送请求的频率设置，用于保持cookie不失效
        JLabel frequencyLabel = new JLabel("发送请求频率设置（秒）： ");
        JTextField frequencyTextField = new JTextField("300", 8);
        // 开启或关闭cookies维持功能
        JButton runButton = new JButton("Run");
        JButton stopButton = new JButton("Stop");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int sendRequestFrequency = Integer.parseInt(frequencyTextField.getText());
                if ( sendRequestFrequency <= 0){
                    return;
                }
                // 先结束之前的定时任务
                if ( timerTask != null){
                    timerTask.cancel();
                }
                timer.purge();  //  从此计时器的任务队列中移除所有已取消的任务。
                // 创建开始新的定时任务
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        runAllRequests();
                    }
                };
                timer.schedule(timerTask, 1000, 1000 * sendRequestFrequency);    // 1s之后开始 ，每多少秒运行一次
            }
        });
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 结束定时任务
                if ( timerTask != null){
                    timerTask.cancel();
                }
                timer.purge();  //  从此计时器的任务队列中移除所有已取消的任务。
            }
        });

        jPanel_1.add(frequencyLabel);
        jPanel_1.add(frequencyTextField);
        jPanel_1.add(runButton);
        jPanel_1.add(stopButton);

        JPanel jPanel_2 = new JPanel();
        topPanel.add(jPanel_2, BorderLayout.NORTH);
        JLabel desLabel = new JLabel("说明：Session维持功能。可以从Burpsuite Proxy、Repeater工具" +
                "右键发送请求到这里。"+
                "这个插件之后会自动用不同用户的Cookie按照发送频率不停发送这些请求，以维持用户Session不失效。" +
                "最好选择后台非静态请求，测试发现有些情况使用访问静态资源的请求不能维持session。");
        // 用户发送到该插件的初始请求
        JTable originalRequestTable = new JTable(originalRequestTableModel);
        originalRequestTable.setPreferredScrollableViewportSize(originalRequestTable.getPreferredSize());
        // 设置表格固定列宽
        TableColumnModel columnModel = originalRequestTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(1200);
        originalRequestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // 给表格添加右键菜单
        JPopupMenu jPopupMenu = new JPopupMenu();
        JMenuItem delReqMenuItem = new JMenuItem("delete item");
        delReqMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<Integer> selectRows = originalRequestTableSelectedRows;
                // selectRows中行行从小到排列。假设删除0th,1th行，删除玩0th行后，原来的1th变成了0th行。
                for (int i = 0, j = 0; i < selectRows.size(); i++, j++) {
                    originalRequestTableModel.deleteORequestEntry(selectRows.get(i) - j);
                }
            }
        });
        jPopupMenu.add(delReqMenuItem);
        originalRequestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    jPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // 获取originalRequestTable选中的行
        ListSelectionModel rowSM = originalRequestTable.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // Ignore extra messages.
                if (e.getValueIsAdjusting()) return;

                originalRequestTableSelectedRows = new ArrayList<>();  // 先清空之前表格行选择情况
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                } else {
                    int minSelectionIndex = lsm.getMinSelectionIndex();
                    int maxSelectionIndex = lsm.getMaxSelectionIndex();
                    for (int i = minSelectionIndex; i <= maxSelectionIndex; i++) {
                        if (lsm.isSelectedIndex(i)) {
                            originalRequestTableSelectedRows.add(i);
                        }
                    }
                }
            }
        });

        jPanel_2.setLayout(new BorderLayout());
        jPanel_2.add(desLabel, BorderLayout.NORTH);
        jPanel_2.add(new JScrollPane(originalRequestTable), BorderLayout.SOUTH);


        // 下面板
        // 历史请求面板 - 保持cookie有效的历史请求
        JTable historyTable = new JTable(historyTableModel);
        JScrollPane historyScrollPane = new JScrollPane(historyTable);
        // 获取HistoryTable选中的行，设置请求与响应TabbedPanel
        ListSelectionModel rowSM1 = historyTable.getSelectionModel();
        rowSM1.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // Ignore extra messages.
                if (e.getValueIsAdjusting()) return;

                historyTableSelectedRows = new ArrayList<>();  // 先清空之前表格行选择情况
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                } else {
                    int minSelectionIndex = lsm.getMinSelectionIndex();
                    int maxSelectionIndex = lsm.getMaxSelectionIndex();
                    for (int i = minSelectionIndex; i <= maxSelectionIndex; i++) {
                        if (lsm.isSelectedIndex(i)) {
                            historyTableSelectedRows.add(i);
                        }
                    }

                    // 查看对应记录请求与响应详情
                    int rowIndex = minSelectionIndex;
                    HistoryEntry entry = historyTableModel.getEntry(rowIndex);
                    reqMessageEditor.setMessage(entry.getRequestResponse().getRequest(), true);
                    respMessageEditor.setMessage(entry.getRequestResponse().getResponse(), false);
                    currentlyDisplayedItem = entry.getRequestResponse();
                }
            }
        });

        // 给History表格添加右键菜单
        JPopupMenu historyTablePopupMenu = new JPopupMenu();
        JMenuItem delItemHistoryTableMenuItem = new JMenuItem("delete item");
        JMenuItem clearHistoryTableMenuItem = new JMenuItem("clear history");
        historyTablePopupMenu.add(delItemHistoryTableMenuItem);
        historyTablePopupMenu.add(clearHistoryTableMenuItem);

        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    historyTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        delItemHistoryTableMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<Integer> selectRows = historyTableSelectedRows;
                // selectRows中行行从小到排列。假设删除0th,1th行，删除玩0th行后，原来的1th变成了0th行。
                for (int i = 0, j = 0; i < selectRows.size(); i++, j++) {
                    historyTableModel.deleteEntry(selectRows.get(i) - j);
                }
            }
        });
        clearHistoryTableMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                historyTableModel.deleteAllEntry();
            }
        });


        // 请求与响应面板
        JTabbedPane reqrespTabbedPane = new JTabbedPane();
        reqMessageEditor = callbacks.createMessageEditor(BurpExtender.this, false);
        respMessageEditor = callbacks.createMessageEditor(BurpExtender.this, false);
        reqrespTabbedPane.addTab("Request", reqMessageEditor.getComponent());
        reqrespTabbedPane.addTab("Response", respMessageEditor.getComponent());


        // 把历史请求面板、请求与响应面板两个面板放在一起生成一个完整功能的面板
        JSplitPane bottomPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                historyScrollPane, reqrespTabbedPane);

        // 合并上面板与下面板为一个面板
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        return jSplitPane;
    }

    // 构建Users面板
    private JPanel getUsersPanel() {
        JPanel usersPanel = new JPanel();
        usersPanel.setLayout(new BorderLayout());
        // 用户cookie面板
        JTable userTable = new JTable(userTableModel);

        // 获取userTable选中的行
        ListSelectionModel rowSM = userTable.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // Ignore extra messages.
                if (e.getValueIsAdjusting()) return;

                userTableSelectedRows = new ArrayList<>();  // 先清空之前表格行选择情况 userTableSelectedRows = null; 用这种语法会报错
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                } else {
                    int minSelectionIndex = lsm.getMinSelectionIndex();
                    int maxSelectionIndex = lsm.getMaxSelectionIndex();
                    for (int i = minSelectionIndex; i <= maxSelectionIndex; i++) {
                        if (lsm.isSelectedIndex(i)) {
                            userTableSelectedRows.add(i);
                        }
                    }
                }
            }
        });

        // 设置用户名列的列宽
        userTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        JScrollPane userScrollPane = new JScrollPane(userTable);

        // 按钮面板
        JPanel buttons = new JPanel();
        JButton newUserBtn = new JButton("New User");
        newUserBtn.setActionCommand("newUser");         // 设置button的ID
        JButton deleteUserBtn = new JButton("Delete User");
        deleteUserBtn.setActionCommand("deleteUser");
        JButton editCookiesBtn = new JButton("Edit Cookie");
        editCookiesBtn.setActionCommand("editCookies");
        JButton clearCookiesBtn = new JButton("Clear Cookies");
        clearCookiesBtn.setActionCommand("clearCookies");
        JButton editNewHeaderBtn = new JButton("Edit New Header");
        editNewHeaderBtn.setActionCommand("editNewHeader");
        JButton clearNewHeaderBtn = new JButton("Clear New Header");
        clearNewHeaderBtn.setActionCommand("clearNewHeader");

        buttons.add(newUserBtn);
        buttons.add(deleteUserBtn);
        buttons.add(new JSeparator());
        buttons.add(editCookiesBtn);
        buttons.add(clearCookiesBtn);
        buttons.add(new JSeparator());
        buttons.add(editNewHeaderBtn);
        buttons.add(clearNewHeaderBtn);

        ButtonActionListener buttonActionListener = new ButtonActionListener();
        newUserBtn.addActionListener(buttonActionListener);
        deleteUserBtn.addActionListener(buttonActionListener);
        editCookiesBtn.addActionListener(buttonActionListener);
        clearCookiesBtn.addActionListener(buttonActionListener);
        editNewHeaderBtn.addActionListener(buttonActionListener);
        clearNewHeaderBtn.addActionListener(buttonActionListener);
        // Users面板上部分
        JSplitPane topPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                userScrollPane, buttons);

//        JPanel jPanel = new JPanel();   // 暂时放这里放，没什么用
//        JSplitPane usersPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
//                topPanel, jPanel);

        usersPanel.add(topPanel, BorderLayout.NORTH);
        return usersPanel;
    }

    //
    // 实现IContextMenuFactory接口，用于创建Repeater中右键菜单
    //
    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        ArrayList<JMenuItem> menus = new ArrayList<>();

        // 在History、Repeater中显示右键菜单
        if (IBurpExtenderCallbacks.TOOL_REPEATER == invocation.getToolFlag() || IBurpExtenderCallbacks.TOOL_PROXY == invocation.getToolFlag()) {
            JMenu menu = new JMenu("Auth Helper");
            menus.add(menu);

            // 添加右键菜单 - 发送请求至插件
            // todo 接收请求
            JMenuItem requestMenuItem = new JMenuItem("Send Request(s) to Auth Helper");
            requestMenuItem.setActionCommand("request");
            requestMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    IHttpRequestResponse[] oldMessages = invocation.getSelectedMessages();  // the objects returned from this method are tied to the originating context of the messages within the Burp UI
                    ArrayList<IHttpRequestResponse> messages = new ArrayList<>();
                    for(IHttpRequestResponse oldMessage : oldMessages){
                        messages.add(callbacks.saveBuffersToTempFiles(oldMessage));
                    }

                    for (IHttpRequestResponse message : messages) {
                        IRequestInfo iRequestInfo = helpers.analyzeRequest(message);
                        String method = iRequestInfo.getMethod();
                        URL url = iRequestInfo.getUrl();
                        originalRequestTableModel.addORequestEntry(new ORequestEntry(message, method, url.toString()));
                    }
                }
            });
            menu.add(requestMenuItem);

            // 如果是在 Proxy History中，不显示切换用户功能
            if ( IContextMenuInvocation.CONTEXT_PROXY_HISTORY == invocation.getInvocationContext() ||
                    IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST == invocation.getInvocationContext() ||
                    IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE == invocation.getInvocationContext()){
                return menus;
            }
            // 添加右键菜单 - 切换请求用户
            for (int i = 0; i < userTableModel.getRowCount(); i++) {
                UserEntry userEntry = userTableModel.getUserEntry(i);
                JMenuItem menuItem = new JMenuItem("Switch to " + userEntry.getName());
                menuItem.setActionCommand(String.valueOf(i));   // 绑定子菜单与行索引
//            menuItem.addActionListener(new MenuItemActionListener());
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 获取用户选中的菜单，要使用那个用户的菜单
                        int rowIndex = Integer.parseInt(e.getActionCommand());
                        UserEntry userEntry = userTableModel.getUserEntry(rowIndex);
                        IHttpRequestResponse selectedMessage = invocation.getSelectedMessages()[0]; // 获取选中的消息

                        byte[] newRequest = changeRequest(selectedMessage, userEntry);
                        selectedMessage.setRequest(newRequest);
                    }
                });
                menu.add(menuItem);
            }
        } else {
            return menus;
        }
        return menus;
    }

    //
    // 实现IHttpListener
    //
    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        // do nothing here right now
    }

    //
    // 实现IMessageEditorController
    //
    @Override
    public IHttpService getHttpService() {
        return currentlyDisplayedItem.getHttpService();
    }

    @Override
    public byte[] getRequest() {
        return currentlyDisplayedItem.getRequest();
    }

    @Override
    public byte[] getResponse() {
        return currentlyDisplayedItem.getResponse();
    }

    //
    // Actions on Button Clicks
    //
    private class ButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            ArrayList<Integer> selectRows = userTableSelectedRows;  // 先保存选中的行，避免刷新事件被清除掉

            switch (actionCommand) {
                case "newUser":     // 点击了newUser按钮
                    String newUser = JOptionPane.showInputDialog(usersPanel, "Enter New User: ");
                    if (!"".equals(newUser)) {
                        UserEntry userEntry = new UserEntry(newUser);
                        userTableModel.addUserEntry(userEntry);
                    }
                    break;
                case "deleteUser":  // 点了deleteUser按钮
                    // selectRows中行行从小到排列。删除0th,1th行，删除玩0th行后，原来的1th变成了0th行。
                    for (int i = 0, j = 0; i < selectRows.size(); i++, j++) {
                        userTableModel.deleteUserEntry(selectRows.get(i) - j);
                    }
                    break;
                case "editCookies":
                    if (selectRows.size() == 1) {    // 选中一行
                        Integer rowIndex = selectRows.get(0);
                        UserEntry userEntry = userTableModel.getUserEntry(rowIndex);
                        String cookies = textAreaDialog("Input Cookies: ", userEntry.getCookies());
                        // 如果cookies不是"Cookie: "开头，添加这个头
                        if (!cookies.startsWith("Cookie: ")){
                            userEntry.setCookies("Cookie: " + cookies);
                        } else {
                            userEntry.setCookies(cookies);
                        }
                        userTableModel.setUserEntry(rowIndex, userEntry);
                    } else {
                    }

                    break;
                case "clearCookies":
                    for (int rowIndex : selectRows) {
                        UserEntry userEntry = userTableModel.getUserEntry(rowIndex);
                        userEntry.setCookies("");
                        userTableModel.setUserEntry(rowIndex, userEntry);
                    }
                    break;
                case "editNewHeader":
                    if (selectRows.size() == 1) {    // 选中一行
                        Integer rowIndex = selectRows.get(0);
                        UserEntry userEntry = userTableModel.getUserEntry(rowIndex);
                        String header = textAreaDialog("Input new header: ", userEntry.getHeader());
                        userEntry.setHeader(header);
                        userTableModel.setUserEntry(rowIndex, userEntry);
                    } else {
//                        stdout.println("编辑new header，选中了多行!");
                    }
                    break;
                case "clearNewHeader":
                    for (int rowIndex : selectRows) {
                        UserEntry userEntry = userTableModel.getUserEntry(rowIndex);
                        userEntry.setHeader("");
                        userTableModel.setUserEntry(rowIndex, userEntry);
                    }
                    break;
                default:
                    // to do
                    break;
            }

        }
    }


    //
    // 点击按钮，弹出文本输入框
    //
    // @param textvalue，编辑框默认值
    private String textAreaDialog(String title, String textvalue) {
        JTextArea textArea = new JTextArea(textvalue);
        textArea.setColumns(100);
        textArea.setRows(8);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        int result = JOptionPane.showConfirmDialog(usersPanel, new JScrollPane(textArea), title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            return textArea.getText();
        } else {
            return textvalue;
        }
    }

    //
    // 改变请求的cookies，或者其他特殊身份认证头
    //
    private byte[] changeRequest(IHttpRequestResponse httpRequestResponse, UserEntry entry){
        IRequestInfo iRequestInfo = helpers.analyzeRequest(httpRequestResponse); // 解析请求信息结构
        String request = new String(httpRequestResponse.getRequest());
        byte[] body = request.substring(iRequestInfo.getBodyOffset()).getBytes();
        List<String> headers = iRequestInfo.getHeaders();

        // 替换请求中cookies
        int index = -1;     // cookie头在headers中的位置
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header.startsWith("Cookie: ")) {
                index = i;
            }
        }
        // 如果请求中不存在Cookies，添加cookies头
        if (index == -1){
            headers.add(entry.getCookies());
        } else {
            // 替换headers中Cookie，Cookie在headers中位置保持不变
            headers.remove(index);
            headers.add(index, entry.getCookies());
        }

        // 如果new header不为空，替换请求中new header
        int indexOf = entry.getHeader().indexOf(":");
        if (indexOf > 0){
            int index2 = -1;    // newHeader头在headers中的位置
            String newHeader = entry.getHeader().substring(0, indexOf);     // 如"Auth: xxxxx"请求头中的 "Auth"

            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                if ( header.startsWith(newHeader)){
                    index2 = i;
                }
            }

            // 如果请求中不存在new header，添加new header
            if (index2 == -1){
                headers.add(entry.getHeader());
            } else {
                // 替换headers中new header，new header在headers中位置保持不变
                headers.remove(index2);
                headers.add(index2, entry.getHeader());
            }
        }

        // 重新组装请求
        byte[] newRequest = helpers.buildHttpMessage(headers, body);
        return newRequest;
    }

    //
    // 发送请求. 对originalRequestTable中的每个url，使用不同用户cookies，发送这这些请求
    //
    private void runAllRequests(){
        // 一定要有异常处理机制
        try{
            ArrayList<IHttpRequestResponse> originalMessages = originalRequestTableModel.getOriginalMessages();
            for(IHttpRequestResponse httpRequestResponse: originalMessages){
                runRequests(httpRequestResponse);
            }
        } catch (Throwable e){
            PrintWriter writer = new PrintWriter(callbacks.getStderr());
            writer.println(e.getMessage());
            e.printStackTrace(writer);
        }

    }

    //
    // 对originalRequestTable中的某个url，使用不同用户cookies，发送这这些请求
    //
    private void runRequests(IHttpRequestResponse originallHttpRequestResponse){
        try{
            String user = "";

            List<UserEntry> userEntries = userTableModel.getAllEntries();
            for ( UserEntry userEntry: userEntries){
                byte[] newRequest = changeRequest(originallHttpRequestResponse, userEntry);

                IHttpRequestResponse newHttpRequestResponse = callbacks.makeHttpRequest(
                        originallHttpRequestResponse.getHttpService(),
                        newRequest);
                user = userEntry.getName();
                addRequestRecord(newHttpRequestResponse, user);  // 给History表添加一行请求记录
            }
        } catch (Throwable e){
            PrintWriter writer = new PrintWriter(callbacks.getStderr());
            writer.println(e.getMessage());
            e.printStackTrace(writer);
        }
    }

    //
    // 给History表添加一行请求记录
    //
    private void addRequestRecord(IHttpRequestResponse httpRequestResponse, String user){
        IHttpRequestResponsePersisted requestResponsePersisted = callbacks.saveBuffersToTempFiles(httpRequestResponse);
        IRequestInfo requestInfo = helpers.analyzeRequest(httpRequestResponse);
        URL url = requestInfo.getUrl();
        String method = requestInfo.getMethod();
        byte[] response = httpRequestResponse.getResponse();
        int length = response.length;
        IResponseInfo responseInfo = helpers.analyzeResponse(response);
        short statusCode = responseInfo.getStatusCode();

        HistoryEntry historyEntry = new HistoryEntry(requestResponsePersisted, url, method, user, statusCode, length);
        historyTableModel.addEntry(historyEntry);
    }

    private void showBanner(){
        String bannerInfo = "github: https://github.com/mattF76/AuthHelper\n";
        stdout.println(bannerInfo);
    }
}
