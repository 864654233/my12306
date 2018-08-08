package cn.com.test.my12306.my12306.core.util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *参考：http://noobjava.iteye.com/blog/739785
 * @author abc
 */
public class TipWindow extends JDialog implements Runnable {

    private static Dimension dim;
    private int x,  y;
    private int width,  height;
    private static Insets screenInsets;


    public TipWindow(int width,int height){
        dim = Toolkit.getDefaultToolkit().getScreenSize();
        screenInsets   =   Toolkit.getDefaultToolkit().getScreenInsets(this.getGraphicsConfiguration());
        this.width=width;
        this.height=height;
        x = (int) (dim.getWidth() - width-3);
        y = (int) (dim.getHeight()-screenInsets.bottom-3);
        initComponents();
    }
    /*
     * 开启渐入效果
     * 开启后3秒，窗口自动渐出
     * 若不需要渐出，注释掉，sleep(3000)和close()方法
     */
    public void run() {
        for (int i = 0; i <= height; i += 10) {
            try {
                this.setLocation(x, y - i);
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(TipWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        close();
    }
    private void initComponents() {
        this.setSize(width, height);
        this.setLocation(x, y);
        this.setBackground(Color.black);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }
    public void close(){
        for (int i = 0; i <= height; i += 10) {
            try {
                setLocation(x, y-height+i);
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(TipWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        dispose();
    }
}
