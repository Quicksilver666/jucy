package eu.jucy.database;



import java.io.File;
import java.io.IOException;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


import logger.LoggerFactory;
import org.apache.log4j.Logger;


import uc.DCClient;
import uc.IUser;
import uc.User;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.crypto.TigerHashValue;
import uc.database.DBLogger;
import uc.database.IDatabase;
import uc.database.ILogEntry;
import uc.database.LogEntry;
import uc.files.downloadqueue.DQEDAO;
import uc.files.filelist.HashedFile;




public class HSQLDB implements IDatabase {

	private static Logger logger = LoggerFactory.make();

	private final boolean deletelogtables = false; //just for testing purposes..
	
	private String url;

	private File storagePath;
	private volatile Connection c;
	
	private boolean ignoreUserUpdates = false;
	
	/**
	 * entity to name .. cached here.. for speed and simplicity reasons..
	 */
	private Map<HashValue,String> knownLogEntitys = new HashMap<HashValue,String>();
	
	private DCClient dcc;
	
	public HSQLDB() {}
	
	public synchronized void init(File storagepath,DCClient dcc) throws Exception {
		this.dcc = dcc;
		this.storagePath = storagepath;

		//new org.hsqldb.jdbcDriver();
		Class.forName("org.hsqldb.jdbcDriver" );


		
		boolean allok 	= connect();

		if (!allok) {
			throw new IllegalStateException("initialization not successful");
		}
		
		if (!tableExists()) {
			createTables();
		}
		if (deletelogtables) {
			deleteLogtables();
		}
		
		if (!logTableExists()) {
			createLogTables();
			setProperties(); // in same version added as LogTables.. -> should also be moved 
		} else {
			loadLogEntitys();
		}
		
	}
	
	
	
	private void setProperties() {
		try {
			Statement s = c.createStatement();
			s.execute("SET PROPERTY \"hsqldb.cache_scale\" 8"); //minimum cache
			s.close();
			logger.debug("Set Properties sent");
		
		} catch (SQLException e) {
			logger.warn(e, e);
		}
	}
	
	public synchronized void shutdown() {
		disconnect();
	}

	private synchronized boolean connect() throws IOException, SQLException {
		boolean allok 	= true;
		File folder 	= new File( storagePath ,"db");
		
		if (!folder.exists()) {
			allok = folder.mkdirs();
		}
		File path =  new File(folder,"data.foo" );
		if (!path.isFile()) {
			allok &= path.createNewFile();
		}
		url = "jdbc:hsqldb:file:" + path;
		
		c = DriverManager.getConnection(url, "sa", "");
		
		return allok;
	}
	

	
	private synchronized void ensureConnectionIsOpen() throws SQLException {
		if (c == null || c.isClosed()) {
			try {
				connect();
			} catch (IOException ioe) {
				logger.warn(ioe, ioe);
				throw new SQLException();
			}
		}
	}
	
	 /**
	  * test if we have to create the tables.. or if they already exist
	  */
	 private synchronized boolean tableExists() {
			try {
				Statement s = c.createStatement();
				s.execute("SELECT * FROM hashes");
			} catch (SQLException e) {
				return false;
			}
			return true;
	 }
	 
	 private synchronized boolean logTableExists() {
			try {
				Statement s = c.createStatement();
				s.execute("SELECT * FROM logEntitys");
			} catch (SQLException e) {
				return false;
			}
			return true;
	 }
	 
