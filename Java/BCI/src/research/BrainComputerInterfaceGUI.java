package research;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 *
 * @author Abdullah Garcia - abdullah.garcia@gmail.com
 */
public class BrainComputerInterfaceGUI extends javax.swing.JFrame {
    /*
    GUI variables.
    */
    private Seesaw seesaw;
    private SeesawBase seesawBase;
    private final JLayeredPane layeredPane;
    private JLabel jLabelTimer;
    private final Point origin = new Point(0, 0);
    
    private final double transX = 0.0;
    private final double transY = 0.0;
    private double rotateTheta = 0.0;
    private double rotateX = 150.0;
    private double rotateY = 150.0;
    private final double scaleX = 1.0;
    private final double scaleY = 1.0;
    
    private double max = 100.0;
    
    /*
    Signal processing variables.
    */
    private double firstSubjectSignal = 0.0;
    private double secondSubjectSignal = 0.0;
    private double difference = 0.0;
    private boolean count = false;
    
    private int mode = 0;
    private double scoreRatio = 20.0;
    private final double competitiveMinimumDifference = 30.0;
    private final double acceptedCollaborativeThreshold = 10.0;
    
    private FirstSubjectSignalUpdater firstSubjectSignalUpdater;
    private SecondSubjectSignalUpdater secondSubjectSignalUpdater;
    private ResetUpdater resetUpdater;
    
    private ScheduledExecutorService exec;
    private ScheduledExecutorService timerUpdater;
    
    private ScheduledExecutorService firstSubjectSimulator;
    private ScheduledExecutorService secondSubjectSimulator;
    
    
    class ActivityPanel extends JPanel {
        public ActivityPanel() {
            
        }
        
