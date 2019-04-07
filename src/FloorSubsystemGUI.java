import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class FloorSubsystemGUI implements Runnable {
    // Arraylists to hold JLables for all icon types
    private ArrayList<JLabel> upButtonArray;
    private ArrayList<JLabel> downButtonArray;
    private ArrayList<JLabel> lampArray;
    private ArrayList<JLabel> directionArray;
    
    // Icons
    // Icons used for floor request buttons
    private ImageIcon upUnlitIcon;
    private ImageIcon downUnlitIcon;
    private ImageIcon upLitIcon;
    private ImageIcon downLitIcon;
    
    // Icons used for elevator direction
    private ImageIcon upDirIcon;
    private ImageIcon downDirIcon;
    private ImageIcon stationaryIcon;
    
    private JFrame mainFrame;
    private JFrame hiddenFrame;
    
    /**
     * FloorSubsystemGUI
     * 
     * Constructor
     * 
     * Creates a new FloorSubsystemGUI.
     * Initializes the frames and sets sizes.
     * Retrieves and scales all icons.
     * Adds everything to the frames.
     * 
     * @param sub   FloorSubsystem that the GUI is displaying
     * 
     * @return  None
     */
    public FloorSubsystemGUI(FloorSubsystem sub) {
        hiddenFrame = new JFrame("");
        mainFrame = new JFrame("Floor Subsystem");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        mainFrame.setSize(1920, 1080);
        mainFrame.setResizable(false);
        
        Container contentPane = mainFrame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        
        
        
        JPanel floorPanel = new JPanel();
        floorPanel.setLayout(new GridLayout(sub.getNumFloors(), 3 * sub.getNumElevators()));
        
        ImageIcon temp = new ImageIcon("Images/Up_Button_OFF.png");
        Image upUnlit = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                            mainFrame.getHeight() / (2 * sub.getNumFloors()), 
                                                            Image.SCALE_DEFAULT);
        upUnlitIcon = new ImageIcon(upUnlit);
        
        
        temp = new ImageIcon("Images/Down_Button_OFF.png");
        Image downUnlit = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                            mainFrame.getHeight() / (2 * sub.getNumFloors()), 
                                                            Image.SCALE_DEFAULT);
        downUnlitIcon = new ImageIcon(downUnlit);
        
        
        temp = new ImageIcon("Images/Up_Button_ON.png");
        Image upLit = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                        mainFrame.getHeight() / (2 * sub.getNumFloors()),  
                                                            Image.SCALE_DEFAULT);
        upLitIcon = new ImageIcon(upLit);
        
        
        temp = new ImageIcon("Images/Down_Button_ON.png");
        Image downLit = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                        mainFrame.getHeight() / (2 * sub.getNumFloors()), 
                                                        Image.SCALE_DEFAULT);
        downLitIcon = new ImageIcon(downLit);
        
        
        
        
        
        temp = new ImageIcon("Images/Up_Direction.png");
        Image upDir = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                        mainFrame.getHeight() / sub.getNumFloors(), 
                                                        Image.SCALE_DEFAULT);
        upDirIcon = new ImageIcon(upDir);
        
        
        temp = new ImageIcon("Images/Stationary_Direction.png");
        Image stationaryDir = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                                mainFrame.getHeight() / sub.getNumFloors(), 
                                                        Image.SCALE_DEFAULT);
        stationaryIcon = new ImageIcon(stationaryDir);
        
        
        temp = new ImageIcon("Images/Down_Direction.png");
        Image downDir = temp.getImage().getScaledInstance(mainFrame.getWidth() / (3 * sub.getNumElevators()), 
                                                            mainFrame.getHeight() / sub.getNumFloors(), 
                                                            Image.SCALE_DEFAULT);
        downDirIcon = new ImageIcon(downDir);
        
        
        ArrayList<JPanel> buttonPArray = new ArrayList<JPanel>();
        upButtonArray = new ArrayList<JLabel>();
        downButtonArray = new ArrayList<JLabel>();
        
        directionArray = new ArrayList<JLabel>();
        lampArray = new ArrayList<JLabel>();
        
        for (int i = 0; i < sub.getNumFloors() * sub.getNumElevators(); i++) {

            
            buttonPArray.add(new JPanel(new GridLayout(2, 1)));
            directionArray.add(new JLabel(stationaryIcon));
            lampArray.add(new JLabel("0", SwingConstants.CENTER));

            upButtonArray.add(new JLabel(upUnlitIcon));
            downButtonArray.add(new JLabel(downUnlitIcon));
            
            
            buttonPArray.get(i).add(upButtonArray.get(i));
            buttonPArray.get(i).add(downButtonArray.get(i));
            
            
            floorPanel.add(buttonPArray.get(i));
            floorPanel.add(directionArray.get(i));
            floorPanel.add(lampArray.get(i));
        }
        
        
        
        
        contentPane.add(floorPanel);
        
        hiddenFrame.pack();
        mainFrame.pack();
        hiddenFrame.setVisible(false);
        mainFrame.setVisible(true);
    }
    
    /**
     * closeGUI
     * 
     * Closes the gui and disposes the frames
     * 
     * @param   None
     * 
     * @return  void
     */
    public void closeGUI() {
        mainFrame.dispose();
    }
    
    /**
     * setUpButtonLit
     * 
     * Lights the up button for the given floors
     * 
     * @param totalElevators    The number of elevators in the system
     * @param sourceFloor   The floor to light
     * 
     * @return  void
     */
    public void setUpButtonLit(int totalElevators, int sourceFloor) {
        for (int i = 0; i < totalElevators; i++) {
            upButtonArray.get((sourceFloor * totalElevators)  + i).setIcon(upLitIcon);
        }
    }
    
    /**
     * setDownButtonLit
     * 
     * Lights the down button for the given floors
     * 
     * @param totalElevators    The number of elevators in the system
     * @param sourceFloor   The floor to light
     * 
     * @return  void
     */
    public void setDownButtonLit(int totalElevators, int sourceFloor) {
        for (int i = 0; i < totalElevators; i++) {
            downButtonArray.get((sourceFloor * totalElevators)  + i).setIcon(downLitIcon);
        }
    }
    
    /**
     * setUpButtonUnlit
     * 
     * Unlights the up button for the given floor and elevator
     * 
     * @param totalElevators    The number of elevators in the system
     * @param sourceFloor       The floor to light
     * @param elevator          The number of the elevator shaft to unlight
     * 
     * @return  void
     */ 
    public void setUpButtonUnlit(int totalElevators, int floor, int elevator) {
        upButtonArray.get((floor * totalElevators)  + elevator).setIcon(upUnlitIcon);
    }

    /**
     * setDownButtonUnlit
     * 
     * Unlights the down button for the given floor and elevator
     * 
     * @param totalElevators    The number of elevators in the system
     * @param sourceFloor       The floor to light
     * @param elevator          The number of the elevator shaft to unlight
     * 
     * @return  void
     */ 
    public void setDownButtonUnlit(int totalElevators, int floor, int elevator) {
        downButtonArray.get((floor * totalElevators)  + elevator).setIcon(downUnlitIcon);
    }
    
    /**
     * updateFloorNum
     * 
     * Updates the floow number of the given elevator to the given floor.
     * 
     * @param totalFloors       Number of floors in the system
     * @param totalElevators    Number of elevators in the system
     * @param floor             Floor number that the elevator is on
     * @param elevator          Number of elevator being updated
     * @param dir               Direction that the elevator is travelling
     * 
     * @return  void
     */
    public void updateFloorNum(int totalFloors, int totalElevators, int floor, int elevator, UtilityInformation.ElevatorDirection dir) {
        for (int i = 0; i < totalFloors; i++ ) {
            lampArray.get((i * totalElevators) + elevator).setText(Integer.toString(floor));
            
            if (dir == UtilityInformation.ElevatorDirection.UP) {
                directionArray.get((i * totalElevators) + elevator).setIcon(upDirIcon);
            }else if (dir == UtilityInformation.ElevatorDirection.DOWN) {
                directionArray.get((i * totalElevators) + elevator).setIcon(downDirIcon);
            }else {
                directionArray.get((i * totalElevators) + elevator).setIcon(stationaryIcon);
            }
        }
    }
    
    /**
     * run
     * 
     * Override
     * 
     * Runs the GUI
     * 
     * @param   None
     * 
     * @return  void
     */
    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }
}