	 public synchronized void deleteLogtables() {
		 String s2 = "DROP TABLE logs IF EXISTS" ;
		 String s1 = "DROP TABLE logEntitys IF EXISTS" ;
		 
		try {
			Statement st1 = c.createStatement();
			st1.execute(s2);
			Statement st2 = c.createStatement();
			st2.execute(s1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	 }
	 
	 private synchronized void loadLogEntitys() {
		 knownLogEntitys.clear();
		 try {
			PreparedStatement loadEntitys = 
				c.prepareStatement("SELECT * FROM logEntitys " );
				
			ResultSet rs = loadEntitys.executeQuery();
			while (rs.next()) {
					
				HashValue id = HashValue.createHash(rs.getString("entityid"));
				String name = rs.getString("name");
				knownLogEntitys.put(id, name);
				
			}
				
		} catch (SQLException e) {
			logger.warn(e, e);
		}
	 }
	 
	 private synchronized void createTables() {
		try {
			

				
			Statement createTTHtoIH = c.createStatement();
			createTTHtoIH.execute("CREATE CACHED TABLE interleaves ("+
					"tthroot CHARACTER("+TigerHashValue.serializedDigestLength+") PRIMARY KEY,"
					+ "interleaves LONGVARCHAR )");
			
			Statement s = c.createStatement();

				// creating table tths
			s.execute("CREATE CACHED TABLE hashes (" 
					+ "tthroot CHARACTER("+TigerHashValue.serializedDigestLength+"), " //the rootTTH
					     //bewares integrity.. so no files without hashes are stored..
					+ "date BIGINT, "			   // the date when the file was hashed
					+ "path VARCHAR PRIMARY KEY,  " //, + // all the tths building the merkletree
			+ "FOREIGN KEY ( tthroot ) "
			+ "REFERENCES interleaves (tthroot) ON DELETE CASCADE ) ");  
				
			Statement s2 = c.createStatement();
			//create an index on tths so we can search there faster..
			s2.execute("CREATE INDEX tthrootindex ON hashes ( tthroot ) ");
			

			Statement createDQEtable = c.createStatement();
			createDQEtable.execute("CREATE CACHED TABLE downloadqueue (" 
					
					+ "tthroot CHARACTER("+TigerHashValue.serializedDigestLength+") PRIMARY KEY,"   //the rootTTH
					+ "date BIGINT, "				// the date when the dqe was added
					+ "path VARCHAR, "				// the path where the file should be downloaded to
					+ "priority INTEGER, " 			//	
					+ "size BIGINT"
					+ " )");
			
			Statement createUserTable = c.createStatement();
			createUserTable.execute("CREATE CACHED TABLE users ("  //subject to change
					
					+ "userid  CHARACTER("+TigerHashValue.serializedDigestLength+") PRIMARY KEY ,"// the id of the user
					+ "nick VARCHAR ,"
					+ "favuser BOOLEAN DEFAULT FALSE , "
					+ "autoGrant BIGINT DEFAULT 0"
					+ " )");
			
			Statement createDQEToUsertable = c.createStatement();
			createDQEToUsertable.execute("CREATE CACHED TABLE dqeToUser (" 
					
					+ "tthroot CHARACTER("+TigerHashValue.serializedDigestLength+"), " //the rootTTH of the dqe
					+ "userid  CHARACTER("+TigerHashValue.serializedDigestLength+"), "  // the id of the user
					+ "PRIMARY KEY ( tthroot , userid ) ,"
					+ "FOREIGN KEY ( userid ) "
					+ " REFERENCES users (userid) ON DELETE CASCADE ,"
					+ "FOREIGN KEY ( tthroot ) "
					+ " REFERENCES downloadqueue (tthroot) ON DELETE CASCADE "
					+ " )");
			

				
		} catch (SQLException e) {
			logger.warn("Couldn't create SQL table " + e.toString(),e);
		}
	 }
	 
	 private void createLogTables() {
		 try {
				Statement createLogentityTable = c.createStatement();
				createLogentityTable.execute("CREATE CACHED TABLE logEntitys (" 
						
						+ "entityid CHARACTER("+TigerHashValue.serializedDigestLength+"), " 
						+ "name VARCHAR,"
						+ "PRIMARY KEY ( entityid) "
						+ " )");
				
			
			Statement createLogtable = c.createStatement();
			createLogtable.execute("CREATE CACHED TABLE logs (" 
					
					+ "entityid CHARACTER("+TigerHashValue.serializedDigestLength+"), " 
					+ "timestamp  BIGINT, " 
					+ "message VARCHAR,"
					+ "FOREIGN KEY ( entityid ) "
					+ " REFERENCES logEntitys (entityid) ON DELETE CASCADE "
					+ " )");
			
			Statement index = c.createStatement();
			//create an index on TimeStamps so we can search there faster..
			index.execute("CREATE INDEX timestampindex ON logs ( timestamp ) ");
			
			
			
			Statement index2 = c.createStatement();
			//create an index on TTHs so we can search there faster..
			index2.execute("CREATE INDEX entityidindex ON logs ( entityid ) ");

			
			
			
			
		} catch (SQLException e) {
			logger.warn("Couldn't create SQL table " + e.toString(),e);
		}
	 }
	
	
	 public synchronized void disconnect() {
		 if (c == null) { //no connection -> no disconnect
			 return;
		 }
		try {
			Statement sd = c.createStatement();
			sd.execute("SHUTDOWN");
			c.close();
		} catch (SQLException e) {
			logger.warn( "Disconnecting failed " + e.toString(),e);
		}
	 }
	 
	 /**
	  * @param file - the file 
	  * @param inter  the interleave hashes
	  * @param Date  the time when it was hashed...
	  */
	 
	 public synchronized void addOrUpdateFile(File file, HashValue tth, InterleaveHashes inter, Date hashed) {
		
		 if (file.exists()) {
			 try {
				 ensureConnectionIsOpen();
				 
				 addOrUpdateInterleave(tth,inter); //first add interleaves..
				 
				 PreparedStatement updateFile = 
					 c.prepareStatement("UPDATE hashes SET tthroot = ? , date = ? WHERE path = ? " );
				 updateFile.setString(1, tth.toString());
				 updateFile.setLong(2, hashed.getTime());
				 updateFile.setString(3, file.getAbsolutePath());
				 
				 int count = updateFile.executeUpdate();
				 
				 
				 logger.debug("updated File: "+count);
				 
				 updateFile.close();
				 
				 if (logger.isDebugEnabled()) {
					 PreparedStatement updateFile2 = 
						 c.prepareStatement("Select * FROM hashes WHERE tthroot = ? " );

				 	updateFile2.setString(1, tth.toString());
				 	ResultSet rs = updateFile2.executeQuery();
				 	while (rs.next()) {
				 		logger.debug("fount item: "+rs.getString("path")+"  : "+rs.getLong("date"));
				 	}
				 }				 
				 
				 if (count == 0) {
					 PreparedStatement addFile = 
						 c.prepareStatement("INSERT INTO hashes (tthroot, date, path) VALUES ( ?, ?, ?) ");
					 
					 addFile.setString(1, tth.toString());
					 addFile.setLong(2, hashed.getTime());
					 addFile.setString(3, file.getAbsolutePath());
					 
					 addFile.execute();
					 addFile.close();
				 }

			} catch (SQLException e) {
				logger.warn( "addOrUpdateFile failed " + e.toString(),e);
			}
			
		 }
	 }
	 
	 
	 private void addOrUpdateInterleave(HashValue tth , InterleaveHashes inter) throws SQLException {
		 
		 PreparedStatement checkExist = c.prepareStatement("SELECT 1 FROM interleaves WHERE tthroot = ? ");
		 checkExist.setString(1, tth.toString());
		 ResultSet rs = checkExist.executeQuery();
		 if (!rs.next()) {
			 PreparedStatement insertInterleaves = c.prepareStatement( 
					 "INSERT INTO interleaves (tthroot, interleaves ) VALUES ( ?, ?) ");
			 insertInterleaves.setString(1, tth.toString());
			 insertInterleaves.setString(2, inter.toString());
			 
			 insertInterleaves.execute();
			 insertInterleaves.close();
		 }
		 checkExist.close();
		 
		 
	 }

	 
	 public synchronized void addUpdateOrDeleteUser(IUser usr) {
		 if (!ignoreUserUpdates) {
			 try {
				 ensureConnectionIsOpen();
				 if (usr.shouldBeStored()) {
					 logger.debug("adding user "+usr.getNick()+" "+usr.getAutograntSlot()+" "+usr.isFavUser());
					 PreparedStatement updateUsr = 
						 c.prepareStatement("UPDATE users SET  nick = ? , favuser = ? , autoGrant = ? WHERE userid = ?"  );
					 updateUsr.setString(1, usr.getNick()); //Set
					 updateUsr.setBoolean(2, usr.isFavUser());
					 updateUsr.setLong(3, usr.getAutograntSlot());
					 updateUsr.setString(4, usr.getUserid().toString()); // Update Where
					 
					 int count = updateUsr.executeUpdate();
					 updateUsr.close();

					 if (count == 0) { //user not found --> insert
						 PreparedStatement addUser = c.prepareStatement(
								 "INSERT INTO users (userid, nick,favuser,autoGrant) VALUES (?,?,?,?) ");
						 
						 addUser.setString(1, usr.getUserid().toString()); //Insert
						 addUser.setString(2, usr.getNick());
						 addUser.setBoolean(3, usr.isFavUser());
						 addUser.setLong(4, usr.getAutograntSlot());
						 
						 addUser.execute();
						 addUser.close();
					 }
					 
				 } else {
					 logger.debug("deleting user "+usr.getNick());
					 PreparedStatement deleteUser = c.prepareStatement(
							 "DELETE FROM users WHERE userid = ? "
							 );
					 deleteUser.setString(1, usr.getUserid().toString());
					 
					 deleteUser.execute();
					 deleteUser.close();
					 
					 
				 }
			 
			 } catch (SQLException sqle) {
				 logger.warn(sqle,sqle);
			 }
		 }
	 }
	 
	 
	 public synchronized void addUserToDQE(IUser usr,HashValue hash) {
		 HashValue tth = hash;
		 if (tth == null) {
			 throw new IllegalArgumentException("DQE has no TTH");
		 }
		 HashValue userid = usr.getUserid();
		 try {
			 ensureConnectionIsOpen();
			 logger.debug("1added user: "+usr.getNick()+" to DQE: "+tth);
			// getNrOfEntrysInUserToDQETable();
			 
			 PreparedStatement checkExist = c.prepareStatement("SELECT * FROM dqeToUser WHERE tthroot = ? AND userid = ? ");
			 checkExist.setString(1, tth.toString());
			 checkExist.setString(2, userid.toString());
			 ResultSet rs = checkExist.executeQuery();
			 logger.debug("2added user: "+usr.getNick()+" to DQE: "+tth);
			 if (!rs.next()) {
				 PreparedStatement insertMapping = 
					 c.prepareStatement("INSERT INTO dqeToUser (tthroot, userid) VALUES ( ?, ?) ");
				 insertMapping.setString(1, tth.toString());
				 insertMapping.setString(2, userid.toString());
				 insertMapping.execute();
				 logger.debug("3added user: "+usr.getNick()+" to DQE: "+tth);
			 }
			 checkExist.close();
			 logger.debug("4added user: "+usr.getNick()+" to DQE: "+tth);
			// getNrOfEntrysInUserToDQETable();
			 
		 } catch(SQLException sqle) {
			 logger.warn(sqle,sqle);
		 }
	 }
	 
	 private void addDQE(HashValue tth,Date added, File target, int priority, long size) throws SQLException  {
		 try {
			 PreparedStatement addDQE = c.prepareStatement(
					 "INSERT INTO downloadqueue (tthroot, date, path, priority, size)"
					 +" VALUES ( ? , ? , ? , ? , ? ) " );
			 addDQE.setString(1, tth.toString()); //Insert
			 addDQE.setLong(2,  added.getTime());
			 addDQE.setString(3, target.getAbsolutePath());
			 addDQE.setInt(4, priority);
			 addDQE.setLong(5, size);
			 addDQE.execute(); 
			 addDQE.close();
		 } catch (SQLException sqle) {
			 if (sqle.toString().contains("Violation of unique constraint")) {
				 updateDQE(tth,added, target, priority, size);
				 logger.debug(sqle,sqle);
			 } else {
				 throw sqle;
			 }
		 }
	 }
	 
	 
	 private void updateDQE(HashValue tth,Date added, File target, int priority, long size) throws SQLException  {
		 int count = 0;
		 PreparedStatement updateDQE = 
			 c.prepareStatement("UPDATE downloadqueue SET "
					 +" date = ? , path = ? , priority = ? , size = ? WHERE tthroot = ? " );
		 
		 updateDQE.setLong(1, added.getTime());   //Set
		 updateDQE.setString(2, target.getAbsolutePath());
		 updateDQE.setInt(3, priority);
		 updateDQE.setLong(4, size);
		 
		 updateDQE.setString(5, tth.toString()); //Update where
		 
		 count = updateDQE.executeUpdate();
		 updateDQE.close();
		 if (count == 0) {
			 addDQE(tth,added, target, priority, size);
		 }
	 }
	 
	 private void addOrUpdateDQE(HashValue tth,Date added, File target, int priority, long size,boolean add) throws SQLException {
		logger.debug("HSQL: adding dqe "+target);  
		if (add) {
			 addDQE(tth,added, target, priority, size);
		} else {
			updateDQE(tth,added, target, priority, size);
		}
	/*	
		int count = 0;
		if (!add) {
		 PreparedStatement updateDQE = 
			 c.prepareStatement("UPDATE downloadqueue SET "
					 +" date = ? , path = ? , priority = ? , size = ? WHERE tthroot = ? " );
		 
		 updateDQE.setLong(1, added.getTime());   //Set
		 updateDQE.setString(2, target.getAbsolutePath());
		 updateDQE.setInt(3, priority);
		 updateDQE.setLong(4, size);
		 
		 updateDQE.setString(5, tth.toString()); //Update where
		 
		 count = updateDQE.executeUpdate();
		 updateDQE.close();
		}
		 logger.debug("dqe found # times: "+count);
		 if (count == 0) {
			 PreparedStatement addDQE = c.prepareStatement(
					 "INSERT INTO downloadqueue (tthroot, date, path, priority, size)"
					 +" VALUES ( ? , ? , ? , ? , ? ) " );
			 addDQE.setString(1, tth.toString()); //Insert
			 addDQE.setLong(2,  added.getTime());
			 addDQE.setString(3, target.getAbsolutePath());
			 addDQE.setInt(4, priority);
			 addDQE.setLong(5, size);
			 addDQE.execute(); 
			 addDQE.close();
			 
			 logger.debug("dqe added");
		 } */
	
		 
	 }
	 
	 private synchronized void getNrOfEntrysInUserToDQETable() {
		 if (logger.isDebugEnabled()) {
			 try {
				 Statement s = c.createStatement();
				 ResultSet rs = s.executeQuery("Select * FROM dqeToUser");
				 int count = 0;
				 while (rs.next()) {
					 count++;
				 }
				 logger.debug("currently Found Mappings: "+count);
			 } catch(SQLException sqle) {
					logger.warn(sqle,sqle);
			}
		 }
	 }
	 
	 
	 public synchronized void deleteUserFromDQE(IUser usr,DQEDAO dqe ) {
		 logger.debug("deleting user from dqe: "+usr.getNick());
		 getNrOfEntrysInUserToDQETable();
			try {
				ensureConnectionIsOpen();
				
				PreparedStatement deleteUserFromDQE = c.prepareStatement(
						 "DELETE FROM dqeToUser WHERE tthroot = ? AND userid = ? "
						 );
				deleteUserFromDQE.setString(1, dqe.getTTHRoot().toString());
				deleteUserFromDQE.setString(2, usr.getUserid().toString());
				 
				if (!deleteUserFromDQE.execute()) {
					logger.debug("deleting user from dqe: "+usr.getNick()+"  count: "+deleteUserFromDQE.getUpdateCount());
				}
				deleteUserFromDQE.close();
				 
			} catch(SQLException sqle) {
				logger.warn(sqle,sqle);
			}
			logger.debug("finished deleting user from dqe: "+usr.getNick());
			getNrOfEntrysInUserToDQETable();
	 }
	 
	 
	 
	
	public synchronized void addOrUpdateDQE(DQEDAO dqe,boolean add) {
		logger.debug("changing dqe: "+dqe);

		try {
			ensureConnectionIsOpen();
			
			addOrUpdateDQE(dqe.getTTHRoot(),dqe.getAdded(),dqe.getTarget(),dqe.getPriority(),dqe.getSize(),add);
			
			if (dqe.getIh() != null) {
				addOrUpdateInterleave(dqe.getTTHRoot() , dqe.getIh());
			}
			
			
		} catch(SQLException  sqle) {
			logger.warn(sqle,sqle);
		}
	}
	
	
	public synchronized void deleteDQE(DQEDAO dqe) {
		logger.debug("deleting dqe: "+dqe.getName());
		try {
			ensureConnectionIsOpen();
			
			 PreparedStatement deleteDQE = c.prepareStatement(
					 "DELETE FROM downloadqueue WHERE tthroot = ? ");
			 
			 deleteDQE.setString(1, dqe.getTTHRoot().toString());
			 
			 deleteDQE.execute();
			 deleteDQE.close();
		 
		} catch(SQLException sqle) {
			logger.warn(sqle,sqle);
		}
	}
	
	
	/**
	 * loads the DownloadqueueEntrys persistent data..
	 * as a side effects all stored users are loaded..
	 */
	public synchronized Set<DQEDAO> loadDQEsAndUsers() {
		
		logger.debug("in loadDQEsAndUsers()");
		Map<HashValue,User> users = new HashMap<HashValue,User>();
		
		Map<HashValue,DQEDAO> dqes = new HashMap<HashValue,DQEDAO>();
		
		try {
			//load users ..
			PreparedStatement prepst = 
				c.prepareStatement("SELECT * FROM users " );
			
			ResultSet rs = prepst.executeQuery();
			
			ignoreUserUpdates = true;
			while (rs.next()) {
				HashValue userid = HashValue.createHash(rs.getString("userid"));
				String nick = rs.getString("nick");
				boolean favuser = rs.getBoolean("favuser");
				long autoGrant = rs.getLong("autoGrant");
				
				User usr = dcc.getPopulation().get(nick, userid);
				usr.setFavUser(favuser); 
				usr.setAutograntSlot(autoGrant);
				users.put(userid, usr);
				logger.debug("loaded user: "+usr.getNick()+" "+usr.getAutograntSlot()+" "+usr.isFavUser());
			}
			prepst.close();
			ignoreUserUpdates = false;
			logger.debug("in loadDQEsAndUsers() found x Users: "+users.size());
			
			/*
			table [{CROSS | INNER | LEFT OUTER | RIGHT OUTER}
		    JOIN table ON Expression] [, ...]
			*/
			PreparedStatement dqesstm = 
				c.prepareStatement("SELECT downloadqueue.* , interleaves.interleaves " 
						+ " FROM downloadqueue LEFT OUTER JOIN interleaves ON  downloadqueue.tthroot =  interleaves.tthroot " 
						+ " WHERE downloadqueue.tthroot = interleaves.tthroot");
			
			ResultSet rs2 = dqesstm.executeQuery();
			
			while (rs2.next()) {
				HashValue tthRoot = HashValue.createHash(rs2.getString("tthroot"));
				Date added =  new Date(rs2.getLong("date"));
				File path = new File(rs2.getString("path") );
				int priority = rs2.getInt("priority");
				String inter = rs2.getString("interleaves");
				long size  = rs2.getLong("size");
				InterleaveHashes ih = null;
				if (inter != null && inter.length() != 0) {
					ih = new InterleaveHashes(inter);
				}
		
				DQEDAO dqedao = new DQEDAO(tthRoot,added,priority,path,ih,size); 
				
				dqes.put(tthRoot, dqedao);
			}
			logger.debug("in loadDQEsAndUsers() found x DQEs: "+dqes.size());
			dqesstm.close();
			
			 //as last action load the mapping from user to dqes..
			
			PreparedStatement mappings =  c.prepareStatement( "SELECT * FROM dqeToUser");
			ResultSet rs3 = mappings.executeQuery();

			while (rs3.next()) {
				HashValue tthRoot = HashValue.createHash(rs3.getString("tthroot"));
				HashValue userid = HashValue.createHash(rs3.getString("userid"));
				User usr = users.get(userid);
				DQEDAO dao =  dqes.get(tthRoot);
				if (usr != null && dao != null) {
					dao.addUser(usr);
					logger.debug("in loadDQEsAndUsers() found Mapping: "+usr.getNick() +" to "+dao.getName());
				}
			}
			
			mappings.close();

		
		} catch(SQLException e) {
			logger.warn(e,e);
		}
		
		ignoreUserUpdates = false;
		
		for (User usr : users.values()) {
			addUpdateOrDeleteUser(usr);
		}

		return new HashSet<DQEDAO>(dqes.values());
		
	}
	
	


	
	public  InterleaveHashes getInterleaves(HashValue tthroot) {
		if (tthroot == null) {
			throw new IllegalArgumentException("tthroot is null in HSQL.getInterleaves()");
		}
		InterleaveHashes ih = null;
		
		try {
		
			PreparedStatement prepst = 
				c.prepareStatement("SELECT * FROM interleaves WHERE tthroot = ? " );
			prepst.setString(1, tthroot.toString());
			
			ResultSet rs = prepst.executeQuery();
			
			if (rs.next()) {
				ih = new InterleaveHashes(rs.getString("interleaves"));
			}
			prepst.close();
			
		} catch (SQLException e) {
			logger.warn(e,e);
		}
		
		return ih;
	}
	

	
	
	public synchronized void deleteAllHashedFiles() {
		try {
			Statement stm = c.createStatement();
			stm.execute("DELETE FROM hashes");
			stm.close();
		} catch(SQLException e) {
			logger.warn(e,e);
		}
	}

	
	public synchronized Map<File,HashedFile> getAllHashedFiles() {
		Map<File,HashedFile> files = new HashMap<File,HashedFile>();
		try {
			ensureConnectionIsOpen();
			PreparedStatement prepst = 
				c.prepareStatement("SELECT * FROM hashes" );
			ResultSet rs = prepst.executeQuery();
			
			while (rs.next()) { //tthroot , date , path
				long date = rs.getLong("date");
				String path = rs.getString("path");
				String tthRoot = rs.getString("tthroot");
				
				HashedFile h = new HashedFile(new Date(date),HashValue.createHash(tthRoot), new File(path));
				
				if ( null !=  files.put(h.getPath(), h)) {
					//little hack ... will remove files already present.. (fixes problems with changed case on case insensitive file-system)
					
					PreparedStatement remove = 
						c.prepareStatement("DELETE FROM hashes WHERE tthroot = ?");
					remove.setString(1, tthRoot);
				
					logger.debug("removing File: "+remove.executeUpdate());
					remove.close();
					files.remove(h.getPath());
				}
			}
			
			
			prepst.close();
			
		} catch (SQLException e) {
			logger.warn(e,e);
		}
		
		return files;
	}
	
	
	
	
	public Map<File, HashedFile> pruneUnusedHashedFiles() {
		int count =0;
		Map<File,HashedFile> files = getAllHashedFiles();
		try {
			Iterator<File> it = files.keySet().iterator();
			while (it.hasNext()) {
				File f = it.next();
				if (f.isFile()) {
					it.remove();
				} else {
					PreparedStatement remove = 
						c.prepareStatement("DELETE FROM hashes WHERE path = ? ");
					remove.setString(1, f.getAbsolutePath());
					count += remove.executeUpdate();
					/*if (count == 0) {
						
						PreparedStatement lookdebug = 
							c.prepareStatement("Select * FROM hashes WHERE tthroot = ? ");
						lookdebug.setString(1,  files.get(f).getTTHRoot().toString());
						
						ResultSet rs = lookdebug.executeQuery();
						while (rs.next()) {
							String path = rs.getString("path")+"  date: "+rs.getLong("date");
							logger.info("Path in rs: "+ path);
						}
				
						*
						PreparedStatement update = 
							c.prepareStatement("UPDATE hashes SET tthroot = ? , date = ? WHERE path LIKE ? " );
				
						update.setLong(2, 100);
						update.setString(3, f.getAbsolutePath()); *
						
						logger.info("Updated in total for deletion: ");
						
						
						
					}  */
					remove.close();
					
				}
			}
			//logger.warn("Totally changed files: "+count);
			return files;
		
		} catch (SQLException e) {
			logger.warn(e,e);
		}
		
		
		
		return null;
	}

	public  void addLogEntry(ILogEntry logentry) {
		try {
			ensureConnectionIsOpen();
			if (!knownLogEntitys.containsKey(logentry.getEntityID())) {
				PreparedStatement addLogentity = c.prepareStatement(
						 "INSERT INTO logEntitys (entityid, name)"
						 +" VALUES ( ? , ? ) " );
				addLogentity.setString(1,logentry.getEntityID().toString());
				addLogentity.setString(2,logentry.getName());
				
				addLogentity.execute();
				addLogentity.close();
				
				knownLogEntitys.put(logentry.getEntityID(), logentry.getName());
			}
			
			PreparedStatement addLogentry = c.prepareStatement(
					 "INSERT INTO logs (entityid, timestamp, message)"
					 +" VALUES ( ? , ? , ? ) " );
			addLogentry.setString(1,logentry.getEntityID().toString());
			addLogentry.setLong(2,  logentry.getDate());
			addLogentry.setString(3,logentry.getMessage());
			
			addLogentry.execute();
			addLogentry.close();
		} catch (SQLException e) {
			logger.warn(e,e);
		}
	}

	public List<ILogEntry> getLogentrys(HashValue entityID, int max,int offset) {
		if (max <= 0) {
			throw new IllegalArgumentException();
		}
		if (offset < 0) {
			offset = 0;
		}
		
		try {
			ensureConnectionIsOpen();
			
			PreparedStatement prepst = 
				c.prepareStatement("SELECT timestamp , message FROM logs WHERE entityid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?" ); 
			
			prepst.setString(1, entityID.toString());
			prepst.setInt(2, max);
			prepst.setInt(3, offset);
			
			ResultSet rs = prepst.executeQuery();
			
			String name = knownLogEntitys.get(entityID);
			
			List<ILogEntry> logs = new ArrayList<ILogEntry>();
			
			while (rs.next() /*&&  --max >= 0 */) {
				String message = rs.getString("message");
				long date = rs.getLong("timestamp");
				logs.add(new LogEntry(date,entityID,message,name));
			}
			prepst.close();
			
			return logs;
			
		} catch (SQLException e) {
			logger.warn(e,e);
		}
		return Collections.emptyList();
	}
	
	

	public  int countLogentrys(HashValue entityID) {
		try {
			ensureConnectionIsOpen();
			PreparedStatement prepst;
			if (entityID != null) {
				prepst = c.prepareStatement("SELECT COUNT(*) FROM logs WHERE entityid = ? " ); 
				prepst.setString(1, entityID.toString());
			} else {
				prepst = c.prepareStatement("SELECT COUNT(*) FROM logs " ); 
			}
			ResultSet rs = prepst.executeQuery();
			
			int count = 0;
			
			if (rs.next()) {
				count = rs.getInt(1);
			}
			prepst.close();
			
			
			return count;
			
		} catch (SQLException e) {
			logger.warn(e,e);
		}
		return 0;
	}

	public synchronized List<DBLogger> getLogentitys() {
		List<DBLogger> loggers = new ArrayList<DBLogger>();
		
		for (Entry<HashValue,String> entity: knownLogEntitys.entrySet()) {
			loggers.add(new DBLogger(entity.getValue(),entity.getKey(),this));
		}
		return loggers;
	}

	
	public void pruneLogentrys(HashValue entityID, Date before) {
		if (before == null) {
			throw new IllegalArgumentException("no date provided");
		}
	
		try {
			ensureConnectionIsOpen();
		
			if (entityID == null) {	
				PreparedStatement stm = c.prepareStatement("DELETE FROM logs WHERE timestamp < ? ");
				stm.setLong(1, before.getTime());
				stm.executeUpdate();
				stm.close();
			} else {
				PreparedStatement stm = c.prepareStatement("DELETE FROM logs WHERE entityid = ? AND timestamp < ? ");
				stm.setString(1, entityID.toString());
				stm.setLong(2, before.getTime());
				stm.executeUpdate();
				stm.close();
			}
			PreparedStatement stm2 = c.prepareStatement(
					"DELETE FROM logEntitys WHERE NOT EXISTS" 
				+	"( SELECT * FROM logs WHERE logs.entityid = logEntitys.entityid ) ");
		
			if (stm2.executeUpdate() > 0) {
				loadLogEntitys();
			}
			stm2.close();
		
		} catch (SQLException e) {
			logger.warn(e,e);
		} 
	}
	
	
}
