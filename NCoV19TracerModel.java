//package NCoV19TracerApp;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class NCoV19TracerModel{
	
    Connection connection;
    PreparedStatement stmt;

	//Constructor
	public NCoV19TracerModel(){
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Unable to find and load driver");
            System.exit(1);
        }
        
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost/covidDB",
                    "samson", "t325gh9QR*");
        } catch (SQLException sqlerr) {
            System.out.println(sqlerr.getSQLState()+":"+sqlerr.getErrorCode());
        }
        System.out.println("Connected Successfully");
	}

    public void insertData(String[] input){
        if(!personExists(Integer.parseInt(input[1]))){//add new person to person relation
            try{
                String sql = "INSERT INTO person VALUES(?,?,?,?,?)";
                stmt = connection.prepareStatement(sql);
                stmt.setInt(1, Integer.parseInt(input[1])); //id
                stmt.setString(2, input[0]); //name
                stmt.setString(3, input[2]);//address
                stmt.setInt(4, Integer.parseInt(input[3]));//age
                stmt.setInt(5, Integer.parseInt(input[4]));//contact #
            
                stmt.executeUpdate();
                stmt.close();
            }catch(SQLException sqle){
                sqle.getMessage();
                sqle.printStackTrace();
            }
        }

        //add to the visited relation
        try{
            String sql = "INSERT INTO visited VALUES(?,?,?,?,null)";
            stmt = connection.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(input[1]));//person_id
            stmt.setInt(2, Integer.parseInt(input[7]));//establishment_id
            stmt.setDate(3, java.sql.Date.valueOf(input[6]));//make sure date is converted right
            stmt.setTime(4, java.sql.Time.valueOf(input[5]));//make sure time is converted right
            
            stmt.executeUpdate(); 
            stmt.close();
        }catch(SQLException sqle){
            sqle.getMessage();
                sqle.printStackTrace();
        }
    }

    public void updateData(int id, String t_Out){
        try{
            String sql = "UPDATE visited SET time_out = ? WHERE person_id ="+id;
            stmt = connection.prepareStatement(sql);
            stmt.setTime(1, java.sql.Time.valueOf(t_Out));
            
            stmt.executeUpdate(); 
            stmt.close();
        }catch(SQLException sqle){
            sqle.getMessage();
                sqle.printStackTrace();
        }
    }

    public void deleteData(){}

    public void regEst(String[] input){
        if(!estabExists(Integer.parseInt(input[0]))){
             try{
                String sql = "INSERT INTO establishment VALUES(?,?,?,?)";
                stmt = connection.prepareStatement(sql);
                stmt.setInt(1, Integer.parseInt(input[0]));
                stmt.setString(2, input[1]);
                stmt.setString(3, input[2]);
                stmt.setString(4, input[3]);
                
                stmt.executeUpdate(); 
                stmt.close();
            }catch(SQLException sqle){
                sqle.getMessage();
                sqle.printStackTrace();
            }
            JOptionPane.showMessageDialog(null,"Establishment registered successfully",
                    "Registration Success", JOptionPane.INFORMATION_MESSAGE);
        }else{
            JOptionPane.showMessageDialog(null,"Establishment already exists.",
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JTable getTable(int type, int id, String from, String to){
        String query;
        javax.swing.JTable table = null;

        if(type==0){ //1st level
                query = "SELECT person.name, contact_no, address FROM visited, person WHERE person.id=visited.person_id AND visited.establishment_id IN (SELECT establishment_id FROM visited WHERE person_id= ? AND date BETWEEN ? AND ? )  AND date BETWEEN ? AND ? ;";
            }else if(type==1){ //2 level contacts
                query = "SELECT person.name, contact_no, address FROM visited, person WHERE person.id=visited.person_id AND visited.establishment_id IN (SELECT establishment_id FROM visited WHERE person_id IN (SELECT person.id FROM visited, person WHERE person.id=visited.person_id AND visited.establishment_id IN (SELECT establishment_id FROM visited WHERE person_id= ?  AND date BETWEEN ? AND ?) AND date BETWEEN ? AND ? ));";
            }else{ //establishment visited
                query = "SELECT establishment.id AS ID, name AS VISITED, address AS ADDRESS FROM establishment, visited WHERE visited.establishment_id=establishment.id AND visited.person_id = ? AND date BETWEEN ? AND ? ;";
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
            sqle.getMessage();
            sqle.printStackTrace();
            //add catch for empty JTable
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
            sqle.getMessage();
            sqle.printStackTrace();
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
            sqle.getMessage();
            sqle.printStackTrace();
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