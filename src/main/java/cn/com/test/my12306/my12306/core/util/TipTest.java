package cn.com.test.my12306.my12306.core.util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
/*
 * java气泡提示效果
 * @author noobjava
 * @version 1.0
 * @since JDK1.6(建议)
 *
 */

public class TipTest extends Thread{
    private Map<String,String> feaMap=null;

    public TipTest(){
        feaMap=new HashMap<String,String>();
        feaMap.put("name", "noobjava气泡提醒");
        feaMap.put("release", "2010-08-20 11:33:00");
        feaMap.put("feature", "1.含动画渐入与渐出效果\n2.3秒后启动动画渐出效果");
        super.start();
    }
    public TipTest(Map<String,String> feaMap){
        this.feaMap=feaMap;
//        feaMap.put("name", "noobjava气泡提醒");
//        feaMap.put("release", "2010-08-20 11:33:00");
//        feaMap.put("feature", "1.含动画渐入与渐出效果\n2.3秒后启动动画渐出效果");
        super.start();
    }
    public TipTest(String name,String time,String content){
        feaMap=new HashMap<String,String>();
        feaMap.put("name", name);
        feaMap.put("release", time);
        feaMap.put("feature", content);
        super.start();
    }
    public void run(){
        final TipWindow tw=new TipWindow(300,220);
        tw.setTitle("my12306提示框");
        JPanel headPan=new JPanel();
        JPanel feaPan=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel btnPan=new JPanel();
        JButton update=new JButton("确定");

//        feaPan.setBorder(BorderFactory.createMatteBorder(1, 2, 3, 0, Color.gray));
        JLabel head=new JLabel(feaMap.get("name")+",含以下功能");
        head.setPreferredSize(new Dimension(250,30));
        head.setForeground(Color.black);
        JTextArea feature=new JTextArea(feaMap.get("feature"));
        feature.setEditable(false);
        feature.setForeground(Color.red);
        feature.setFont(new Font("宋体",Font.PLAIN,13));
//  feature.setBackground(Color.ORANGE);

        feature.setPreferredSize(new Dimension(280,60));

        JScrollPane jfeaPan=new JScrollPane(feature);
        jfeaPan.setPreferredSize(new Dimension(283,80));
//        jfeaPan.setBorder(null);
        jfeaPan.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.gray));

        JLabel releaseLabel=new JLabel("发布日期"+feaMap.get("release"));
        releaseLabel.setForeground(Color.gray);


        feaPan.add(jfeaPan);
        feaPan.add(releaseLabel);
        headPan.add(head);
        btnPan.add(update);
        tw.add(headPan,BorderLayout.NORTH);
        tw.add(feaPan,BorderLayout.CENTER);
        tw.add(btnPan,BorderLayout.SOUTH);

        update.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                JOptionPane.showMessageDialog(tw, "点这里干吗？点那个XX关掉嘛");
            }
        });
        tw.setAlwaysOnTop(true);
        tw.setResizable(false);
        tw.setVisible(true);
        tw.run();
    }

    public static void main(String args[]){
       /* Map<String,String> feaMap = new HashMap<String,String>();
        feaMap.put("name", "标题");
        feaMap.put("release", "2010-08-20 11:33:00");
        feaMap.put("feature", "XXX");
//        new TipTest();
        new TipTest(feaMap);*/
        new TipTest("名称","","内容");
    }


}
