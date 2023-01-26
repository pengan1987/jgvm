package eastsun.jgvm.plaf;

import eastsun.jgvm.module.GvmConfig;
import eastsun.jgvm.module.JGVM;
import eastsun.jgvm.module.LavApp;
import eastsun.jgvm.module.io.DefaultFileModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;

/**
 * @version Aug 13, 2008
 * @author Eastsun
 */
public class MainFrame extends JFrame {

    JGVM gvm;
    ScreenPane screenPane;
    KeyBoard keyBoard;
    Worker worker;
    JFileChooser fileChooser;
    JLabel msgLabel;

    public MainFrame() {
        super("GVmakerSE");
        keyBoard = new KeyBoard();
        gvm = JGVM.newGVM(new GvmConfig(), new DefaultFileModel(new FileSysSE("GVM_ROOT")), keyBoard.getKeyModel());
        screenPane = new ScreenPane(gvm);
        fileChooser = new JFileChooser("/");
        fileChooser.addChoosableFileFilter(new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".lav");
                }
            }

            public String getDescription() {
                return "GVmaker Application";
            }
        });

        msgLabel = new JLabel(" Stop");
        add(screenPane, BorderLayout.NORTH);
        add(msgLabel, BorderLayout.CENTER);
        add(keyBoard, BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());
        pack();
        setResizable(false);
        openDefaultLav();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.out.println(ex);
        }
        System.out.println(System.getProperty("java.class.path"));
        JFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setMsg(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                msgLabel.setText(" " + msg);
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent e) {
                if (worker != null && worker.isAlive()) {
                    worker.setPaused(true);
                    setMsg("Pause");
                }
            }

            public void menuDeselected(MenuEvent e) {
                if (worker != null && worker.isAlive()) {
                    worker.setPaused(false);
                    setMsg("Run");
                }
            }

            public void menuCanceled(MenuEvent e) {
                System.out.println("canzzzz");
            }
        });
        menuBar.add(file);
        JMenuItem open = new JMenuItem(new AbstractAction("Open") {

            public void actionPerformed(ActionEvent e) {
                if (worker != null && worker.isAlive()) {
                    worker.setPaused(true);
                    setMsg("Pause");
                }
                openLavFile();
            }
        });
        JMenuItem autoload = new JMenuItem(new AbstractAction("Auto Load") {

            public void actionPerformed(ActionEvent e) {
                if (worker != null && worker.isAlive()) {
                    worker.setPaused(true);
                    setMsg("Pause");
                }
                openDefaultLav();
            }
        });
       
        file.add(open);
        file.add(autoload);
        file.add(new AbstractAction("Exit") {

            public void actionPerformed(ActionEvent e) {
                if (worker != null && worker.isAlive()) {
                    worker.interrupt();
                    try {
                        worker.join();
                    } catch (InterruptedException ex) {
                        System.err.println(ex);
                    }
                }
                System.exit(0);
            }
        });
        return menuBar;
    }
    
    public static void copyDataDirectory(String sourceDirectory) {
        File gvmLavaData = new File("GVM_ROOT/LavaData/");
        if (!gvmLavaData.isDirectory()) {
            gvmLavaData.mkdirs();
        }
        File folder = new File(sourceDirectory);

        if (folder.isDirectory()) {
            File[] fileList = folder.listFiles();
            for (File myfile : fileList) {
                File copied = new File("GVM_ROOT/LavaData/" + myfile.getName());
                if (myfile.getName().endsWith(".dat")) {
                    try {
                        InputStream in = new BufferedInputStream(
                                new FileInputStream(myfile));
                        OutputStream out = new BufferedOutputStream(
                                new FileOutputStream(copied));

                        byte[] buffer = new byte[1024];
                        int lengthRead;
                        while ((lengthRead = in.read(buffer)) > 0) {
                            out.write(buffer, 0, lengthRead);
                            out.flush();
                        }
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
    }
    
    public void openDefaultLav() {
        File defaultLav = new File("/str/game.lav");
        if (defaultLav.isFile()) {
            InputStream in = null;
            try {
                String parentPath = defaultLav.getParent();
                copyDataDirectory(parentPath);
                in = new FileInputStream(defaultLav);

            } catch (FileNotFoundException ex) {
                System.err.println(ex);
            }
            LavApp lavApp = LavApp.createLavApp(in);
            if (worker != null && worker.isAlive()) {
                worker.interrupt();
                try {
                    worker.join();
                } catch (InterruptedException ex) {
                    System.out.println(ex);
                }
            }
            gvm.loadApp(lavApp);
            worker = new Worker();
            worker.start();
            setMsg("Run");
        }
    }

    private void openLavFile() {
        int res = fileChooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            InputStream in = null;
            try {
                File selectedfile = fileChooser.getSelectedFile();
                String parentPath = selectedfile.getParent();
                copyDataDirectory(parentPath);
                in = new FileInputStream(selectedfile);
            } catch (FileNotFoundException ex) {
                System.err.println(ex);
            }
            LavApp lavApp = LavApp.createLavApp(in);
            if (worker != null && worker.isAlive()) {
                worker.interrupt();
                try {
                    worker.join();
                } catch (InterruptedException ex) {
                    System.out.println(ex);
                }
            }
            gvm.loadApp(lavApp);
            worker = new Worker();
            worker.start();
            setMsg("Run");
        } else {
            if (worker != null && worker.isAlive()) {
                worker.setPaused(false);
                setMsg("Run");
            } else {
                setMsg("Stop");
            }
        }
    }

    class Worker extends Thread {

        private boolean isPaused;

        public Worker() {
            setDaemon(true);
            setPaused(false);
        }

        public void run() {
            try {
                int step = 0;
                while (!(gvm.isEnd() || isInterrupted())) {
                    while (isPaused()) {
                        synchronized (this) {
                            wait();
                        }
                    }
                    gvm.nextStep();
                    step++;
                    if (step == 100) {
                        step = 0;
                        Thread.sleep(0, 100);
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex);
            } finally {
                gvm.dispose();
                setMsg("Stop");
            }
        }

        public synchronized boolean isPaused() {
            return isPaused;
        }

        public synchronized void setPaused(boolean p) {
            if (p != isPaused) {
                isPaused = p;
                notifyAll();
            }
        }
    }
}
