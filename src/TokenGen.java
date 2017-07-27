/*
TokenGen a token generator
Copyright (C) 2017 Davide Sestili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

public class TokenGen 
{
	private static Connection connection = null;
	private static final String PROP_FILE_NAME = "config.properties";
	private static String baseDir;
	
	public static void main(String[] args)
	{
		if(args.length == 2)
		{
			Integer num = null;
			try
			{
				num = new Integer(args[0]);
			}
			catch(NumberFormatException e)
			{
				e.printStackTrace();
				return;
			}

			baseDir = args[1];
			
			openConnection();

			try 
			{
				connection.setAutoCommit(false);

				for(int i = 0; i < num; i++)
				{
					/*
					if(i == 50)
					{
						throw new Exception("test transaction");
					}
					*/
					UUID uuid = UUID.randomUUID();
					System.out.println("Token: " + uuid.toString());
					saveToDB(uuid.toString());
				}
				
				connection.commit();
			} 
			catch(Exception e) 
			{
				try 
				{
					connection.rollback();
				} 
				catch(SQLException e1) 
				{
					e1.printStackTrace();
				}
				
				System.out.println("Errore in saveToDB(): " + e.getMessage());
			}
			
			closeConnection();
		}
		else
		{
			System.out.println("Usage: param 1: numer of tokens to be generated, param 2: baseDir");
		}
	}

	protected static void closeConnection()
	{
		if(connection != null)
		{
			try 
			{
				connection.close();
				connection = null;
				System.out.println("Connessione chiusa");
			} 
			catch(SQLException e) 
			{
				System.out.println("Errore di chiusura connessione: " + e.getMessage());
			}
		}
	}

	private static void saveToDB(String uuid) throws Exception
	{
		String queryInsertToken = getProperty("query.insertToken");
		PreparedStatement statement = connection.prepareStatement(queryInsertToken);
		statement.setString(1, uuid);
		statement.setInt(2, 0);
		statement.setString(3, baseDir);
		statement.executeUpdate();
	}
	
	protected static String getProperty(String key)
	{
		Properties prop = new Properties();
		InputStream input = null;
		String value = null;

		try
		{
			input = TokenGen.class.getClassLoader().getResourceAsStream(PROP_FILE_NAME);
			
			if(input == null)
			{
				System.out.println("File di properties non trovato " + PROP_FILE_NAME);
				return null;
			}

			prop.load(input);
			value = prop.getProperty(key);
		}
		catch(IOException e)
		{
			System.out.println("Errore di lettura dal file di properties: " + e.getMessage());
		}
		finally
		{
			if(input != null)
			{
				try 
				{
					input.close();
				} 
				catch(IOException e) 
				{
					System.out.println("Errore di chiusura input stream: " + e.getMessage());
				}
			}
		}
		
		return value;
	}
	
	protected static String decodeBase64(String enc)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(enc);
		return new String(decodedBytes);
	}
	
	protected static void openConnection()
	{
		if(connection == null)
		{
			try 
			{
				Class.forName("com.mysql.jdbc.Driver");
				
				String connectionString = getProperty("connectionString");
				String userName = decodeBase64(getProperty("userName"));
				String password = decodeBase64(getProperty("password"));
				
				connection = DriverManager.getConnection(connectionString, userName, password);
				System.out.println("Connessione riuscita");
			}
			catch(SQLException e) 
			{
				System.out.println("Errore di connessione: " + e.getMessage());
			} 
			catch(ClassNotFoundException e) 
			{
				System.out.println("Errore di connessione: " + e.getMessage());
			}
		}
	}
}
