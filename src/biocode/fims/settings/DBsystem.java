package biocode.fims.settings;

/**
 * Enumerates supported Database systems with their JDBC drivers.
 * Needs the driver jars in path.
 */
public enum DBsystem {
	sqlite ("org.sqlite.JDBC"), 
	mysql ("com.mysql.jdbc.Driver"), 
	postgresql ("org.postgresql.Driver"), 
	oracle ("oracle.jdbc.OracleDriver"), 
	sqlserver ("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	
	public final String driver;
	
	DBsystem(String driver) {
		this.driver = driver;
	}

}
