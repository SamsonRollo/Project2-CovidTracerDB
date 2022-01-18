//package Covid19TracerApp;
//package tracerapp;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class NCoV19TracerUI extends JFrame{
	private String establishment;
	private NCoV19TracerController controller = null;
	private int establishmentID;
	
	private int activePanel;
	//panel1:login
	//panel2:logging in
	//panel3:contact tracer
	//panel4:traced table
	
	private CardLayout cardLayout;
	private JPanel mainPanel;
	
	private JPasswordField companyPass;

	//constructor
	public NCoV19TracerUI(){
		initUI();
		controller = new NCoV19TracerController(this);
	}

	//initialize the UI for the user
	private void initUI(){
		//FURNISH THIS
		setSize(500,500);
		setResizable(false);
		setTitle("UP Contact Tracer");
		
		cardLayout = new CardLayout();
		mainPanel = new JPanel();
		//mainPanel.setPreferredSize(new Dimension(500,200));
		mainPanel.setLayout(cardLayout);
		setLocationRelativeTo(null);
		
		//init cards
		initLogin();
		viewingScreen();
		listingScreen();
		// initMenu();

		//make this center EDIT PA
		//JLabel title = new JLabel("UP Contact Tracing\n DataBase", JLabel.CENTER);
		//title.setFont(new Font("Arial", Font.BOLD, 30));
		//title.setBounds(0, 0, this.getWidth(), 50);

		//this.add(title);
		//pack();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		mainPanel.setVisible(true);
		this.add(mainPanel);
		setScreen(1);
	}

	//login panel 
	private void initLogin(){
		this.setJMenuBar(null);
		
		JLabel companyName = new JLabel("Establishment ");
		companyName.setPreferredSize(new Dimension(250,50));
		JLabel password = new JLabel("Password ");
		companyName.setPreferredSize(new Dimension(250,50));
		final JTextField companyTF = new JTextField(10);
		companyName.setPreferredSize(new Dimension(250,50));
		companyPass = new JPasswordField(10);
		companyName.setPreferredSize(new Dimension(250,50));
		JButton loginSubmit = new JButton("Login");
		companyName.setPreferredSize(new Dimension(250,50));

		final JPanel loginPanel = new JPanel(new GridLayout(3,2,2,5));
		loginPanel.setPreferredSize(new Dimension(300,80));

		loginPanel.add(companyName);
		loginPanel.add(companyTF);
		loginPanel.add(password);
		loginPanel.add(companyPass);
		loginPanel.add(loginSubmit);

		loginSubmit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String estName = companyTF.getText();
				String estPass = new String (companyPass.getPassword());
				establishmentID = controller.logIn(estName, estPass);

				if(controller.establishmentVerified(establishmentID)){
						if(establishmentID==0){ //contact tracers
							setScreen(3);
							setMenu(3);
						}else{ //other establishments
							//loadMenu(false, true);
							setMenu(2);
							setScreen(2);
							//listingScreen();
						}
				}else{
					//establishment does not exists pop up
					if(establishmentID!=-1)
						JOptionPane.showMessageDialog(null,"Wrong Name or Password.\nIf your establishment is"
						+"not yet registered,\nPlease contact the developers.","Login Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mainPanel.add("a",loginPanel);
	}

	//JMenu
	private void setMenu(int menuState){
		final JMenuBar menuBar = new JMenuBar();
		
		//register button
		if(menuState == 3){
			JButton regBut = new JButton("Register New");
			regBut.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){ //edit
					JPanel inputPanel = new JPanel(new GridLayout(4,2,2,2));
					JTextField id = new JTextField(10);
					JTextField name = new JTextField(10);
					JTextField addr = new JTextField(20);
					JTextField pass = new JTextField(10);
					inputPanel.add(new JLabel("Estab. ID #"));
					inputPanel.add(id);
					inputPanel.add(new JLabel("Name"));
					inputPanel.add(name);
					inputPanel.add(new JLabel("Address"));
					inputPanel.add(addr);
					inputPanel.add(new JLabel("Password"));
					inputPanel.add(pass);

					int i = JOptionPane.showConfirmDialog(null,inputPanel,"Register New Establishment",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if(i==0){
						if(controller.isIDValid(id.getText())){
							String[] newEst = new String[4];
							
							newEst[0] = id.getText();
							newEst[1] = name.getText();
							newEst[2] = addr.getText();
							newEst[3] = pass.getText(); 
							boolean success = controller.regEstablishment(newEst); 

							if(success){
								JOptionPane.showMessageDialog(null,"Establishment registered successfully",
            				         "Registration Success", JOptionPane.INFORMATION_MESSAGE);
							}else{
								JOptionPane.showMessageDialog(null,"ID already exists.",
            				         "Registration Error", JOptionPane.ERROR_MESSAGE);
							}
						}else{
							JOptionPane.showMessageDialog(null,"Invalid ID.",
            				         "Registration Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			});
			menuBar.add(regBut);
		}
		
		//about
		if(menuState == 1){
			JButton aboutBut = new JButton("About");
			aboutBut.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					JOptionPane.showMessageDialog(null,"COVID19 Tracer App"+
						"\nThis app is a project for CMSC 127 which digitized the"+
						"\nfilling of forms in the establishments. This also made"+
						"\ncontact tracing efficient by implementing DBMS."+
						"\n\nDevelopers:\nSamson Rollo Jr"+
						"\nAdrian Mabansag","ABOUT", JOptionPane.INFORMATION_MESSAGE);
				}
			});
			menuBar.add(aboutBut);
			menuBar.add(Box.createHorizontalGlue());
		}
	
		//back button
		if(menuState == 4){
			JButton backButton = new JButton("Back");
			backButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					setScreen(3);
				}
			});
			menuBar.add(backButton);
		}
		//logout button
		else if(menuState == 3 || menuState == 2){
			JButton logoutBut = new JButton("Log Out");
			logoutBut.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					int choice = JOptionPane.showConfirmDialog(null,"You are about to log out!","LogOut",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if(choice==0){
						setScreen(1);
					}
				}
			});
			menuBar.add(logoutBut);
		}
		
		this.setJMenuBar(menuBar);
	}
	
	//listing screen for the establishments
	private void listingScreen(){
		//JLabel estLabel =  new JLabel(establishment);
		JLabel nameLabel = new JLabel("Name ");
		JLabel iDLabel = new JLabel("ID");
		JLabel addrLabel = new JLabel("Address");
		JLabel ageLabel = new JLabel("Age");
		JLabel contNumLabel = new JLabel("Contact #");
		JLabel custIDLabelOut = new JLabel("ID");
		final JTextField nameField = new JTextField(15);
		final JTextField idField = new JTextField(10);
		final JTextField addrField = new JTextField(15);
		final JTextField ageField = new JTextField(2);
		final JTextField contNumField = new JTextField(11);
		final JTextField idOutField = new JTextField(10);
		JButton add = new JButton("GO IN");
		JButton out = new JButton("GO OUT");
		//add label for establishment on top

		JPanel listingPanel = new JPanel(new GridLayout(10,2,2,5));
		//listingPanel.setBounds(50, 70, this.getWidth()-100, this.getHeight()-160);
		listingPanel.setPreferredSize(new Dimension(500,500));
		listingPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),BorderFactory.createEmptyBorder(10,10,10,10)));
		
		//add to card
		listingPanel.add(nameLabel);
		listingPanel.add(nameField);
		listingPanel.add(iDLabel);
		listingPanel.add(idField);
		listingPanel.add(addrLabel);
		listingPanel.add(addrField);
		listingPanel.add(ageLabel);
		listingPanel.add(ageField);
		listingPanel.add(contNumLabel);
		listingPanel.add(contNumField);
		listingPanel.add(add);
		listingPanel.add(new JLabel(""));
		listingPanel.add(new JLabel(""));
		listingPanel.add(new JLabel(""));
		listingPanel.add(new JLabel(""));
		listingPanel.add(new JLabel(""));
		listingPanel.add(custIDLabelOut);
		listingPanel.add(idOutField);
		listingPanel.add(out);

		add.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				//assume all are valid entries
				if(controller.isIDValid(idField.getText())){
					if(!controller.userVisited(Integer.parseInt(idField.getText()))){
						if(controller.isPhoneValid(contNumField.getText())){
							String[] input = new String[8];
							input[0] = nameField.getText();
							input[1] = idField.getText();
							input[2] = addrField.getText();
							input[3] = ageField.getText();
							input[4] = contNumField.getText();
							input[5] = (java.time.LocalTime.now()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")).toString();//(java.time.LocalTime.now()).toString(); //change to date fromat later
							input[6] = (java.time.LocalDate.now()).toString();
							input[7] = String.valueOf(establishmentID);
							controller.addRow(input);
							nameField.setText("");
							idField.setText("");
							addrField.setText("");
							ageField.setText("");
							contNumField.setText("");
						}else{
							JOptionPane.showMessageDialog(null,"Invalid Contact Number",
					            "Signup Error", JOptionPane.ERROR_MESSAGE);
						}
					}else{
						JOptionPane.showMessageDialog(null,"Person is already IN!",
		                	"Signup Error", JOptionPane.ERROR_MESSAGE);
					}
				}else{
					JOptionPane.showMessageDialog(null,"Invalid ID.",
            			"Registration Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		out.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent f){
				if(controller.isIDValid(idOutField.getText())){
					int id = Integer.parseInt(idOutField.getText());
					if(controller.userVisited(id)){
						String outTime = (java.time.LocalTime.now()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")).toString();
						controller.updateRow(id, outTime);
						idOutField.setText("");
						JOptionPane.showMessageDialog(null,"Thank you come again!",
	                       "Sign out", JOptionPane.INFORMATION_MESSAGE);
					}else{
						JOptionPane.showMessageDialog(null,"Person is not in yet!",
	                        "Sign out Error", JOptionPane.ERROR_MESSAGE);
					}
				}else{
					JOptionPane.showMessageDialog(null,"Not an ID number!",
	                "Sign out Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mainPanel.add("b",listingPanel);
		//this.revalidate();
	}

	//screen for contact tracers
	private void viewingScreen(){
		this.getContentPane().repaint();

		JPanel searchPanel = new JPanel(new GridLayout(2,1));
		searchPanel.setPreferredSize(new Dimension(500,140));
		//searchPanel.setBounds(10, 60, this.getWidth()-20, this.getHeight()-140);
		
		JPanel topPanel = new JPanel();
		JPanel bottomPanel = new JPanel();
		JLabel searchTitle = new JLabel("Search", JLabel.LEFT);
		final JTextField searchbox = new JTextField("Enter ID",15);
		String[] categories = {"1st-Level Contact","2nd-Level Contact","Establishments"};
		final JComboBox<String> traceType = new JComboBox<>(categories);
		final JTextField fromDate = new JTextField("from: yyyy-MM-dd",10);
		final JTextField toDate = new JTextField("to: yyyy-MM-dd",10);
		JButton traceButton = new JButton("Trace");
		JLabel toLabel = new JLabel("to");

		topPanel.add(searchTitle);
		topPanel.add(searchbox);
		topPanel.add(traceType);
		bottomPanel.add(fromDate);
		bottomPanel.add(toLabel);
		bottomPanel.add(toDate);
		bottomPanel.add(traceButton);

		searchPanel.add(topPanel);
		searchPanel.add(bottomPanel);
		traceButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String contact = searchbox.getText();
				String traceTypeString = traceType.getSelectedItem().toString();

				try{
					java.sql.Date.valueOf(fromDate.getText());
					java.sql.Date.valueOf(toDate.getText());
					setTable(controller.tracerQuery(contact, traceTypeString,fromDate.getText(), toDate.getText()));
					
					setScreen(4);
				}catch(Exception eD){
					JOptionPane.showMessageDialog(null,"Tracing Error!",
	                        "Trace Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}); 
		mainPanel.add("c",searchPanel);
		//this.revalidate();
	}

	private void setTable(final JTable table){
		
		JTableHeader header = table.getTableHeader();
		TableColumnModel tmc = header.getColumnModel();
		
		header.setReorderingAllowed(false);
        tmc.getColumn(0).setHeaderValue("ID");
        tmc.getColumn(1).setHeaderValue("Name");
        tmc.getColumn(2).setHeaderValue("Contact Number");
        tmc.getColumn(3).setHeaderValue("Address");
        
        
		JPanel tableResultPanel = new JPanel(new FlowLayout());
		tableResultPanel.setBounds(10, 120, this.getWidth()-20, this.getHeight()-200); //make this dynamic
		
		JButton exportButton = new JButton("Export");
		exportButton.setPreferredSize(new Dimension(200,50));
		exportButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Specify a file save");
				int choice = fileChooser.showSaveDialog(NCoV19TracerUI.this);
				if(choice == JFileChooser.APPROVE_OPTION){
					File file = fileChooser.getSelectedFile();
					FileWriter fw;
					try {
						fw = new FileWriter(file);
						BufferedWriter bw = new BufferedWriter(fw);
						for(int i = 0; i < table.getRowCount(); i++){
							for(int j = 0; j < table.getColumnCount(); j++){
								bw.write(table.getValueAt(i,j).toString()+",");
							}
							bw.newLine();
						}
						bw.close();
						fw.close();
						JOptionPane.showMessageDialog(NCoV19TracerUI.this, "Export was successful!", "EXPORT SUCCESS", JOptionPane.INFORMATION_MESSAGE);
					} catch (IOException e) {
						//e.printStackTrace();
						JOptionPane.showMessageDialog(NCoV19TracerUI.this, "Error in exporting file!", "EXPORT ERROR", JOptionPane.ERROR_MESSAGE);
					}
					
				}
			}
		});

		tableResultPanel.add(new JScrollPane(table));
		tableResultPanel.add(exportButton);
		

		mainPanel.add("d",tableResultPanel);
	}

	public String[] askCreden(){
		String[] creden = {"",""};
		JPanel inputPanel = new JPanel(new GridLayout(4,2,2,2));
		JTextField name = new JTextField(15);
		JTextField pass = new JTextField(15);
		inputPanel.add(new JLabel("DB username"));
		inputPanel.add(name);
		inputPanel.add(new JLabel("Password"));
		inputPanel.add(pass);

		int i = JOptionPane.showConfirmDialog(null,inputPanel,"Login to MYSQL DATABASE",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if(i==2){
			System.exit(1);
		}

		creden[0] = name.getText();
		creden[1] = pass.getText();

		return creden;
	}
	
	private void setScreen(int screenState){
		switch(screenState){
			//loginScreen	
			case 1:
				activePanel = 1;
				cardLayout.show(mainPanel,"a");
				setMenu(1);
				companyPass.setText("");
				mainPanel.setPreferredSize(new Dimension(400,140)); 
				this.setPreferredSize(new Dimension (500,200));
				pack();
			break;
			//listingScreen
			case 2:
				activePanel = 2;
				cardLayout.show(mainPanel, "b");
				setMenu(2);
				mainPanel.setPreferredSize(new Dimension(400,440)); 
				this.setPreferredSize(new Dimension (500,500));
				pack();
			break;
			//viewingScreen
			case 3:
				activePanel = 3;
				cardLayout.show(mainPanel, "c");
				setMenu(3);
				mainPanel.setPreferredSize(new Dimension(400,140));
				this.setPreferredSize(new Dimension (500,200));
				pack();
			break;
			//traceTableScreen
			case 4:
				activePanel = 4;
				cardLayout.show(mainPanel, "d");
				setMenu(4);
				mainPanel.setPreferredSize(new Dimension(400,400));
				this.setPreferredSize(new Dimension(500,600));
				pack();
			break;
		}
		this.repaint();
		this.revalidate();
	}

	public static void main(String[] args){
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				new NCoV19TracerUI().setVisible(true);
			}
		});
	}
}