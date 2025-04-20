package com.example;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class AutoClicker implements NativeKeyListener {
    private Robot robot;
    private volatile boolean isRunning = false;     // 连点状态标志
    private volatile boolean hotkeyEnabled = false; // 热键启用标志
    private JButton button;
    private int clickCount;
    private int interval;

    public AutoClicker() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "无法创建Robot对象：" + e.getMessage());
            System.exit(1);
        }
        createGUI();
        registerHotKeys();
    }

    private void createGUI() {
        JFrame frame = new JFrame("简单连点器");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel countPanel = new JPanel();
        countPanel.add(new JLabel("连点次数"));
        JTextField countTextField = new JTextField("100", 8);
        countPanel.add(countTextField);

        JPanel intervalPanel = new JPanel();
        intervalPanel.add(new JLabel("连点间隔(毫秒)"));
        JTextField intervalTextField = new JTextField("100", 8);
        intervalPanel.add(intervalTextField);

        JPanel showPanel = new JPanel();
        showPanel.add(new JLabel("按下F6启动连点，按下F7停止连点"));

        button = new JButton("保存并准备");
        button.addActionListener((ActionEvent e) -> {
            button.setText("已保存");
            clickCount = Integer.parseInt(countTextField.getText());
            interval = Integer.parseInt(intervalTextField.getText());
            hotkeyEnabled = true;
        });

        panel.add(countPanel);
        panel.add(intervalPanel);
        panel.add(showPanel);
        panel.add(button);

        frame.add(panel);
        frame.setSize(300, 180);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }

    private void registerHotKeys() {
        try {
            GlobalScreen.registerNativeHook();
            Logger.getLogger(GlobalScreen.class.getName()).setLevel(Level.OFF);
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException ex) {
            Logger.getLogger(AutoClicker.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!hotkeyEnabled){
            return; 
        }

        if (e.getKeyCode() == NativeKeyEvent.VC_F6) {
            if (!isRunning) {
                isRunning = true;
                SwingUtilities.invokeLater(() -> button.setText("点击中"));
                new Thread(new Clicker(), "AutoClicker-Thread").start();
            }
        } else if (e.getKeyCode() == NativeKeyEvent.VC_F7) {
            if (isRunning) {
                isRunning = false;
                SwingUtilities.invokeLater(() -> button.setText("已保存"));
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) { }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) { }

    private class Clicker implements Runnable {
        @Override
        public void run() {
            int clickCompleted = 0;
            if (clickCount != 0) {
                while (isRunning && clickCompleted < clickCount) {
                    robot.mousePress(java.awt.event.MouseEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(java.awt.event.MouseEvent.BUTTON1_DOWN_MASK);
                    clickCompleted++;
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        isRunning = false;
                        break;
                    }
                }
                if (clickCompleted >= clickCount) {
                    isRunning = false;
                    SwingUtilities.invokeLater(() -> button.setText("已保存"));
                }
            } else {
                while (isRunning) {
                    robot.mousePress(java.awt.event.MouseEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(java.awt.event.MouseEvent.BUTTON1_DOWN_MASK);
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        isRunning = false;
                        break;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AutoClicker();
        });
    }
}