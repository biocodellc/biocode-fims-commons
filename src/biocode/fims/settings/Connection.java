package biocode.fims.settings;

import biocode.fims.fimsExceptions.FimsRuntimeException;

import java.io.File;

/**
 * Represents a Database connection, can be used to generate a JDBC URL,
 * make a new D2RQ Database or generate a D2RQ Mapping entry.
 */
public class Connection {
	public DBsystem system;
	public String host;
	public String database;
	public String password;
	public String username;

	/**
	 * For construction from JSON.
	 */
    public Connection() {}

	/**
	 * Create Connection to a sqlite file, where
	 * Database = file name, host = path
	 */
	public Connection(File sqliteFile) {
		system = DBsystem.sqlite;
		host = sqliteFile.getParent().replace("\\", "/" );
		database = sqliteFile.getName();

	}
	
	/**
	 * Generate a JDBC URL specific to DBsystem.
	 * 
     * @return JDBC URL.
	 */
	public String getJdbcUrl() {
		switch(system) {
			case mysql:
				return "jdbc:mysql://" + host + "/" + database;
			case postgresql:
				return "jdbc:postgresql://" + host + "/" + database;
			case oracle:
				return "jdbc:oracle:thin:@" + host + ":" + database;
			case sqlserver:
				return "jdbc:sqlserver://" + host + ";databaseName=" + database;
			case sqlite:
				return "jdbc:sqlite:" + host + "/" + database;
		}
		return null;
	}
	
    /**
     * For SQLite DBsystems verify if the file exists 
     * in local filesystem, throw exception if not.
     *
     */
	public void verifyFile() {
		if (system.equals(DBsystem.sqlite) && !new File(host + File.separator + database).exists())
			throw new FimsRuntimeException("Data Source file not available.", 500);
	}
	
}