        @Override 
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            
            Graphics2D g2D = (Graphics2D) g;
        }
    }
    
    class Seesaw extends JPanel {
        public Line2D line;
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2D = (Graphics2D) g;
            
            g2D.translate(transX, transY);
            g2D.rotate(rotateTheta, rotateX, rotateY);
            g2D.scale(scaleX, scaleY);
            
            g2D.setStroke(new BasicStroke(10));

            drawSeesaw(g2D);
        }
        /* the seesaw structure, */
        public void drawSeesaw(Graphics2D g2D){
            line = new Line2D.Double(this.getWidth() / 8, this.getHeight() / 2, this.getWidth() / 8 * 7, this.getHeight() / 2);
            
            rotateX = this.getWidth() / 2;
            rotateY = line.getY1();
            
            g2D.draw(line);
        }
    }
    
    class SeesawBase extends JPanel {
        public Polygon base;
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2D = (Graphics2D) g;
            
            g2D.setColor(Color.red);

            drawSeesawBase(g2D);
        }
        
        public void drawSeesawBase(Graphics2D g2D){
            base = new Polygon(
                    new int[]{this.getWidth() / 4 * 1, this.getWidth() / 2, this.getWidth() / 4 * 3},
                    new int[]{this.getHeight() / 2 * 3, this.getHeight() / 2, this.getHeight() / 2 * 3},
                    3);

            g2D.fill(base);
        }
    }
    
    class FirstSubjectSignalUpdater extends SwingWorker<Void, Void> {
        private final double signalValue;
        
        public FirstSubjectSignalUpdater(double signalValue) {
            this.signalValue = signalValue;
        }
        
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            firstSubjectSignal = signalValue;
            
            return null;
        }
 
        /*
         * Executed in event dispatching thread.
         */
        @Override
        public void done() {
            
        }
    }
    
    class SecondSubjectSignalUpdater extends SwingWorker<Void, Void> {
        private final double signalValue;
        
        public SecondSubjectSignalUpdater(double signalValue) {
            this.signalValue = signalValue;
        }
        
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            secondSubjectSignal = signalValue;
            
            return null;
        }
 
        /*
         * Executed in event dispatching thread.
         */
        @Override
        public void done() {
            
        }
    }
    
    class ResetUpdater extends SwingWorker<Void, Void> {
        public ResetUpdater() {
            
        }
        
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            max = 100;
        
            mode = 0;

            scoreRatio = 20.0;

            // Reset signal bars max.
            updateSubjectsBarsMax(100.0);

            // Reset signal bars.
            updateFirstSubjectBar(0.0);
            updateSecondSubjectBar(0.0);

            hideAllScoreComponents();

            resetAllScoreComponents();

            // Enable menu.
            jMenuExperiment.setEnabled(true);
            jMenuHelp.setEnabled(true);

            // Reset GUI.
            rotateTheta = Math.toRadians(0);

            seesaw.repaint();

            // Disable reset button.
            jButtonReset.setEnabled(false);

            // Attempt to shutdown any pending process.
            try
            {
                exec.shutdownNow();
                exec = null;
            }
            catch (Exception ex)
            {

            }
            
            return null;
        }
 
        /*
         * Executed in event dispatching thread.
         */
        @Override
        public void done() {
            
        }
    }
    
    public BrainComputerInterfaceGUI() {
        initComponents();
        
        // Maximise window.
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        
        // Central layered panel.
        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(jPanelActivityArea.getWidth(), jPanelActivityArea.getHeight()));
        
        // Initialise GUI objects.
        seesaw = new Seesaw();
        seesawBase = new SeesawBase();
        jLabelTimer = new JLabel("0", SwingConstants.CENTER);
        jLabelTimer.setFont(new Font("Arial", Font.PLAIN, 40));
        
        // Make them transparent.
        seesaw.setOpaque(false);
        seesawBase.setOpaque(false);
        jLabelTimer.setOpaque(false);
        
        // Set the bounds: position and dimentions.
        seesaw.setBounds(origin.x, origin.y, jPanelActivityArea.getWidth(), jPanelActivityArea.getHeight() / 2);
        seesawBase.setBounds(origin.x, origin.y + 5, jPanelActivityArea.getWidth(), jPanelActivityArea.getHeight() / 2);
        jLabelTimer.setBounds(jPanelActivityArea.getWidth() / 2 - 60, origin.y, 120, 50);
        
        // Add the GUI objects according to their appropriate z order.
        layeredPane.add(seesawBase, 0);
        layeredPane.add(jLabelTimer, 1);
        layeredPane.add(seesaw, 2);
        
        layeredPane.addComponentListener(new ComponentListener(){

            @Override
            public void componentResized(ComponentEvent e) {
                // Resize accordingly.
                seesaw.setBounds(origin.x, origin.y, jPanelActivityArea.getWidth(), jPanelActivityArea.getHeight() / 4 * 2);
                seesawBase.setBounds(origin.x, origin.y + 5, jPanelActivityArea.getWidth(), jPanelActivityArea.getHeight() / 4 * 2);
                jLabelTimer.setBounds(jPanelActivityArea.getWidth() / 2 - 60, origin.y, 120, 50);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                
            }

            @Override
            public void componentShown(ComponentEvent e) {
                
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                
            }
        
        });
        
        // Add central layered panel to central panel.
        GroupLayout jPanelActivityAreaLayout = new GroupLayout(jPanelActivityArea);
        jPanelActivityArea.setLayout(jPanelActivityAreaLayout);
        jPanelActivityAreaLayout.setAutoCreateGaps(false);
        jPanelActivityAreaLayout.setAutoCreateContainerGaps(false);
        
        jPanelActivityAreaLayout.setHorizontalGroup(
            jPanelActivityAreaLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(layeredPane)
        );
        
        jPanelActivityAreaLayout.setVerticalGroup(
            jPanelActivityAreaLayout.createSequentialGroup()
                .addComponent(layeredPane)
        );
        
        // Hide score bars until needed.
        hideAllScoreComponents();
        
        // Set max values for the score bars.
        jProgressBarFirstSubjectScore.setMaximum((int) max);
        jProgressBarSecondSubjectScore.setMaximum((int) max);
        jProgressBarCollaborativeScore.setMaximum((int) max);
        
        // Disable reset button.
        jButtonReset.setEnabled(false);
        
        exec = Executors.newSingleThreadScheduledExecutor();
        timerUpdater = Executors.newSingleThreadScheduledExecutor();
        
        firstSubjectSimulator = Executors.newSingleThreadScheduledExecutor();
        secondSubjectSimulator = Executors.newSingleThreadScheduledExecutor();
        
        firstSubjectSimulator.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run() {
                Random randomGenerator = new Random();
                updateFirstSubjectSignal(randomGenerator.nextInt(100));
          }
        }, 0, 500, TimeUnit.MILLISECONDS);
        
        secondSubjectSimulator.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run() {
                Random randomGenerator = new Random();
                updateSecondSubjectSignal(randomGenerator.nextInt(100));
          }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelFirstSubject = new javax.swing.JPanel();
        jProgressBarFirstSubject = new javax.swing.JProgressBar();
        jPanelActivityArea = new ActivityPanel();
        jPanelSecondSubject = new javax.swing.JPanel();
        jProgressBarSecondSubject = new javax.swing.JProgressBar();
        jProgressBarFirstSubjectScore = new javax.swing.JProgressBar();
        jProgressBarSecondSubjectScore = new javax.swing.JProgressBar();
        jButtonReset = new javax.swing.JButton();
        jProgressBarCollaborativeScore = new javax.swing.JProgressBar();
        jMenuBarGeneral = new javax.swing.JMenuBar();
        jMenuExperiment = new javax.swing.JMenu();
        jMenuTraining = new javax.swing.JMenu();
        jMenuItemTrainingCompetitive = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemTrainingCollaborative = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemCompetitive = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemCollaborative = new javax.swing.JMenuItem();
        jMenuFirstEmptySpace = new javax.swing.JMenu();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemAbout = new javax.swing.JMenuItem();
        jMenuSecondEmptySpace = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("EEG-based BCI");

        jPanelFirstSubject.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "First Subject", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 20))); // NOI18N
        jPanelFirstSubject.setToolTipText("");
        jPanelFirstSubject.setName("firstSubjectPanel"); // NOI18N
        jPanelFirstSubject.setPreferredSize(new java.awt.Dimension(200, 500));

        jProgressBarFirstSubject.setOrientation(1);
        jProgressBarFirstSubject.setMinimumSize(new java.awt.Dimension(150, 400));

        javax.swing.GroupLayout jPanelFirstSubjectLayout = new javax.swing.GroupLayout(jPanelFirstSubject);
        jPanelFirstSubject.setLayout(jPanelFirstSubjectLayout);
        jPanelFirstSubjectLayout.setHorizontalGroup(
            jPanelFirstSubjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFirstSubjectLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProgressBarFirstSubject, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelFirstSubjectLayout.setVerticalGroup(
            jPanelFirstSubjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFirstSubjectLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProgressBarFirstSubject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelActivityArea.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Activity", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 20))); // NOI18N
        jPanelActivityArea.setFont(new java.awt.Font("Tahoma", 0, 20)); // NOI18N
        jPanelActivityArea.setPreferredSize(new java.awt.Dimension(550, 500));

        javax.swing.GroupLayout jPanelActivityAreaLayout = new javax.swing.GroupLayout(jPanelActivityArea);
        jPanelActivityArea.setLayout(jPanelActivityAreaLayout);
        jPanelActivityAreaLayout.setHorizontalGroup(
            jPanelActivityAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 733, Short.MAX_VALUE)
        );
        jPanelActivityAreaLayout.setVerticalGroup(
            jPanelActivityAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 469, Short.MAX_VALUE)
        );

        jPanelSecondSubject.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Second Subject", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 20))); // NOI18N
        jPanelSecondSubject.setToolTipText("");
        jPanelSecondSubject.setName("secondSubjectPanel"); // NOI18N
        jPanelSecondSubject.setPreferredSize(new java.awt.Dimension(200, 500));

        jProgressBarSecondSubject.setOrientation(1);
        jProgressBarSecondSubject.setMinimumSize(new java.awt.Dimension(150, 400));

        javax.swing.GroupLayout jPanelSecondSubjectLayout = new javax.swing.GroupLayout(jPanelSecondSubject);
        jPanelSecondSubject.setLayout(jPanelSecondSubjectLayout);
        jPanelSecondSubjectLayout.setHorizontalGroup(
            jPanelSecondSubjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSecondSubjectLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProgressBarSecondSubject, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelSecondSubjectLayout.setVerticalGroup(
            jPanelSecondSubjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSecondSubjectLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProgressBarSecondSubject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jProgressBarFirstSubjectScore.setOrientation(1);

        jProgressBarSecondSubjectScore.setOrientation(1);

        jButtonReset.setText("Reset");
        jButtonReset.setEnabled(false);
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jMenuBarGeneral.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N

        jMenuExperiment.setText("Experiment");
        jMenuExperiment.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N

        jMenuTraining.setText("Training");
        jMenuTraining.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N

        jMenuItemTrainingCompetitive.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuItemTrainingCompetitive.setText("Competitive");
        jMenuItemTrainingCompetitive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTrainingCompetitiveActionPerformed(evt);
            }
        });
        jMenuTraining.add(jMenuItemTrainingCompetitive);
        jMenuTraining.add(jSeparator3);

        jMenuItemTrainingCollaborative.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuItemTrainingCollaborative.setText("Collaborative");
        jMenuItemTrainingCollaborative.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTrainingCollaborativeActionPerformed(evt);
            }
        });
        jMenuTraining.add(jMenuItemTrainingCollaborative);

        jMenuExperiment.add(jMenuTraining);
        jMenuExperiment.add(jSeparator1);

        jMenuItemCompetitive.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuItemCompetitive.setText("Competitive");
        jMenuItemCompetitive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCompetitiveActionPerformed(evt);
            }
        });
        jMenuExperiment.add(jMenuItemCompetitive);
        jMenuExperiment.add(jSeparator2);

        jMenuItemCollaborative.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuItemCollaborative.setText("Collaborative");
        jMenuItemCollaborative.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCollaborativeActionPerformed(evt);
            }
        });
        jMenuExperiment.add(jMenuItemCollaborative);

        jMenuBarGeneral.add(jMenuExperiment);

        jMenuFirstEmptySpace.setEnabled(false);
        jMenuFirstEmptySpace.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuBarGeneral.add(jMenuFirstEmptySpace);

        jMenuHelp.setText("Help");
        jMenuHelp.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N

        jMenuItemAbout.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuItemAbout.setText("About");
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemAbout);

        jMenuBarGeneral.add(jMenuHelp);

        jMenuSecondEmptySpace.setEnabled(false);
        jMenuSecondEmptySpace.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jMenuBarGeneral.add(jMenuSecondEmptySpace);

        setJMenuBar(jMenuBarGeneral);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelFirstSubject, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jProgressBarFirstSubjectScore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBarCollaborativeScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelActivityArea, javax.swing.GroupLayout.DEFAULT_SIZE, 745, Short.MAX_VALUE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jProgressBarSecondSubjectScore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanelSecondSubject, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(174, 174, 174)
                        .addComponent(jButtonReset, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonReset, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                    .addComponent(jProgressBarCollaborativeScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBarFirstSubjectScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelSecondSubject, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE)
                    .addComponent(jPanelActivityArea, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE)
                    .addComponent(jPanelFirstSubject, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE)
                    .addComponent(jProgressBarSecondSubjectScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanelFirstSubject.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAboutActionPerformed
        JOptionPane.showMessageDialog(null, "EEG-based BCI was written by Abdullah Garcia.\nContact: abdullah.garcia@gmail.com", "About EEG-based BCI", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMenuItemAboutActionPerformed

    private void jMenuItemTrainingCompetitiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTrainingCompetitiveActionPerformed
        // Set mode.
        mode = 1;
        
        // Set different score limit.
        setScoreLimitForTraining();
        
        // Disable menu.
        jMenuExperiment.setEnabled(false);
        jMenuHelp.setEnabled(false);
        
        // Display score bars.
        displayCompetitiveScoreBars();
        
        if (exec == null)
        {
            exec = Executors.newSingleThreadScheduledExecutor();
            
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
        else
        {
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
    }//GEN-LAST:event_jMenuItemTrainingCompetitiveActionPerformed

    private void jMenuItemTrainingCollaborativeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTrainingCollaborativeActionPerformed
        // Set mode.
        mode = 2;
        
        // Set different score limit.
        setScoreLimitForTraining();
        
        // Disable menu.
        jMenuExperiment.setEnabled(false);
        jMenuHelp.setEnabled(false);
        
        // Display score components.
        displayCollaborativeScoreComponents();
        
        if (timerUpdater == null)
        {
            timerUpdater = Executors.newSingleThreadScheduledExecutor();
            
            timerUpdater.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (count)
                    {
                        if (Integer.parseInt(jLabelTimer.getText()) == 5)
                        {
                            if (jProgressBarCollaborativeScore.getValue() < 100)
                            {
                                jProgressBarCollaborativeScore.setValue((int) Math.round(jProgressBarCollaborativeScore.getValue() + 100 / scoreRatio));

                                count = false;

                                jLabelTimer.setText("0");
                            }
                            else
                            {
                                exec.shutdownNow();
                                exec = null;

                                timerUpdater.shutdownNow();
                                timerUpdater = null;

                                jButtonReset.setEnabled(true);
                            }
                        }
                        else
                        {
                            int value = Integer.parseInt(jLabelTimer.getText());

                            value = value + 1;

                            jLabelTimer.setText(String.valueOf(value));
                        }
                    }
                    else
                    {
                        jLabelTimer.setText("0");
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
        else
        {
            timerUpdater.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (count)
                {
                    if (Integer.parseInt(jLabelTimer.getText()) == 5)
                    {
                        if (jProgressBarCollaborativeScore.getValue() < 100)
                        {
                            jProgressBarCollaborativeScore.setValue((int) Math.round(jProgressBarCollaborativeScore.getValue() + 100 / scoreRatio));
                            
                            count = false;
                            
                            jLabelTimer.setText("0");
                        }
                        else
                        {
                            exec.shutdownNow();
                            exec = null;

                            timerUpdater.shutdownNow();
                            timerUpdater = null;

                            jButtonReset.setEnabled(true);
                        }
                    }
                    else
                    {
                        int value = Integer.parseInt(jLabelTimer.getText());

                        value = value + 1;

                        jLabelTimer.setText(String.valueOf(value));
                    }
                }
                else
                {
                    jLabelTimer.setText("0");
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        }
        
        if (exec == null)
        {
            exec = Executors.newSingleThreadScheduledExecutor();
            
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
        else
        {
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
    }//GEN-LAST:event_jMenuItemTrainingCollaborativeActionPerformed

    private void jMenuItemCompetitiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCompetitiveActionPerformed
        // Set mode.
        mode = 3;
        
        // Disable menu.
        jMenuExperiment.setEnabled(false);
        jMenuHelp.setEnabled(false);

        // Display score bars.
        displayCompetitiveScoreBars();
        
        if (exec == null)
        {
            exec = Executors.newSingleThreadScheduledExecutor();
            
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
        else
        {
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
    }//GEN-LAST:event_jMenuItemCompetitiveActionPerformed

    private void jMenuItemCollaborativeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCollaborativeActionPerformed
        // Set mode.
        mode = 4;
        
        // Disable menu.
        jMenuExperiment.setEnabled(false);
        jMenuHelp.setEnabled(false);
        
        // Display score bars.
        displayCollaborativeScoreComponents();
        
        if (timerUpdater == null)
        {
            timerUpdater = Executors.newSingleThreadScheduledExecutor();
            
            timerUpdater.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (count)
                    {
                        if (Integer.parseInt(jLabelTimer.getText()) == 5)
                        {
                            if (jProgressBarCollaborativeScore.getValue() < 100)
                            {
                                jProgressBarCollaborativeScore.setValue((int) Math.round(jProgressBarCollaborativeScore.getValue() + 100 / scoreRatio));

                                count = false;

                                jLabelTimer.setText("0");
                            }
                            else
                            {
                                exec.shutdownNow();
                                exec = null;

                                timerUpdater.shutdownNow();
                                timerUpdater = null;

                                jButtonReset.setEnabled(true);
                            }
                        }
                        else
                        {
                            int value = Integer.parseInt(jLabelTimer.getText());

                            value = value + 1;

                            jLabelTimer.setText(String.valueOf(value));
                        }
                    }
                    else
                    {
                        jLabelTimer.setText("0");
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
        else
        {
            timerUpdater.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (count)
                {
                    if (Integer.parseInt(jLabelTimer.getText()) == 5)
                    {
                        if (jProgressBarCollaborativeScore.getValue() < 100)
                        {
                            jProgressBarCollaborativeScore.setValue((int) Math.round(jProgressBarCollaborativeScore.getValue() + 100 / scoreRatio));
                            
                            count = false;
                            
                            jLabelTimer.setText("0");
                        }
                        else
                        {
                            exec.shutdownNow();
                            exec = null;

                            timerUpdater.shutdownNow();
                            timerUpdater = null;

                            jButtonReset.setEnabled(true);
                        }
                    }
                    else
                    {
                        int value = Integer.parseInt(jLabelTimer.getText());

                        value = value + 1;

                        jLabelTimer.setText(String.valueOf(value));
                    }
                }
                else
                {
                    jLabelTimer.setText("0");
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        }
        
        if (exec == null)
        {
            exec = Executors.newSingleThreadScheduledExecutor();
            
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
        else
        {
            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    normaliseValues();
                    update(firstSubjectSignal, secondSubjectSignal);
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
    }//GEN-LAST:event_jMenuItemCollaborativeActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        this.resetUpdater = new ResetUpdater();
        this.resetUpdater.execute();
    }//GEN-LAST:event_jButtonResetActionPerformed

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(BrainComputerInterfaceGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BrainComputerInterfaceGUI().setVisible(true);
            }
        });
    }
    
    /*
    Matlab - GUI interaction methods.
    */
    public void updateFirstSubjectSignal(double firstSubjectSignal)
    {
        this.firstSubjectSignalUpdater = new FirstSubjectSignalUpdater(firstSubjectSignal);
        this.firstSubjectSignalUpdater.execute();
    }
    
    public void updateSecondSubjectSignal(double secondSubjectSignal)
    {
        this.secondSubjectSignalUpdater = new SecondSubjectSignalUpdater(secondSubjectSignal);
        this.secondSubjectSignalUpdater.execute();
    }
    
    private void setScoreLimitForTraining()
    {
        Object[] possibilities = {"10", "20", "50"};
        
        String selectedOption = (String)JOptionPane.showInputDialog(
                            this,
                            "Select the amount of times the subjects need to score in order to win: ",
                            "EEG-based BCI",
                            JOptionPane.PLAIN_MESSAGE,
                            new ImageIcon(),
                            possibilities,
                            "20");
        
        switch (selectedOption)
        {
            case "10":
                scoreRatio = 10.0;
                break;
            
            case "20":
                scoreRatio = 20.0;
                break;
            
            case "50":
                scoreRatio = 50.0;
                break;
                
            default:
                break;
        }
    }
    
    /*
    Signal processing methods.
    */
    /*Peilun added. previous version of normalisation generating unfair pairing
      if the actural signal of first one is tiny larger than 100 but the second
      one is a bit smaller, the original normalization method would give a 
      result of second > first, and there is no exception in */
    private void normaliseValues()
    {
        /*less than zero case*/
        if (firstSubjectSignal <= 0 || secondSubjectSignal <= 0)
        {
        
            if (firstSubjectSignal <= 0)
                firstSubjectSignal = 0;
            
            if (secondSubjectSignal <= 0)
                secondSubjectSignal = 0;           
        }
        /*restructure if statement*/
        if (firstSubjectSignal > 0 && secondSubjectSignal > 0)
        {
            // Normalise values to tenths.
            if (firstSubjectSignal < 1)
            {    
                if (secondSubjectSignal >= 1)
                {                
                    secondSubjectSignal = 100;
                }
                else 
                {
                    secondSubjectSignal = secondSubjectSignal * 100.0;
                }
                firstSubjectSignal = firstSubjectSignal * 100.0;
            }
            else if (firstSubjectSignal < 10)
            {
                if (secondSubjectSignal >= 10)
                {
                    secondSubjectSignal = 100;
                }
                else
                {
                    secondSubjectSignal = secondSubjectSignal * 10.0;
                }
                
                firstSubjectSignal = firstSubjectSignal * 10.0;
            }
            else if (firstSubjectSignal > 100)
            {
                if (secondSubjectSignal < 100)
                {
                    secondSubjectSignal = 9;
                }
                else
                {
                    secondSubjectSignal = secondSubjectSignal / 10.0;
                }
                
                firstSubjectSignal = firstSubjectSignal / 10.0;
            }
            
            if (secondSubjectSignal < 1)
            {
                secondSubjectSignal = secondSubjectSignal * 100.0;
            }           
            else if (secondSubjectSignal < 10 && secondSubjectSignal >= 1)
            {
                secondSubjectSignal = secondSubjectSignal * 10.0;
            }
            
            
            
            if (secondSubjectSignal > 100)
            {
                secondSubjectSignal = secondSubjectSignal / 10.0;
            }
        }
    }
    
    private void update(double firstSubjectSignal, double secondSubjectSignal)
    {
        if (firstSubjectSignal != 0.0 && secondSubjectSignal != 0.0)
        {
            switch (mode)
            {
                // Training competitive.
                case 1:
                    displayCompetitiveScoreBars();
                    
                    adjustMaxValueForBars(firstSubjectSignal, secondSubjectSignal);
                    
                    // Calculate difference between signals.
                    difference = firstSubjectSignal - secondSubjectSignal;
                    
                    // Consider an absolute difference between values.
                    if (difference >= competitiveMinimumDifference)
                    {
                        // Display absolute difference.
                        rotateTheta = Math.toRadians(-30.0);

                        // Update score or finish the activity accordingly.
                        if (jProgressBarFirstSubjectScore.getValue() < 100)
                        {
                            jProgressBarFirstSubjectScore.setValue((int) Math.round(jProgressBarFirstSubjectScore.getValue() + 100 / scoreRatio));
                        }
                        else
                        {
                            exec.shutdownNow();
                            exec = null;
                            
                            jButtonReset.setEnabled(true);
                        }
                    }
                    else
                    {
                        // Consider an absolute difference between values.
                        if (difference <= competitiveMinimumDifference * -1.0)
                        {
                            // Display absolute difference.
                            rotateTheta = Math.toRadians(30.0);

                            // Update score or finish the activity accordingly.
                            if (jProgressBarSecondSubjectScore.getValue() < 100)
                            {
                                jProgressBarSecondSubjectScore.setValue((int) Math.round(jProgressBarSecondSubjectScore.getValue() + 100 / scoreRatio));
                            }
                            else
                            {
                                exec.shutdownNow();
                                exec = null;
                                
                                jButtonReset.setEnabled(true);
                            }
                        }
                        else
                        {
                            // Assuming there isn't an absolute difference, update accordingly. However, keep track of scores.
                            if ((jProgressBarFirstSubjectScore.getValue() < 100) && (jProgressBarSecondSubjectScore.getValue() < 100))
                            {
                                rotateTheta = Math.toRadians(difference * -1.0);
                            }
                            else
                            {
                                exec.shutdownNow();
                                exec = null;
                                
                                jButtonReset.setEnabled(true);
                            }
                        }
                    }
                    
                    break;
                    
                // Training collaborative.
                case 2:
                    displayCollaborativeScoreComponents();
                            
                    adjustMaxValueForBars(firstSubjectSignal, secondSubjectSignal);
                    
                    // Calculate difference between signals.
                    difference = firstSubjectSignal - secondSubjectSignal;
                    
                    if (difference >= acceptedCollaborativeThreshold * -1 && difference <= acceptedCollaborativeThreshold)
                    {
                        rotateTheta = Math.toRadians(0);
                        
                        count = true;
                    }
                    else
                    {
                        if (difference > acceptedCollaborativeThreshold)
                        {
                            rotateTheta = Math.toRadians(-30);
                            
                            //Stop timer.
                            count = false;
                        }
                        
                        if (difference < acceptedCollaborativeThreshold * -1.0)
                        {
                            rotateTheta = Math.toRadians(30);
                            
                            //Stop timer.
                            count = false;
                        }
                    }
                    
                    break;
                
                // Competitive.
                case 3:
                    displayCompetitiveScoreBars();
                    
                    adjustMaxValueForBars(firstSubjectSignal, secondSubjectSignal);
                    
                    // Calculate difference between signals.
                    difference = firstSubjectSignal - secondSubjectSignal;
                    
                    // Consider an absolute difference between values.
                    if (difference >= competitiveMinimumDifference)
                    {
                        // Display absolute difference.
                        rotateTheta = Math.toRadians(-30.0);

                        // Update score or finish the activity accordingly.
                        if (jProgressBarFirstSubjectScore.getValue() < 100)
                        {
                            jProgressBarFirstSubjectScore.setValue((int) Math.round(jProgressBarFirstSubjectScore.getValue() + 100 / scoreRatio));
                        }
                        else
                        {
                            exec.shutdownNow();
                            exec = null;
                            
                            jButtonReset.setEnabled(true);
                        }
                    }
                    else
                    {
                        // Consider an absolute difference between values.
                        if (difference <= competitiveMinimumDifference * -1.0)
                        {
                            // Display absolute difference.
                            rotateTheta = Math.toRadians(30.0);

                            // Update score or finish the activity accordingly.
                            if (jProgressBarSecondSubjectScore.getValue() < 100)
                            {
                                jProgressBarSecondSubjectScore.setValue((int) Math.round(jProgressBarSecondSubjectScore.getValue() + 100 / scoreRatio));
                            }
                            else
                            {
                                exec.shutdownNow();
                                exec = null;
                                
                                jButtonReset.setEnabled(true);
                            }
                        }
                        else
                        {
                            // Assuming there isn't an absolute difference, update accordingly. However, keep track of scores.
                            if ((jProgressBarFirstSubjectScore.getValue() < 100) && (jProgressBarSecondSubjectScore.getValue() < 100))
                            {
                                rotateTheta = Math.toRadians(difference * -1.0);
                            }
                            else
                            {
                                exec.shutdownNow();
                                exec = null;
                                
                                jButtonReset.setEnabled(true);
                            }
                        }
                    }
                    
                    break;
                    
                // Collaborative.
                case 4:
                    displayCollaborativeScoreComponents();
                            
                    adjustMaxValueForBars(firstSubjectSignal, secondSubjectSignal);
                    
                    // Calculate difference between signals.
                    difference = firstSubjectSignal - secondSubjectSignal;
                    
                    if (difference >= acceptedCollaborativeThreshold * -1 && difference <= acceptedCollaborativeThreshold)
                    {
                        rotateTheta = Math.toRadians(0);
                        
                        count = true;
                    }
                    else
                    {
                        if (difference > acceptedCollaborativeThreshold)
                        {
                            rotateTheta = Math.toRadians(-30);
                            
                            //Stop timer.
                            count = false;
                        }
                        
                        if (difference < acceptedCollaborativeThreshold * -1.0)
                        {
                            rotateTheta = Math.toRadians(30);
                            
                            //Stop timer.
                            count = false;
                        }
                    }
                    
                    break;
                default:
                    break;
            }
            
            
            seesaw.repaint();

            updateFirstSubjectBar(firstSubjectSignal);
            updateSecondSubjectBar(secondSubjectSignal);
            
            this.firstSubjectSignal = 0.0;
            this.secondSubjectSignal = 0.0;
        }
    }
    
    private void hideAllScoreComponents()
    {
        jProgressBarFirstSubjectScore.setVisible(false);
        jProgressBarSecondSubjectScore.setVisible(false);
        jProgressBarCollaborativeScore.setVisible(false);
        jLabelTimer.setVisible(false);
    }
    
    private void resetAllScoreComponents()
    {
        jProgressBarFirstSubjectScore.setValue(0);
        jProgressBarSecondSubjectScore.setValue(0);
        jProgressBarCollaborativeScore.setValue(0);
        jLabelTimer.setText("");
    }
    
    private void displayCompetitiveScoreBars()
    {
        jProgressBarFirstSubjectScore.setVisible(true);
        jProgressBarSecondSubjectScore.setVisible(true);
    }
    
    private void displayCollaborativeScoreComponents()
    {
        jProgressBarCollaborativeScore.setVisible(true);
        jLabelTimer.setVisible(true);
    }
    
    private void adjustMaxValueForBars(double firstSubjectSignal, double secondSubjectSignal)
    {
        if (firstSubjectSignal > max)
        {
            updateSubjectsBarsMax(firstSubjectSignal);
            max = firstSubjectSignal;
        }

        if (secondSubjectSignal > max)
        {
            updateSubjectsBarsMax(secondSubjectSignal);
            max = secondSubjectSignal;
        }
    }
    
    private void updateSubjectsBarsMax(double signal)
    {
        this.jProgressBarFirstSubject.setMaximum((int) Math.round(signal));
        this.jProgressBarSecondSubject.setMaximum((int) Math.round(signal));
    }
    
    private void updateFirstSubjectBar(double signal)
    {
        this.jProgressBarFirstSubject.setValue((int) Math.round(signal));
    }
    
    private void updateSecondSubjectBar(double signal)
    {
        this.jProgressBarSecondSubject.setValue((int) Math.round(signal));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonReset;
    private javax.swing.JMenuBar jMenuBarGeneral;
    private javax.swing.JMenu jMenuExperiment;
    private javax.swing.JMenu jMenuFirstEmptySpace;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemCollaborative;
    private javax.swing.JMenuItem jMenuItemCompetitive;
    private javax.swing.JMenuItem jMenuItemTrainingCollaborative;
    private javax.swing.JMenuItem jMenuItemTrainingCompetitive;
    private javax.swing.JMenu jMenuSecondEmptySpace;
    private javax.swing.JMenu jMenuTraining;
    private javax.swing.JPanel jPanelActivityArea;
    private javax.swing.JPanel jPanelFirstSubject;
    private javax.swing.JPanel jPanelSecondSubject;
    private javax.swing.JProgressBar jProgressBarCollaborativeScore;
    private javax.swing.JProgressBar jProgressBarFirstSubject;
    private javax.swing.JProgressBar jProgressBarFirstSubjectScore;
    private javax.swing.JProgressBar jProgressBarSecondSubject;
    private javax.swing.JProgressBar jProgressBarSecondSubjectScore;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    // End of variables declaration//GEN-END:variables
}