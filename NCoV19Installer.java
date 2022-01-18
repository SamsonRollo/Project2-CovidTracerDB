

import java.lang.ProcessBuilder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.sql.*;

public class NCoV19Installer{

	private Connection connection = null;
	
	public NCoV19Installer(){
 		String[] creden = askCreden("DB username", "Login to MYSQL DATABASE", JOptionPane.OK_CANCEL_OPTION, 4);
		//add concurrency for waiting
		ProcessReport pr = importDDL(creden);
		if(!pr.getStatus()){//check status
			reportError(pr);
		}
		pr = importAdditionalSQL(creden);

		if(!pr.getStatus()){
			if(!createContactTracerAccount()){
				revertChanges();
				reportError(new ProcessReport());
			}
		}
		//on success showwwww
		JOptionPane.showMessageDialog(null,"You're done! You can now use the Contact Tracer App.","Finish Installing",
			JOptionPane.INFORMATION_MESSAGE);
		System.exit(0);
	}

	private String[] askCreden(String user, String caller, int option, int glRow){ //glRow = 4 is db login, glRow = 6 is new contact company
		String[] creden = {"","",""};
		JPanel inputPanel = new JPanel(new GridLayout(glRow,2,2,2));
		JTextField name = new JTextField(15);
		JTextField pass = new JTextField(15);
		JTextField addr = new JTextField(25);
		inputPanel.add(new JLabel(user));
		inputPanel.add(name);
		if(glRow==6){
			inputPanel.add(new JLabel("Address"));
			inputPanel.add(addr);
		}
		inputPanel.add(new JLabel("Password"));
		inputPanel.add(pass);

		int i = JOptionPane.showConfirmDialog(null,inputPanel, caller,
			option, JOptionPane.PLAIN_MESSAGE);

		if(i==2 && glRow==4){ //used only at the beginning
			System.exit(1);
		}else if(i==2 && glRow==6){
			String[] cre =  {"0","0","0","false"};
			return cre;
		}

		creden[0] = name.getText();
		creden[1] = pass.getText();
		if(glRow==6){
			creden[2] = addr.getText();
		}

		return creden;
	}

	private ProcessReport importDDL(String[] creden){
		//create a database
		ProcessReport pp = createDatabase(creden[0], creden[1]);
		if(!pp.getStatus())
			return pp;

		ProcessBuilder pb = new ProcessBuilder();

		if((System.getProperty("os.name").toLowerCase()).startsWith("windows")){ //windows installer
			pb.command("cmd.exe", "-c", "mysql -u "+creden[0]+" -p"+creden[1]+" coviddb < covidDDL.sql");
		}else{ //linux installer
			pb.command("/bin/bash", "-c", "mysql -u "+creden[0]+" -p"+creden[1]+" coviddb < covidDDL.sql");
		}
		pb.redirectErrorStream(true);
		try{
			Process p =	pb.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        	while (true) {
        	   String line = r.readLine();
        	    if (line == null) { break; }
        	    if(line.toLowerCase().contains("covidddl.sql: no such file")){
        	    	p.destroy();
        	    	return new ProcessReport(2, false, "Missing CovidDDL");
        	    }
        	}
		}catch(IOException ioe){
			return new ProcessReport(3, false, "cannot access shell");
		}

		return new ProcessReport(100, true, "Succes");
	}

	private ProcessReport createDatabase(String user, String pass){
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Unable to find and load driver");
            System.exit(1);
        }
        
        try {
            String url = "jdbc:mysql://localhost:/coviddb?createDatabaseIfNotExist=true";
            connection = DriverManager.getConnection(url, user, pass);
            setDBLogIn(user, pass);
        } catch (SQLException sqle) {
	        return new ProcessReport(sqle.getErrorCode(), false, "Database Error"); //get the sqle errorcode
        }
        return new ProcessReport(100, true, "Success");
	}

	//line 93 ui change to numeric 0
	private boolean createContactTracerAccount(){
		String[] ctCreden = askCreden("Contact Tracer Company", "Create contact tracer account", JOptionPane.OK_CANCEL_OPTION, 6);
		if(ctCreden.length==4 && ctCreden[3].equals("false")){
			return false;
		}

		try{
			Statement stmt = connection.createStatement();
			stmt.executeQuery("DELETE FROM establishment WHERE id=0");
		}catch(SQLException sqle){}
		try{
			String sql = "INSERT INTO establishment VALUES(0,?,?,?)";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, ctCreden[0]);
			stmt.setString(2, ctCreden[2]);
			stmt.setString(3, ctCreden[1]);
			stmt.executeUpdate();
			stmt.close();
			return true;
		}catch(SQLException sqle){
			return false;
		}
	}

	private ProcessReport importAdditionalSQL(String[] creden){
		int choice = JOptionPane.showConfirmDialog(null,"Do you want to import an existing database?","Import",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if(choice == 1)
			return new ProcessReport(5,false,"No import");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Import file");
		fileChooser.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter fnef = new FileNameExtensionFilter("Covid Files", "covid");
		fileChooser.addChoosableFileFilter(fnef);

		if(fileChooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){
			String path = fileChooser.getSelectedFile().getPath();
			//concurrent please wait
			ProcessBuilder pb = new ProcessBuilder();

			if((System.getProperty("os.name").toLowerCase()).startsWith("windows")){ //windows 
				pb.command("cmd.exe", "-c", "mysql -u "+creden[0]+" -p"+creden[1]+" coviddb < "+path);
			}else{ //linux 
				pb.command("/bin/bash", "-c", "mysql -u "+creden[0]+" -p"+creden[1]+" coviddb < "+path);
			}
			pb.redirectErrorStream(true);
			try{
				Process p =	pb.start();
			}catch(IOException ioe){
				return new ProcessReport(3, false, "cannot access shell");
			}
			//concurrency wait like after this is finished
			int i = JOptionPane.showConfirmDialog(null,"DO you want to change the Contact Tracer Account?", "Change Contact Tracer Account",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

			if(i==0)
				createContactTracerAccount();
			return new ProcessReport(100, true, "Success");
		}
		return new ProcessReport(5, false, "No Import");
	}

	private void setDBLogIn(String user, String pass){
		File file = new File("usr.ldb");

		try{
			if(!file.exists())
				file.createNewFile();

			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(user);
			bw.newLine();
			bw.write(pass);
			bw.close();
		}catch(IOException e){}
	}

	private void reportError(ProcessReport pr){
		//1=db error, 2=missing file, 3=shell error
		if(pr.getCode()==1045){ //invalid login info
			JOptionPane.showMessageDialog(null,"Invalid login info","Login Error",
			JOptionPane.ERROR_MESSAGE);
			new NCoV19Installer();
		}
		else{
			JOptionPane.showMessageDialog(null,"Cannot finish installation. Try again later.","Installation Error",
			JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}

	private void revertChanges(){
		//drop db
		try{
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("DROP DATABASE coviddb");
			stmt.close();	
		}catch(SQLException sqle){}
	}

	public static void main(String[] args){
		new NCoV19Installer();
	}
}