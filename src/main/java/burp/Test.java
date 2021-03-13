package burp;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Test {
    public static void main(String[] args) {
//        headers();
        test4();
    }
    public static void test4(){
        final int flag = 5;
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            int count = 0;
            @Override
            public void run() {
                System.out.println(System.currentTimeMillis() + "发送请求" + count);
                count += 1;
                if ( count > flag){
                    cancel();       //取消当前 TimerTask
                    timer.purge(); // 终止此计时器
                    System.out.println("定时器已终止");
                }
            }
        };
        timer.schedule(task, 1000, 1000 * 5);    // 1s之后开始 ，每10秒运行一次
        System.out.println("定时任务成功执行");
    }

    public static void test3(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(System.currentTimeMillis() + "发送请求");
            }
        }, 1000, 1000 * 5);    // 1s之后开始 ，每10秒运行一次
    }

    public static void test1() {
        ArrayList<String> strings = new ArrayList<String>();
        strings.add("123");
        strings.add("abc");
        strings.add("sdfsd");
        strings.remove(0);
        System.out.println(strings.get(0));
        System.out.println(strings.size());
    }

    public static void test2() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                display2();
            }
        });
    }

    private static void display() {
        String[] items = {"One", "Two", "Three", "Four", "Five"};
        JComboBox<String> combo = new JComboBox<>(items);
        JTextField field1 = new JTextField("1234.56");
        JTextField field2 = new JTextField("9876.54");
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(combo);
        panel.add(new JLabel("Field 1:"));
        panel.add(field1);
        panel.add(new JLabel("Field 2:"));
        panel.add(field2);
        int result = JOptionPane.showConfirmDialog(null, panel, "Test",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            System.out.println(combo.getSelectedItem()
                    + " " + field1.getText()
                    + " " + field2.getText());
        } else {
            System.out.println("Cancelled");
        }
    }

    private static void display2() {
        JTextArea textArea = new JTextArea();
        textArea.setColumns(100);
        textArea.setRows(8);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        int result = JOptionPane.showConfirmDialog(null, textArea, "Input Cookies", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            System.out.println(textArea.getText());
        } else {
            System.out.println("Cancelled");
        }
    }

    private static void headers() {
        ArrayList<String> headers = new ArrayList<>();
        headers.add("Accept: image/webp,*/*");
        headers.add("Host: entry.baidu.com");
        headers.add("Cookie: BIDUPSID=152625BF89BA62570053222B9903D44A; BAIDUID=C487AE4A86630563BCDF02E708C9ED53:FG=1; PSTM=1612946654; __yjs_duid=1_0f5ae821a9ae84a412b88d6abdc842281612958416638; BDORZ=FFFB88E999055A3F8A630C64834BD6D0; MCITY=-289%3A");
        headers.add("Referer: https://home.firefoxchina.cn/");

        String newCookies = "Cookie: admin";
        int index = -1;
        for(int i=0; i< headers.size(); i++){
            String s = headers.get(i);
            if(s.startsWith("Cookie:")){
                index = i;
            }
        }
        headers.remove(index);
        headers.add(index, newCookies);

        // 打印
        for(String header : headers){
            System.out.println(header);
        }
    }


}
