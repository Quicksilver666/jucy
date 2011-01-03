package eu.jucy.database;

import helpers.GH;
import helpers.LockedRunnable;

import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import logger.LoggerFactory;
import org.apache.log4j.Logger;

import uc.DCClient;
import uc.IUser;
import uc.crypto.BASE32Encoder;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.crypto.TigerHashValue;
import uc.database.DBLogger;
import uc.database.HashedFile;
import uc.database.IDatabase;
import uc.database.ILogEntry;
import uc.database.LogEntry;
import uc.files.downloadqueue.DQEDAO;
import uc.user.User;

public class HSQLDB implements IDatabase {

	private static Logger logger = LoggerFactory.make();

	private static final int MAX_INTERLEAVESIZE = 1024*1024; 
	
	private final boolean deletelogtables = false; // just for testing
													// purposes..

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();
	
	private String url;

	private File storagePath;
	private volatile Connection c;

	private volatile boolean ignoreUserUpdates = false;

	/**
	 * entity to name .. cached here.. for speed and simplicity reasons..
	 */
	private Map<HashValue, String> knownLogEntities = Collections.synchronizedMap(new HashMap<HashValue, String>());

	private DCClient dcc;

	public HSQLDB() {
	}

	public void init(File storagepath, DCClient dcc) throws Exception {
		writeLock.lock();
		try {
			if (this.dcc != null) {
				throw new IllegalStateException();
			}
			this.dcc = dcc;
			this.storagePath = storagepath;
	
			Class.forName("org.hsqldb.jdbcDriver");
	
			boolean allok = connect();
	
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
				// setProperties(); // in same version added as LogTables.. ->
				// should also be moved
			} else {
				loadLogEntitys();
			}
			
			if (!restoreTableExists()) {
				createRestoreTable();
			}
		} finally {
			writeLock.unlock();
		}

	}

	// private void setProperties() {
	// try {
	// Statement s = c.createStatement();
	// s.execute("SET PROPERTY \"hsqldb.cache_scale\" 8"); //minimum cache
	// s.close();
	// logger.debug("Set Properties sent");
	//
	// } catch (SQLException e) {
	// logger.warn(e, e);
	// }
	// }

	public void shutdown() {
		disconnect();
	}

	private boolean connect() throws IOException, SQLException {
		boolean allok = true;
		File folder = new File(storagePath, "db");

		if (!folder.exists()) {
			allok = folder.mkdirs();
		}
		String oldPrefix = "data.foo";
		String newPrefix = "jucydb";
		File newPath = new File(folder, newPrefix);
		File oldPath = new File(folder, oldPrefix);
		// if (!oldPath.isFile()) {
		// allok &= oldPath.createNewFile();
		// }
		if (!newPath.isFile()) {
			if (oldPath.isFile()) {
				for (File f : folder.listFiles()) {
					if (f.isFile() && f.getName().startsWith(oldPrefix)) {
						GH.copy(f, new File(f.getParentFile(), f.getName()
								.replace(oldPrefix, newPrefix)));
					}
				}
			} else {
				allok &= newPath.createNewFile();
			}
		}

		url = "jdbc:hsqldb:file:" + newPath;
		try {
			c = DriverManager.getConnection(url, "sa", "");
		} catch (SQLException sqle) {
			if (sqle.toString().contains(
					"old version database must be shutdown")) {
				logger.warn("Old db can not be opened (no proper shutdown on update )-> deleting db so jucy can at least start up");
				for (File f : folder.listFiles()) { // DELEting db so next start
													// could work...
					if (f.isFile() && f.getName().startsWith(newPrefix)) {
						if (!f.delete()) {
							f.deleteOnExit();
						}
					}
					newPath.createNewFile();
				}
			}
			throw sqle;
		}
		return allok;
	}

	private void ensureConnectionIsOpen() throws SQLException {
		
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
	private boolean tableExists() {
		try {
			Statement s = c.createStatement();
			s.execute("SELECT 1 FROM hashes");
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	private boolean logTableExists() {
		try {
			Statement s = c.createStatement();
			s.execute("SELECT 1 FROM logEntitys");
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	private void deleteLogtables() {
		String s2 = "DROP TABLE logs IF EXISTS";
		String s1 = "DROP TABLE logEntitys IF EXISTS";

		try {
			Statement st1 = c.createStatement();
			st1.execute(s2);
			st1.close();
			Statement st2 = c.createStatement();
			st2.execute(s1);
			st2.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadLogEntitys() {
		knownLogEntities.clear();
		try {
			PreparedStatement loadEntitys = c
					.prepareStatement("SELECT * FROM logEntitys ");

			ResultSet rs = loadEntitys.executeQuery();
			while (rs.next()) {

				HashValue id = HashValue.createHash(rs.getString("entityid"));
				String name = rs.getString("name");
				knownLogEntities.put(id, name);

			}

		} catch (SQLException e) {
			logger.warn(e, e);
		} 
	}

	private void createTables() {
		try {

			Statement createTTHtoIH = c.createStatement();
			createTTHtoIH.execute("CREATE CACHED TABLE interleaves ("
					+ "tthroot CHARACTER("
					+ TigerHashValue.serializedDigestLength + ") PRIMARY KEY,"
					+ "interleaves LONGVARCHAR )");
			createTTHtoIH.close();

			Statement s = c.createStatement();

			// creating table tths
			s.execute("CREATE CACHED TABLE hashes ("
					+ "tthroot CHARACTER("+ TigerHashValue.serializedDigestLength+ ") " 
					+ " ,date BIGINT " 
					+ ", path VARCHAR(32767) PRIMARY KEY "
					+ ", FOREIGN KEY ( tthroot ) "
					+ "REFERENCES interleaves (tthroot) ON DELETE CASCADE ) ");

			s.close();
			Statement s2 = c.createStatement();
			// create an index on tths so we can search there faster..
			s2.execute("CREATE INDEX tthrootindex ON hashes ( tthroot ) ");

			s2.close();
			
			Statement createDQEtable = c.createStatement();
			createDQEtable.execute("CREATE CACHED TABLE downloadqueue ("

			+ "tthroot CHARACTER(" + TigerHashValue.serializedDigestLength
					+ ") PRIMARY KEY," // the rootTTH
					+ "date BIGINT, " // the date when the dqe was added
					+ "path VARCHAR(32767), " // the path where the file should
												// be downloaded to
					+ "priority INTEGER, " //
					+ "size BIGINT" + " )");

			createDQEtable.close();
			
			Statement createUserTable = c.createStatement();
			createUserTable.execute("CREATE CACHED TABLE users (" // subject to
																	// change

					+ "userid  CHARACTER("
					+ TigerHashValue.serializedDigestLength
					+ ") PRIMARY KEY ,"// the id of the user
					+ "nick VARCHAR(1024) ,"
					+ "favuser BOOLEAN DEFAULT FALSE , "
					+ "autoGrant BIGINT DEFAULT 0" + " )");
			
			createUserTable.close();

			Statement createDQEToUsertable = c.createStatement();
			createDQEToUsertable.execute("CREATE CACHED TABLE dqeToUser ("

					+ "tthroot CHARACTER("
					+ TigerHashValue.serializedDigestLength
					+ "), " // the rootTTH of the dqe
					+ "userid  CHARACTER("
					+ TigerHashValue.serializedDigestLength
					+ "), " // the id of the user
					+ "PRIMARY KEY ( tthroot , userid ) ,"
					+ "FOREIGN KEY ( userid ) "
					+ " REFERENCES users (userid) ON DELETE CASCADE ,"
					+ "FOREIGN KEY ( tthroot ) "
					+ " REFERENCES downloadqueue (tthroot) ON DELETE CASCADE "
					+ " )");
			createDQEToUsertable.close();
		} catch (SQLException e) {
			logger.warn("Couldn't create SQL table " + e.toString(), e);
		}
	}

	private void createLogTables() {
		try {
			Statement createLogentityTable = c.createStatement();
			createLogentityTable.execute("CREATE CACHED TABLE logEntitys ("

					+ "entityid CHARACTER(" + TigerHashValue.serializedDigestLength+ "), " 
					+ "name VARCHAR(1024),"
					+ "PRIMARY KEY ( entityid) " + " )");

			createLogentityTable.close();
			
			Statement createLogtable = c.createStatement();
			createLogtable.execute("CREATE CACHED TABLE logs ("

			+ "entityid CHARACTER(" + TigerHashValue.serializedDigestLength
					+ "), " + "timestamp  BIGINT, " + "message VARCHAR("
					+ ILogEntry.MAX_MESSAGELENGTH + "),"
					+ "FOREIGN KEY ( entityid ) "
					+ " REFERENCES logEntitys (entityid) ON DELETE CASCADE "
					+ " )");
			createLogtable.close();

			Statement index = c.createStatement();
			// create an index on TimeStamps so we can search there faster..
			index.execute("CREATE INDEX timestampindex ON logs ( timestamp ) ");
			index.close();
			
			Statement index2 = c.createStatement();
			// create an index on TTHs so we can search there faster..
			index2.execute("CREATE INDEX entityidindex ON logs ( entityid ) ");
			
			index2.close();

		} catch (SQLException e) {
			logger.warn("Couldn't create SQL table " + e.toString(), e);
		}
	}
	
	private boolean restoreTableExists() {
		try {
			Statement s = c.createStatement();
			s.execute("SELECT 1 FROM dqrestoreinfo");
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	private void createRestoreTable() {
		try {
			Statement restoreInfo = c.createStatement();
			restoreInfo.execute("CREATE CACHED TABLE dqrestoreinfo ("
					+ "tthroot CHARACTER("+ TigerHashValue.serializedDigestLength+ "),"
					+ "restoreInfo VARCHAR(32767),"
					+ "PRIMARY KEY ( tthroot ),"
					+ "FOREIGN KEY ( tthroot ) "
					+ "REFERENCES downloadqueue (tthroot) ON DELETE CASCADE )"
			);
			
		} catch (SQLException e) {
			logger.warn("Couldn't create SQL table " + e.toString(), e);
		}
	}
	
	

	public void disconnect() {
		writeLock.lock();
		try {
			if (c == null) { // no connection -> no disconnect
				return;
			}
			Statement sd = c.createStatement();
			sd.execute("SHUTDOWN");
			c.close();
		} catch (SQLException e) {
			logger.warn("Disconnecting failed " + e.toString(), e);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param file
	 *            - the file
	 * @param inter
	 *            the interleave hashes
	 * @param Date
	 *            the time when it was hashed...
	 */

	public void addOrUpdateFile(HashedFile hf,
			InterleaveHashes inter) {

		if (hf.getPath().exists()) {
			writeLock.lock();
			try {
				ensureConnectionIsOpen();

				addOrUpdateInterleave(hf.getTTHRoot(), inter); // first add
																// interleaves..

				PreparedStatement updateFile = c
						.prepareStatement("UPDATE hashes SET tthroot = ? , date = ? WHERE path = ? ");
				updateFile.setString(1, hf.getTTHRoot().toString());
				updateFile.setLong(2, hf.getLastChanged().getTime());
				updateFile.setString(3, hf.getPath().getAbsolutePath());

				int count = updateFile.executeUpdate();

				logger.debug("updated File: " + count);

				updateFile.close();

				

				if (count == 0) {
					PreparedStatement addFile = c
							.prepareStatement("INSERT INTO hashes (tthroot, date, path) VALUES ( ?, ?, ?) ");

					addFile.setString(1, hf.getTTHRoot().toString());
					addFile.setLong(2, hf.getLastChanged().getTime());
					addFile.setString(3, hf.getPath().getAbsolutePath());

					addFile.execute();
					addFile.close();
				}
				
				if (logger.isDebugEnabled()) {
					PreparedStatement updateFile2 = c
							.prepareStatement("SELECT * FROM hashes WHERE tthroot = ? ");

					updateFile2.setString(1, hf.getTTHRoot().toString());
					ResultSet rs = updateFile2.executeQuery();
					while (rs.next()) {
						File f = new File( rs.getString("path"));
						logger.debug("found item: " + f
								+ "  : " + rs.getLong("date")+ "  "+f.equals(hf.getPath()));
					}
				}

			} catch (SQLException e) {
				logger.warn("addOrUpdateFile failed " + e.toString()
						+ " interleaves length: " + inter.toString().length(),
						e);
			} finally {
				writeLock.unlock();
			}

		}
	}

	private void addOrUpdateInterleave(HashValue tth, InterleaveHashes inter)
			throws SQLException {

		PreparedStatement checkExist = c
				.prepareStatement("SELECT 1 FROM interleaves WHERE tthroot = ? ");
		checkExist.setString(1, tth.toString());
		ResultSet rs = checkExist.executeQuery();
		if (!rs.next()) {
			PreparedStatement insertInterleaves = c
					.prepareStatement("INSERT INTO interleaves (tthroot, interleaves ) VALUES ( ?, ?) ");
			insertInterleaves.setString(1, tth.toString());

			String s;
			while ((s = inter.toString()).length() > MAX_INTERLEAVESIZE) { 
				inter = inter.getParentInterleaves();
				logger.warn("To long interleave provided for DB root: "+tth);
			}
			insertInterleaves.setString(2, s);

			insertInterleaves.execute();
			insertInterleaves.close();
		}
		checkExist.close();

	}

	public void addUpdateOrDeleteUser(IUser usr) {
		if (!ignoreUserUpdates) {
			writeLock.lock();
			try {
				ensureConnectionIsOpen();
				if (usr.shouldBeStored()) {
					logger.debug("adding user " + usr.getNick() + " "
							+ usr.getAutograntSlot() + " " + usr.isFavUser());
					PreparedStatement updateUsr = c
							.prepareStatement("UPDATE users "
									+ " SET  nick = ? , favuser = ? , autoGrant = ? "
									+ " WHERE userid = ?");
					
					updateUsr.setString(1, usr.getNick()); // Set
					updateUsr.setBoolean(2, usr.isFavUser());
					updateUsr.setLong(3, usr.getAutograntSlot());
					updateUsr.setString(4, usr.getUserid().toString()); // Update
																		// Where

					int count = updateUsr.executeUpdate();
					updateUsr.close();

					if (count == 0) { // user not found --> insert
						PreparedStatement addUser = c
								.prepareStatement("INSERT INTO users "
												+ "(userid, nick,favuser,autoGrant) "
												+ " VALUES (?,?,?,?) ");

						addUser.setString(1, usr.getUserid().toString()); // Insert
						addUser.setString(2, usr.getNick());
						addUser.setBoolean(3, usr.isFavUser());
						addUser.setLong(4, usr.getAutograntSlot());

						addUser.execute();
						addUser.close();
					}

				} else {
					logger.debug("deleting user " + usr.getNick());
					PreparedStatement deleteUser = c
							.prepareStatement("DELETE FROM users WHERE userid = ? ");
					deleteUser.setString(1, usr.getUserid().toString());

					deleteUser.execute();
					deleteUser.close();

				}

			} catch (SQLException sqle) {
				logger.warn(sqle, sqle);
			} finally {
				writeLock.unlock();
			}
		}
	}

	public void addUserToDQE(IUser usr, HashValue hash) {
		HashValue tth = hash;
		if (tth == null) {
			throw new IllegalArgumentException("DQE has no TTH");
		}
		HashValue userid = usr.getUserid();
		writeLock.lock();
		try {
			ensureConnectionIsOpen();
			logger.debug("1added user: " + usr.getNick() + " to DQE: " + tth);
			// getNrOfEntrysInUserToDQETable();

			PreparedStatement checkExist = c
					.prepareStatement("SELECT 1 FROM dqeToUser WHERE tthroot = ? AND userid = ? ");
			checkExist.setString(1, tth.toString());
			checkExist.setString(2, userid.toString());
			ResultSet rs = checkExist.executeQuery();
			logger.debug("2added user: " + usr.getNick() + " to DQE: " + tth);
			if (!rs.next()) {
				rs.close();
				PreparedStatement insertMapping = c
						.prepareStatement("INSERT INTO dqeToUser (tthroot, userid) VALUES ( ?, ?) ");
				insertMapping.setString(1, tth.toString());
				insertMapping.setString(2, userid.toString());
				insertMapping.execute();
				logger.debug("3added user: " + usr.getNick() + " to DQE: "
						+ tth);
			}
			checkExist.close();
			logger.debug("4added user: " + usr.getNick() + " to DQE: " + tth);
			// getNrOfEntrysInUserToDQETable();

		} catch (SQLException sqle) {
			logger.warn(sqle, sqle);
		} finally {
			writeLock.unlock();
		}
	}

	private void addDQE(HashValue tth, Date added, File target, int priority,
			long size) throws SQLException {
		try {
			PreparedStatement addDQE = c
					.prepareStatement("INSERT INTO downloadqueue (tthroot, date, path, priority, size)"
							+ " VALUES ( ? , ? , ? , ? , ? ) ");
			addDQE.setString(1, tth.toString()); // Insert
			addDQE.setLong(2, added.getTime());
			addDQE.setString(3, target.getAbsolutePath());
			addDQE.setInt(4, priority);
			addDQE.setLong(5, size);
			addDQE.execute();
			addDQE.close();
		} catch (SQLException sqle) {
			if (sqle.toString().contains("Violation of unique constraint")) {
				updateDQE(tth, added, target, priority, size);
				logger.debug(sqle, sqle);
			} else {
				throw sqle;
			}
		}
	}

	private void updateDQE(HashValue tth, Date added, File target,
			int priority, long size) throws SQLException {
		int count = 0;
		PreparedStatement updateDQE = c
				.prepareStatement("UPDATE downloadqueue SET "
						+ " date = ? , path = ? , priority = ? , size = ? WHERE tthroot = ? ");

		updateDQE.setLong(1, added.getTime()); // Set
		updateDQE.setString(2, target.getAbsolutePath());
		updateDQE.setInt(3, priority);
		updateDQE.setLong(4, size);

		updateDQE.setString(5, tth.toString()); // Update where

		count = updateDQE.executeUpdate();
		updateDQE.close();
		if (count == 0) {
			addDQE(tth, added, target, priority, size);
		}
	}

	private void addOrUpdateDQE(HashValue tth, Date added, File target,
			int priority, long size, boolean add) throws SQLException {
		logger.debug("HSQL: adding dqe " + target);
		if (add) {
			addDQE(tth, added, target, priority, size);
		} else {
			updateDQE(tth, added, target, priority, size);
		}
		/*
		 * int count = 0; if (!add) { PreparedStatement updateDQE =
		 * c.prepareStatement("UPDATE downloadqueue SET "
		 * +" date = ? , path = ? , priority = ? , size = ? WHERE tthroot = ? "
		 * );
		 * 
		 * updateDQE.setLong(1, added.getTime()); //Set updateDQE.setString(2,
		 * target.getAbsolutePath()); updateDQE.setInt(3, priority);
		 * updateDQE.setLong(4, size);
		 * 
		 * updateDQE.setString(5, tth.toString()); //Update where
		 * 
		 * count = updateDQE.executeUpdate(); updateDQE.close(); }
		 * logger.debug("dqe found # times: "+count); if (count == 0) {
		 * PreparedStatement addDQE = c.prepareStatement(
		 * "INSERT INTO downloadqueue (tthroot, date, path, priority, size)"
		 * +" VALUES ( ? , ? , ? , ? , ? ) " ); addDQE.setString(1,
		 * tth.toString()); //Insert addDQE.setLong(2, added.getTime());
		 * addDQE.setString(3, target.getAbsolutePath()); addDQE.setInt(4,
		 * priority); addDQE.setLong(5, size); addDQE.execute(); addDQE.close();
		 * 
		 * logger.debug("dqe added"); }
		 */

	}

	private void getNrOfEntrysInUserToDQETable() {
		if (logger.isDebugEnabled()) {
			readLock.lock();
			try {
				Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM dqeToUser");
				int count = 0;
				while (rs.next()) {
					count++;
				}
				logger.debug("currently Found Mappings: " + count);
			} catch (SQLException sqle) {
				logger.warn(sqle, sqle);
			} finally {
				readLock.unlock();
			}
		}
	}

	public void deleteUserFromDQE(IUser usr, DQEDAO dqe) {
		logger.debug("deleting user from dqe: " + usr.getNick());
		getNrOfEntrysInUserToDQETable();
		writeLock.lock();
		try {
			ensureConnectionIsOpen();
			PreparedStatement deleteUserFromDQE = c
					.prepareStatement("DELETE FROM dqeToUser WHERE tthroot = ? AND userid = ? ");
			deleteUserFromDQE.setString(1, dqe.getTTHRoot().toString());
			deleteUserFromDQE.setString(2, usr.getUserid().toString());

			if (!deleteUserFromDQE.execute()) {
				logger.debug("deleting user from dqe: " + usr.getNick()
						+ "  count: " + deleteUserFromDQE.getUpdateCount());
			}
			deleteUserFromDQE.close();

		} catch (SQLException sqle) {
			logger.warn(sqle, sqle);
		} finally {
			writeLock.unlock();
		}
		logger.debug("finished deleting user from dqe: " + usr.getNick());
		getNrOfEntrysInUserToDQETable();
	}
	
	/**
	 * 		if (dqe.getRestoreInfo() != null) {
			String restoreInfo = BASE32Encoder.encode(
					GH.toBytes(dqe.getRestoreInfo(),dqe.getIh().hashValuesSize()));
			addRestoreInfo(dqe.getTTHRoot(), restoreInfo);
		}
	 * 
	 * 
	 * @param hash
	 * @param restoreInfo
	 * @param size
	 */
	public void addRestoreInfo(HashValue hash,BitSet restoreInfo) {
		writeLock.lock();
		try {
			ensureConnectionIsOpen();
			PreparedStatement addRestoreInfo = c
			.prepareStatement("INSERT INTO dqrestoreinfo (tthroot, restoreInfo)"
					+ " VALUES (?,?) ");
			
			addRestoreInfo.setString(1, hash.toString());
			
			addRestoreInfo.setString(2, BASE32Encoder.encode(
					GH.toBytes(restoreInfo)));
			
			addRestoreInfo.execute();
			addRestoreInfo.close();
			
		} catch (SQLException sqle) {
			logger.warn(sqle, sqle);
		} finally  {
			writeLock.unlock();
		}
	}

	public void addOrUpdateDQE(DQEDAO dqe, boolean add) {
		logger.debug("changing dqe: " + dqe);

		writeLock.lock();
		try {
			ensureConnectionIsOpen();
			addOrUpdateDQE(dqe.getTTHRoot(), dqe.getAdded(), dqe.getTarget(),
					dqe.getPriority(), dqe.getSize(), add);

			if (dqe.getIh() != null) {
				addOrUpdateInterleave(dqe.getTTHRoot(), dqe.getIh());
			}
			

		} catch (SQLException sqle) {
			logger.warn(sqle, sqle);
		} finally {
			writeLock.unlock();
		}
	}
	
	

	public void deleteDQE(DQEDAO dqe) {
		logger.debug("deleting dqe: " + dqe.getName());
		writeLock.lock();
		try {
			ensureConnectionIsOpen();
			PreparedStatement deleteDQE = c
					.prepareStatement("DELETE FROM downloadqueue WHERE tthroot = ? ");

			deleteDQE.setString(1, dqe.getTTHRoot().toString());
			deleteDQE.execute();
			deleteDQE.close();

		} catch (SQLException sqle) {
			logger.warn(sqle, sqle);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * loads the DownloadqueueEntrys persistent data.. as a side effects all
	 * stored users are loaded..
	 */
	public Set<DQEDAO> loadDQEsAndUsers() {

		logger.debug("in loadDQEsAndUsers()");
		Map<HashValue, User> users = new HashMap<HashValue, User>();

		Map<HashValue, DQEDAO> dqes = new HashMap<HashValue, DQEDAO>();
		writeLock.lock();
		try {
			// load users ..
			PreparedStatement prepst = c
					.prepareStatement("SELECT * FROM users ");

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
				logger.debug("loaded user: " + usr.getNick() + " "
						+ usr.getAutograntSlot() + " " + usr.isFavUser());
			}
			prepst.close();
			ignoreUserUpdates = false;
			logger.debug("in loadDQEsAndUsers() found x Users: " + users.size());

			PreparedStatement dqeRestoreInfo = c.prepareStatement("SELECT * FROM dqrestoreinfo");
			ResultSet restoreRes = dqeRestoreInfo.executeQuery();
			HashMap<HashValue,BitSet> restoreInfos = new HashMap<HashValue,BitSet>();
			
			while (restoreRes.next()) {
				HashValue tthRoot = HashValue.createHash(restoreRes
						.getString("tthroot"));
				
				String restoreData = restoreRes.getString("restoreInfo");
				if (restoreData != null) {
					byte[] bits = BASE32Encoder.decode(restoreData);
					BitSet restoreBitSet = GH.toSet(bits);
					restoreInfos.put(tthRoot, restoreBitSet);
					logger.debug("loaded restoreinfo for: "+tthRoot );
				}
			}
			dqeRestoreInfo.close();
			
			
			PreparedStatement dqesstm = c
					.prepareStatement("SELECT downloadqueue.* , interleaves.interleaves  "
							+ " FROM downloadqueue LEFT OUTER JOIN interleaves ON  downloadqueue.tthroot = interleaves.tthroot "
								);

			ResultSet rs2 = dqesstm.executeQuery();

			while (rs2.next()) {
				HashValue tthRoot = HashValue.createHash(rs2
						.getString("tthroot"));
				Date added = new Date(rs2.getLong("date"));
				File path = new File(rs2.getString("path"));
				int priority = rs2.getInt("priority");
				long size = rs2.getLong("size");

				String inter = rs2.getString("interleaves");
				InterleaveHashes ih = null;
				if (!GH.isNullOrEmpty(inter)) {
					ih = new InterleaveHashes(inter);
				}
				
				

				DQEDAO dqedao = new DQEDAO(tthRoot, added, priority, path, ih,
						size,restoreInfos.get(tthRoot));

				dqes.put(tthRoot, dqedao);

			}
			logger.debug("in loadDQEsAndUsers() found x DQEs: " + dqes.size());
			dqesstm.close();

			// as last action load the mapping from user to dqes..

			PreparedStatement mappings = c
					.prepareStatement("SELECT * FROM dqeToUser");
			ResultSet rs3 = mappings.executeQuery();

			while (rs3.next()) {
				HashValue tthRoot = HashValue.createHash(rs3
						.getString("tthroot"));
				HashValue userid = HashValue
						.createHash(rs3.getString("userid"));
				User usr = users.get(userid);
				DQEDAO dao = dqes.get(tthRoot);
				if (usr != null && dao != null) {
					dao.addUser(usr);
					logger.debug("in loadDQEsAndUsers() found Mapping: "
							+ usr.getNick() + " to " + dao.getName());
				}
			}

			mappings.close();
			Statement deleteRestoreInfo = c.createStatement();
			deleteRestoreInfo.execute("DELETE FROM dqrestoreinfo");
			deleteRestoreInfo.close();
		} catch (SQLException e) {
			logger.warn(e, e);
		} finally {
			writeLock.unlock();
		}

		ignoreUserUpdates = false;

		for (User usr : users.values()) {
			addUpdateOrDeleteUser(usr);
		}

		return new HashSet<DQEDAO>(dqes.values());
	}
	


	public InterleaveHashes getInterleaves(HashValue tthroot) {
		if (tthroot == null) {
			throw new IllegalArgumentException(
					"tthroot is null in HSQL.getInterleaves()");
		}
		InterleaveHashes ih = null;
		
		readLock.lock();
		try {

			PreparedStatement prepst = c
					.prepareStatement("SELECT interleaves FROM interleaves WHERE tthroot = ? ");
			prepst.setString(1, tthroot.toString());

			ResultSet rs = prepst.executeQuery();

			if (rs.next()) {
				ih = new InterleaveHashes(rs.getString("interleaves"));
			}
			prepst.close();

		} catch (SQLException e) {
			logger.warn(e, e);
		} finally {
			readLock.unlock();
		}

		return ih;
	}

	public void deleteAllHashedFiles() {
		writeLock.lock();
		try {
			Statement stm = c.createStatement();
			stm.execute("DELETE FROM hashes");
			stm.close();
		} catch (SQLException e) {
			logger.warn(e, e);
		} finally {
			writeLock.unlock();
		}
	}

	public Map<File, HashedFile> getAllHashedFiles() {
		Map<File, HashedFile> files = new HashMap<File, HashedFile>();
		Map<File, HashedFile> removeFiles = new HashMap<File, HashedFile>();
		readLock.lock();
		try {
			PreparedStatement prepst = c
					.prepareStatement("SELECT tthroot ,date ,path FROM hashes");
			ResultSet rs = prepst.executeQuery();

			while (rs.next()) { // tthroot , date , path
				long date = rs.getLong("date");
				String path = rs.getString("path");
				String tthRoot = rs.getString("tthroot");

				HashedFile h = new HashedFile(new Date(date), HashValue
						.createHash(tthRoot), new File(path));

				if (null != files.put(h.getPath(), h)) {
					// little hack ... will remove files already present..
					// (fixes problems with changed case on case insensitive
					// file-system)
					removeFiles.put(h.getPath(), h);
					
				}
			}
			prepst.close();

		} catch (SQLException e) {
			logger.error(e, e);
		} finally {
			readLock.unlock();
		}
		if (!removeFiles.isEmpty()) {
			writeLock.lock();
			try {
				for (HashedFile h:removeFiles.values()) {
					PreparedStatement remove = c
					.prepareStatement("DELETE FROM hashes WHERE tthroot = ?");
					remove.setString(1, h.getTTHRoot().toString());
	
					logger.debug("removing File: " + remove.executeUpdate());
					remove.close();
					files.remove(h.getPath());
				}
			} catch (SQLException e) {
				logger.error(e, e);
			} finally {
				writeLock.unlock();
			}
		}

		return files;
	}

	public HashedFile getHashedFile(File f) {
		readLock.lock();
		try {
			PreparedStatement prepst = c
					.prepareStatement("SELECT date,  tthroot FROM hashes WHERE path = ? ");
			prepst.setString(1, f.getAbsolutePath());
			ResultSet rs = prepst.executeQuery();

			while (rs.next()) { 
				long date = rs.getLong("date");
				String path = f.getAbsolutePath();
				String tthRoot = rs.getString("tthroot");

				HashedFile h = new HashedFile(new Date(date), HashValue
						.createHash(tthRoot), new File(path));
				
				prepst.close();
				return h;
			}
		} catch (SQLException e) {
			logger.error(e, e);
		} finally {
			readLock.unlock();
		}
		return null;
	}

	public Map<File, HashedFile> pruneUnusedHashedFiles() {
		int count = 0;
		Map<File, HashedFile> files = getAllHashedFiles();
		
		try {
			Iterator<File> it = files.keySet().iterator();
			while (it.hasNext()) {
				File f = it.next();
				if (f.isFile()) {
					it.remove();
				} else {
					writeLock.lock();
					try {
						PreparedStatement remove = c
								.prepareStatement("DELETE FROM hashes WHERE path = ? ");
						remove.setString(1, f.getAbsolutePath());
						count += remove.executeUpdate();
						remove.close();
					} finally {
						writeLock.unlock();
					}
				}
			}
			// logger.warn("Totally changed files: "+count);
			return files;

		} catch (SQLException e) {
			logger.warn(e, e);
		}

		return null;
	}

	public void addLogEntry(final ILogEntry logentry) {
		
		dcc.executeDir(new LockedRunnable(writeLock) {
			public void lockedRun() {
				try {
					if (!knownLogEntities.containsKey(logentry.getEntityID())) {
						PreparedStatement addLogentity = c
						.prepareStatement("INSERT INTO logEntitys (entityid, name)"
								+ " VALUES ( ? , ? ) ");
						addLogentity.setString(1, logentry.getEntityID().toString());
						addLogentity.setString(2, logentry.getName());

						addLogentity.execute();
						addLogentity.close();

						knownLogEntities.put(logentry.getEntityID(), logentry.getName());
					}

					PreparedStatement addLogentry = c
					.prepareStatement("INSERT INTO logs (entityid, timestamp, message)"
							+ " VALUES ( ? , ? , ? ) ");
					addLogentry.setString(1, logentry.getEntityID().toString());
					addLogentry.setLong(2, logentry.getDate());
					addLogentry.setString(3, logentry.getMessage());

					addLogentry.execute();
					addLogentry.close();
				} catch (SQLException e) {
					logger.warn(e, e);
				} 
			}
		});
		
	
	}

	public List<ILogEntry> getLogentrys(HashValue entityID,int max, int offset) {
		if (max <= 0) {
			throw new IllegalArgumentException();
		}
		if (offset < 0) {
			offset = 0;
		}
		readLock.lock();
		try {

			PreparedStatement prepst = c
					.prepareStatement("SELECT timestamp , message FROM logs "
										+ " WHERE entityid = ? " 
										+ " ORDER BY timestamp DESC LIMIT ? OFFSET ?");

			prepst.setString(1, entityID.toString());
			prepst.setInt(2, max);
			prepst.setInt(3, offset);

			ResultSet rs = prepst.executeQuery();

			String name = knownLogEntities.get(entityID);

			List<ILogEntry> logs = new ArrayList<ILogEntry>();

			while (rs.next() /* && --max >= 0 */) {
				String message = rs.getString("message");
				long date = rs.getLong("timestamp");
				logs.add(new LogEntry(date, entityID, message, name));
			}
			prepst.close();

			return logs;

		} catch (SQLException e) {
			logger.warn(e, e);
		} finally {
			readLock.unlock();
		}
		return Collections.emptyList();
	}

	public int countLogentrys(HashValue entityID) {
		readLock.lock();
		try {
			PreparedStatement prepst;
			if (entityID != null) {
				prepst = c
						.prepareStatement("SELECT COUNT(*) FROM logs WHERE entityid = ? ");
				prepst.setString(1, entityID.toString());
			} else {
				prepst = c.prepareStatement("SELECT COUNT(*) FROM logs ");
			}
			ResultSet rs = prepst.executeQuery();

			int count = 0;

			if (rs.next()) {
				count = rs.getInt(1);
			}
			prepst.close();

			return count;

		} catch (SQLException e) {
			logger.warn(e, e);
		} finally {
			readLock.unlock();
		}
		return 0;
	}
	
	@Override
	public List<DBLogger> getLogentitys() {
		List<DBLogger> loggers = new ArrayList<DBLogger>();

		synchronized (knownLogEntities) {
			for (Entry<HashValue, String> entity : knownLogEntities.entrySet()) {
				loggers.add(new DBLogger(entity.getValue(), entity.getKey(), this));
			}
		}
		return loggers;
	}

	@Override
	public  void pruneLogentrys(HashValue entityID, Date before) {
		if (before == null) {
			throw new IllegalArgumentException("no date provided");
		}

		writeLock.lock();
		try {

			if (entityID == null) {
				PreparedStatement stm = c
						.prepareStatement("DELETE FROM logs WHERE timestamp < ? ");
				stm.setLong(1, before.getTime());
				stm.executeUpdate();
				stm.close();
			} else {
				PreparedStatement stm = c
						.prepareStatement("DELETE FROM logs WHERE entityid = ? AND timestamp < ? ");
				stm.setString(1, entityID.toString());
				stm.setLong(2, before.getTime());
				stm.executeUpdate();
				stm.close();
			}
			PreparedStatement stm2 = c
					.prepareStatement("DELETE FROM logEntitys WHERE NOT EXISTS"
							+ "( SELECT * FROM logs WHERE logs.entityid = logEntitys.entityid ) ");

			if (stm2.executeUpdate() > 0) {
				loadLogEntitys();
			}
			stm2.close();

		} catch (SQLException e) {
			logger.warn(e, e);
		} finally {
			writeLock.unlock();
		}
	}

}
