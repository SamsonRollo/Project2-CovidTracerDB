//package NCoV19TracerApp;

import java.sql.*;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;
import java.io.*;
import java.io.FileNotFoundException;

public class NCoV19TracerModel{
	
	NCoV19TracerController cont;
    Connection connection;
    PreparedStatement stmt;

    //Constructor
	public NCoV19TracerModel(NCoV19TracerController cont){ // didid na igtest if mayda connection
		this.cont = cont;
		String[] dBlogin = getDBLogin();
		initConnection(dBlogin[0], dBlogin[1]);
	}

	public NCoV19TracerModel(NCoV19TracerController cont, String user, String pass){
		this.cont = cont;
		initConnection(user, pass);
	}

	private void initConnection(String user, String pass){
		System.out.println("Connecting to Database.......");
		if(cont.getCode()==1045)
			setDBLogIn(user, pass);

		try{
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Unable to find and load driver");
            System.exit(1);
        }
        
        try {
            String url = "jdbc:mysql://localhost:/coviddb";
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected Successfully");
            cont.setCode(200);
        } catch (SQLException sqle) {
            System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
             if(sqle.getErrorCode()==1045){ //wrong credentials
	        	System.out.println("No login info!");
	          	cont.setCode(1045);
	        }
        }
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

	private String[] getDBLogin(){
		String[] dBlogin ={"",""};
		try{
			BufferedReader bf = new BufferedReader(new FileReader(new File("usr.ldb")));
			String st;
			int i = 0;
			while((st = bf.readLine())!=null || i<2)
				dBlogin[i++] = st;
			bf.close();
		}catch(Exception e){}

		return dBlogin;
	}

    public boolean insertData(String[] input){
        int id;
        try{
            id = Integer.parseInt(input[1]);
        }catch(NumberFormatException nfe){
            return false;
        }
        if(!personExists(id)){//add new person to person relation
            try{
                String sql = "INSERT INTO person VALUES(?,?,?,?,?)";
                stmt = connection.prepareStatement(sql);
                stmt.setInt(1, id); //id
                stmt.setString(2, input[0]); //name
                stmt.setString(3, input[2]);//address
                stmt.setInt(4, Integer.parseInt(input[3]));//age
                stmt.setString(5, input[4]);//contact #
            
                stmt.executeUpdate();
                stmt.close();
            }catch(SQLException sqle){
                System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
                return false;
            }
        }
        //add to the visited relation
        try{
            String sql = "INSERT INTO visited VALUES(?,?,?,?,null)";
            stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);//person_id
            stmt.setInt(2, Integer.parseInt(input[7]));//establishment_id
            stmt.setDate(3, java.sql.Date.valueOf(input[6]));//make sure date is converted right
            stmt.setTime(4, java.sql.Time.valueOf(input[5]));//make sure time is converted right
            
            stmt.executeUpdate(); 
            stmt.close();
        }catch(SQLException sqle){
            System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
            return false;
        }
        return true;
    }

    public boolean updateData(int id, String t_Out){
        try{
            String sql = "UPDATE visited SET time_out = ? WHERE person_id ="+id;
            stmt = connection.prepareStatement(sql);
            stmt.setTime(1, java.sql.Time.valueOf(t_Out));
            
            stmt.executeUpdate(); 
            stmt.close();
        }catch(SQLException sqle){
            System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
            return false;
        }
        return true;
    }

    public boolean deleteData(){
        return false;
    }

    public boolean regEst(String[] input){
        try{
            int id = Integer.parseInt(input[0]);
            if(!estabExists(id)){
                 try{
                    String sql = "INSERT INTO establishment VALUES(?,?,?,?)";
                    stmt = connection.prepareStatement(sql);
                    stmt.setInt(1, id);
                    stmt.setString(2, input[1]);
                    stmt.setString(3, input[2]);
                    stmt.setString(4, input[3]);
                    
                    stmt.executeUpdate(); 
                    stmt.close();
                    return true; //success
                }catch(SQLException sqle){
                    System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
                }
            }
        }catch(NumberFormatException nfe){
            System.out.println("Invalid ID");
        }
        return false; //all failed
    }

    public JTable getTable(int type, int id, String from, String to){
        String query;
        javax.swing.JTable table = null;

        if(type==0){ //1st level
                query = "SELECT DISTINCT person.id AS ID, person.name, contact_no, address FROM visited, person WHERE person.id=visited.person_id AND visited.establishment_id IN (SELECT establishment_id FROM visited WHERE person_id= ? AND date BETWEEN ? AND ? )  AND date BETWEEN ? AND ? ORDER BY address, person.name;";
            }else if(type==1){ //2 level contacts
                query = "SELECT DISTINCT person.id AS ID, person.name, contact_no, address FROM visited, person WHERE person.id=visited.person_id AND visited.establishment_id IN (SELECT establishment_id FROM visited WHERE person_id IN (SELECT person.id FROM visited, person WHERE person.id=visited.person_id AND visited.establishment_id IN (SELECT establishment_id FROM visited WHERE person_id= ?  AND date BETWEEN ? AND ?) AND date BETWEEN ? AND ? )) ORDER BY address, person.name;";
            }else{ //establishment visited
                query = "SELECT DISTINCT establishment.id AS ID, name AS VISITED, address AS ADDRESS FROM establishment, visited WHERE visited.establishment_id=establishment.id AND visited.person_id = ? AND date BETWEEN ? AND ? ORDER BY name, address;";
            }
            
        try{
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, id);
            stmt.setDate(2, java.sql.Date.valueOf(from));
            stmt.setDate(3, java.sql.Date.valueOf(to));
            if(type!=2){
                stmt.setDate(4, java.sql.Date.valueOf(from));
                stmt.setDate(5, java.sql.Date.valueOf(to));
            }
            ResultSet rg = stmt.executeQuery();
            
            table = new javax.swing.JTable(buildTable(rg)){
                public boolean isCellEditable(int row, int column){
                    return false;
                }
            };
        }catch(SQLException sqle){
            System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
        }
        return table;
    }

    private DefaultTableModel buildTable(ResultSet rs) throws SQLException{
        ResultSetMetaData rsm = rs.getMetaData();
        int colCount = rsm.getColumnCount();
        Vector<String> colNames = new Vector<String>();
        Vector<Vector<Object>> rowData =  new Vector<Vector<Object>>();

        for(int i = 1; i<=colCount; i++) //list all the column names from the query output
            colNames.add(rsm.getColumnName(i));

        while(rs.next()){
            Vector<Object> data = new Vector<Object>();
            for(int colIndex = 1; colIndex<=colCount; colIndex++)
                data.add(rs.getObject(colIndex));
            rowData.add(data);
        }
        return new DefaultTableModel(rowData, colNames);
    }

    //check if person exists in the db
    public boolean personExists(int id){
        return exists("SELECT * FROM person WHERE id="+id);
    }

    public boolean personExists(String name){
        return exists("SELECT * FROM person WHERE name='"+name+"'");
    }

    public boolean estabExists(int id){
        return exists("SELECT * from establishment where name= "+id);
    }

    public boolean isIn(int id){
        return exists("SELECT * from visited where person_id= "+id+" AND date= '"+
                Date.valueOf(java.time.LocalDate.now())+"' AND time_out is null;");
    }

    private boolean exists(String query){
        try{
            Statement st = connection.createStatement();
            ResultSet rg = st.executeQuery(query);

            if(rg.next())
                return true;
        }catch(SQLException sqle){
            System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
        }
        return false;
    }

     public int verifyLogin(String name, String pass){
        return getID("SELECT * from establishment where name= '"+name+"' AND password= '"+pass+"'");
    }
    //atomize with estab exists
    public int getPersonID(String name){
        return getID("SELECT * from person where name= '"+name+"' LIMIT 1;");
    }

    private int getID(String query){
        try{
            Statement st = connection.createStatement();
            ResultSet rg = st.executeQuery(query);

            if(rg.next())
                return rg.getInt("id");
        }catch(SQLException sqle){
            System.out.println(sqle.getSQLState()+":"+sqle.getErrorCode());
        }
        return 0;
    }

    public Connection getConnection(){
        return connection;
    }

	public static void main(String[] args){
		NCoV19TracerUI ui = new NCoV19TracerUI();
        ui.setVisible(true);
	}
}