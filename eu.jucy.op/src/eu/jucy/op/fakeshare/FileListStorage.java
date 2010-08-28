package eu.jucy.op.fakeshare;



import helpers.GH;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import uc.IUser;
import uc.crypto.HashValue;
import uc.crypto.TigerHashValue;
import uc.files.filelist.FileList;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;
import uc.files.filelist.IFileListItem;

public class FileListStorage {

	private static Logger logger = LoggerFactory.make(Level.DEBUG);
	
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false);
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();
	
	private File storagePath;
	private String url;
	private Connection c;
	
	private long maxID;
	private static final long ROOT = 0;
	
	
	
	public void init(File storagePath) throws Exception {
		writeLock.lock();
		try {
			this.storagePath = storagePath; 
			
			Class.forName("org.hsqldb.jdbcDriver");
			
			boolean allok = connect();
			
			if (!allok) {
				throw new IllegalStateException("initialization not successful");
			}
			
			if (!tableExists()) {
				createTables();
			}
			
			
			setMaxID();
		} finally {
			writeLock.unlock();
		}
		logger.debug("FL storage initialized");
	}
	
	private void setMaxID() throws SQLException {
		Statement maxFolderID = c.createStatement();
		ResultSet maxIDrs = maxFolderID.executeQuery("SELECT MAX(ID) AS maxID FROM folders");
		while (maxIDrs.next()) {
			maxID = maxIDrs.getLong("maxID");
			break;
		}
		logger.debug("found maxID: "+maxID);
		maxIDrs.close();
	}
	
	
	public void shutdown() {
		writeLock.lock();
		try {
			disconnect();
			logger.debug("FL storage shutdown");
		} finally {
			writeLock.unlock();
		}
	}
	
	private  boolean connect() throws IOException {
		boolean allok = true;
		File folder = new File(storagePath, "db");

		if (!folder.exists()) {
			allok = folder.mkdirs();
		}

		String newPrefix = "operator_plugin_db";
		File newPath = new File(folder, newPrefix);

		if (!newPath.isFile()) {
			allok &= newPath.createNewFile();
		}

		url = "jdbc:hsqldb:file:" + newPath;
		try {
			c = DriverManager.getConnection(url, "sa", "");
		} catch (SQLException sqle) {
			throw new IOException(sqle);
		}
		return allok;
	}
	
	private void disconnect() {
		if (c == null) {
			return;
		}
		try {
			Statement sd = c.createStatement();
			sd.execute("SHUTDOWN");
			sd.close();
			c.close();
			c = null;
		} catch (SQLException e) {
			logger.warn("Disconnecting failed " + e.toString(), e);
		}
	}
	
	/**
	 * test if we have to create the tables.. or if they already exist
	 */
	private boolean tableExists() {
		try {
			Statement s = c.createStatement();
			s.execute("SELECT 1 FROM files");
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	private void createTables() {
		try {
			Statement createFileListUsers = c.createStatement();
			createFileListUsers.execute("CREATE CACHED TABLE users ("
					+ "   userID BINARY( "+TigerHashValue.digestlength+") PRIMARY KEY"// CHARACTER( "+ TigerHashValue.serializedDigestLength + " ) PRIMARY KEY"
					+ " , nick VARCHAR(1024) "
					+ " , cid BINARY( "+TigerHashValue.digestlength+ " )" 
					+ " )");
			
			createFileListUsers.close();
			
			
			Statement createFileListCheck = c.createStatement();
			createFileListCheck.execute("CREATE CACHED TABLE fileListCheck ("
					+ "   userID BINARY( "+TigerHashValue.digestlength+")"//CHARACTER( "+ TigerHashValue.serializedDigestLength + " )"
					+ " , cidFL BINARY( "+TigerHashValue.digestlength+")"// CHARACTER( "+ TigerHashValue.serializedDigestLength + " )" //CID in the filelist..
					+ " , dateChecked BIGINT"
					+ " , size BIGINT"
					+ " , numberFiles INT"
					
					+ " , PRIMARY KEY ( userID , dateChecked )"
					+ " , FOREIGN KEY ( userID )"
				    + " REFERENCES users ( userID )" 
					+ " )");
			
			createFileListCheck.close();
			
			
			Statement fileListFolders = c.createStatement();
			fileListFolders.execute("CREATE CACHED TABLE folders ("
					+ "   userID BINARY( "+TigerHashValue.digestlength+")"// CHARACTER(" + TigerHashValue.serializedDigestLength + ")"  
					+ " , ID BIGINT PRIMARY KEY "  
					+ " , name VARCHAR(1024)"
					+ " , parentID BIGINT "
					+ " , UNIQUE ( name, userID ,parentID)"
					
		//			+ " , FOREIGN KEY ( parentID )"
		//			+ " REFERENCES folders (ID) ON DELETE CASCADE "
					
					+ " , FOREIGN KEY ( userID )"
				    + " REFERENCES users ( userID ) ON DELETE CASCADE " 
					+ ")");
			
			fileListFolders.close();
			
			Statement index1 = c.createStatement();
			index1.execute("CREATE INDEX userid_index ON folders ( userID ) ");
			index1.close();
			
			insertFolder(null, ROOT,"", ROOT); //unnamed root for all user root folders..
			
			
			Statement fileListFiles = c.createStatement();
			fileListFiles.execute("CREATE CACHED TABLE files ("
					+ "   folderID BIGINT"  //belongs to
					+ " , tth BINARY( "+TigerHashValue.digestlength+")"// CHARACTER("	+ TigerHashValue.serializedDigestLength + ")"
					+ " , name VARCHAR(1024) "
					+ " , size BIGINT" 
					+ " , firstSeen BIGINT"
					+ " , userID BINARY( "+TigerHashValue.digestlength+")"// CHARACTER("	+ TigerHashValue.serializedDigestLength + ")"
					
					+ " , FOREIGN KEY ( folderID )"
					+ " REFERENCES folders ( ID ) ON DELETE CASCADE "
					+ " , FOREIGN KEY ( userID )"
				    + " REFERENCES users ( userID ) ON DELETE CASCADE " 
					+		")");
			fileListFiles.close();
			
			Statement index2 = c.createStatement();
			index2.execute("CREATE INDEX userid_files_index ON files ( userID ) ");
			index2.close();
			
			Statement index3 = c.createStatement();
			index3.execute("CREATE INDEX first_seen_index ON files ( firstSeen ) ");
			index3.close();
			
//			Statement filesWithUserIDView = c.createStatement();
//			filesWithUserIDView.execute("CREATE VIEW userfiles AS "
//					+ " SELECT f.folderID folderID, f.tth tth, f.name name, f.size size, f.firstSeen firstSeen, o.userID userID"
//					+ " FROM files f , folders o WHERE f.folderID = o.ID "
//			);
			
//			filesWithUserIDView.close();
			
		} catch (SQLException e) {
			logger.warn(e,e);
		}
	}
	
	
	private void addFileListCheck(FileList fl,long downloaded) throws SQLException {
		
		IUser usr = fl.getUsr();
		//UPDATE table SET column = Expression [, ...] [WHERE Expression];
		PreparedStatement existsCheck = 
			c.prepareStatement("UPDATE users SET nick = ? , cid = ? WHERE userID = ? ");
		
		byte[] cid = usr.getCID() == null? null: usr.getCID().getRaw();
		existsCheck.setString(1, usr.getNick());
		existsCheck.setBytes(2, cid );
		existsCheck.setBytes(3, usr.getUserid().getRaw());
		
		int count = existsCheck.executeUpdate();
		existsCheck.close();
		
		if (count == 0) {
			PreparedStatement addUser = 
				c.prepareStatement("INSERT INTO users (nick,cid,userID) VALUES(?,?,?) ");
			
			addUser.setString(1, usr.getNick());
			addUser.setBytes(2, cid );
			addUser.setBytes(3, usr.getUserid().getRaw());
			addUser.execute();
			addUser.close();
		}
		
		PreparedStatement addCheck = 
			c.prepareStatement("INSERT INTO fileListCheck (userID,cidFL,dateChecked,size,numberFiles) VALUES(?,?,?,?,?) ");
		
		addCheck.setBytes(1, usr.getUserid().getRaw());
		addCheck.setBytes(2, fl.getCID() != null? fl.getCID().getRaw():null);
		addCheck.setLong(3 ,downloaded);
		addCheck.setLong(4, fl.getSharesize());
		addCheck.setInt(5, fl.getNumberOfFiles());
		addCheck.execute();
		addCheck.close();
		
	}
	
	public void insertFileList(FileList newFileList,long downloaded) {
		
		writeLock.lock();
		long start= System.currentTimeMillis();;
		try {
			Map<Long,FileListFolder> folders = new TreeMap<Long,FileListFolder>();
			Map<HashValue,Long> firstSeen = new HashMap<HashValue,Long>();
			loadFileList(newFileList.getUsr(),folders,firstSeen);
			

			
			IUser user = newFileList.getUsr();
			addFileListCheck(newFileList, downloaded);
			
			
			
			PreparedStatement deletePresentFolders = c
				.prepareStatement("DELETE FROM folders WHERE userID = ? ");		
			
			deletePresentFolders.setBytes(1, user.getUserid().getRaw());
			int deletedFolders= deletePresentFolders.executeUpdate();
			deletePresentFolders.close();
			

			logger.info("deleted Folders: "+deletedFolders);
			
			FileListFolder root = newFileList.getRoot();
			
			HashMap<FileListFolder,Long> foldersNew = new HashMap<FileListFolder,Long>();
			
			
			HashValue userID = user.getUserid();
		
			++maxID;
			foldersNew.put(root, maxID);
			insertFolder(userID,maxID, root.getName(),ROOT);
			
			long count = 1;
			for (FileListFolder current: newFileList.getFolderIterable()) {
				if (current.isOriginal()) {
					++maxID;
					foldersNew.put(current, maxID);
					insertFolder(userID,maxID, current.getName(), foldersNew.get(current.getParent()));
					count++;
				}
			}
			logger.debug("Inserted: "+count+" folders");
			count = 0;
			for (FileListFile file : newFileList.getFileIterable()) {
				if (file.isOriginal()) {
					long parentID = foldersNew.get(file.getParent());
					Long fs = firstSeen.get(file.getTTHRoot());
					insertFile(parentID
							,file.getTTHRoot()
							,file.getName()
							,file.getSize()
							,fs == null? downloaded:fs
							, userID);
					
					count++;
				}
			}
			logger.debug("Inserted: "+count+" files");
			logger.debug("after maxID: "+maxID);
		} catch (SQLException e) {
			logger.error(e,e);
		} finally {
			writeLock.unlock();
		}
		long end = System.currentTimeMillis();
		logger.debug(String.format("Time needed insert: %d ms", (end-start)));
	}
	
	private void insertFolder(HashValue userID,long folderid,String foldername,long parentID) throws SQLException {
	//	logger.debug("inserting folder: foldername: "+foldername +" id: "+folderid+"  parentID: "+parentID);
		PreparedStatement addFolders = c
		.prepareStatement("INSERT INTO folders (userID,ID,name,parentID) VALUES(?,?,?,?) ");
		
		addFolders.setBytes(1, userID != null ?userID.getRaw():null);
		addFolders.setLong(2, folderid);
		addFolders.setString(3, foldername);
		addFolders.setLong(4, parentID);
		
		try {
			addFolders.execute();
		} catch (SQLException sqle) {
			logger.warn("error folder: "+foldername+"  parentID "+parentID+"  folderid: " +folderid+"  "+sqle.toString(),sqle);
		}
		addFolders.close();
	}
	
	private void insertFile(long parentID,HashValue hash,String name,long size,long firstSeen,HashValue userID) throws SQLException {
		PreparedStatement addFile = c
		.prepareStatement("INSERT INTO files (folderID,tth,name,size,firstSeen,userID) VALUES(?,?,?,?,?,?) ");
		
		addFile.setLong(1, parentID);
		addFile.setBytes(2, hash.getRaw());
		addFile.setString(3, name);
		addFile.setLong(4, size);
		addFile.setLong(5, firstSeen);
		addFile.setBytes(6, userID.getRaw());
		
		try {
			addFile.execute();
		} catch (SQLException sqle) {
			logger.warn("error file "+name+"  parentID "+parentID+" "+sqle.toString(),sqle);
		}
		addFile.close();
	}
	
	
	
	private FileList loadFileList(IUser user,Map<Long,FileListFolder> folders,Map<HashValue,Long> firstSeen) {
		FileList fl = new FileList(user);
		readLock.lock();
		try {
			PreparedStatement loadFolders = c
			.prepareStatement("SELECT ID , name, parentID FROM folders WHERE userID = ? ORDER BY parentID ASC ");
			
			loadFolders.setBytes(1, user.getUserid().getRaw());
			
			ResultSet rs = loadFolders.executeQuery();
			while (rs.next()) {
				String name  = rs.getString("name");
				long id = rs.getLong("ID");
				long parentID = rs.getLong("parentID");
				
				if (parentID == ROOT) {
					folders.put(id, fl.getRoot());
				} else {
					try {
						FileListFolder parent = folders.get(parentID);
						FileListFolder child = new FileListFolder(parent, name);
						folders.put(id, child);
					} catch (NullPointerException npe) {
						logger.debug("name: "+name,npe);
					}
				}
			}
			rs.close();
			
			PreparedStatement loadFiles = c
			.prepareStatement("SELECT name , tth , size , folderID , firstSeen FROM files WHERE userID = ? ");//folderID, tth , name, size, firstSeen FROM files INNER JOIN folder ON folderID = ID WHERE userID = ? ");
			
			loadFiles.setBytes(1, user.getUserid().getRaw());
			
			ResultSet rs2 = loadFiles.executeQuery();
			while (rs2.next()) {
				String name  = rs2.getString("name");
				HashValue hash = HashValue.createHash(rs2.getBytes("tth"));
				long size = rs2.getLong("size");
				long folderID = rs2.getLong("folderID");
				long seen = rs2.getLong("firstSeen");
				FileListFolder parent = folders.get(folderID);
				new FileListFile(parent, name, size, hash);
				firstSeen.put(hash, seen);
			}
			rs2.close();
			
		} catch (SQLException e) {
			logger.warn(e,e);
		} finally {
			readLock.unlock();
		}
		
		return fl;	
	}
	
	public FileList loadFileList(IUser user) {
		HashMap<Long,FileListFolder> folders = new HashMap<Long,FileListFolder>();
		return loadFileList(user, folders,new HashMap<HashValue,Long>() );
	}
	
	
	/**
	 * @param user create the TTH distribution for the given use..
	 * 
	 */
	public Map<Integer,Integer> createKnownTTHDistribution(IUser user) {
		HashMap<Integer,Integer> found = new HashMap<Integer,Integer>();
		
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			PreparedStatement countFiles = c
			.prepareStatement(
				"SELECT amount , count(*) AS times FROM "
					+" (SELECT tth , count(*) AS amount FROM " 
						+ " (SELECT DISTINCT tth AS hash FROM files WHERE userID = ? AND size != 0 ) " 
					+ " INNER JOIN " 
						+ " (SELECT DISTINCT tth , userID FROM files)  " 
					+ " ON hash = tth " 
					+ " GROUP BY tth )"
				+ " GROUP BY amount "
				+ " ORDER BY amount DESC"
					);
			
			countFiles.setBytes(1, user.getUserid().getRaw());
			
			ResultSet rs = countFiles.executeQuery();
			
			while (rs.next()) {
				Integer count = rs.getInt("amount");
				Integer present = rs.getInt("times");
				found.put(count, present);
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("User distribution: "+user);
				logger.debug( GH.concat(found.entrySet(), "\n", "none"));
			}
			
		} catch (SQLException e) {
			logger.warn(e,e);
		} finally {
			readLock.unlock();
		}
		long end = System.currentTimeMillis();
		logger.debug(String.format("Time needed create: %d ms", (end-start)));
		
		return found;
	}
	
	
	/**
	 * 
	 * @param user - calculate connections for this user..
	 * @return a collection of users and how many files the given user shares with them
	 */
	public Map<IUser,Integer> createKnownUserConnections(IUser user) {
		HashMap<IUser,Integer> found = new HashMap<IUser,Integer>();
		
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			PreparedStatement countFiles = c
			.prepareStatement(
					"SELECT userID, nick, cid, amount FROM " 
						+ " users "
					+ " INNER JOIN "
						+ "(SELECT userID AS uid, count(*) AS amount FROM " 
								+ "(SELECT DISTINCT tth AS hash FROM files WHERE userID = ? AND size != 0 ) " 
							+ " INNER JOIN " 
								+	"(SELECT DISTINCT tth AS hash2, userID FROM files)  " 
							+ " ON hash = hash2 " 	
							+ " GROUP BY userID )"
					+ " ON users.userID = uid "
					+ " ORDER BY amount DESC"
					);
			
			countFiles.setBytes(1, user.getUserid().getRaw());
			
			ResultSet rs = countFiles.executeQuery();
			
			while (rs.next()) {
				int count = rs.getInt("amount");
				HashValue userID = HashValue.createHash(rs.getBytes("userID"));
				String nick = rs.getString("nick");
				
				byte[] possiblyCID = rs.getBytes("cid");
				if (HashValue.isHash(possiblyCID)) {
					HashValue cid = HashValue.createHash(possiblyCID);
				}
				
				logger.debug("Connection: "+nick+"  "+count);
			}
			rs.close();
			

			
		} catch (SQLException e) {
			logger.warn(e,e);
		} finally {
			readLock.unlock();
		}
		long end = System.currentTimeMillis();
		logger.debug(String.format("Time needed create: %d ms", (end-start)));
		
		return found;
	}
	
	private Distributions calculateDistributions(FileList fl) {
		Distributions dist = new Distributions();
		
		for (IFileListItem item: fl) {
			String name = item.getName().toLowerCase();
			for (char c:name.toCharArray()) {
				GH.incrementMappedCounter(dist.letters, c, 1);
			}
			for (String s: name.split("\\W")) {
				String word = s.trim();
				GH.incrementMappedCounter(dist.words, word, 1);
			}
			long blockSize =  (item.getSize() / 4096) *4096;
			GH.incrementMappedCounter(dist.sizes, blockSize, 1);
		}
		
		return dist;
	}
	
	
	private static TreeMap<Integer,Integer> createZipf(Collection<Integer> frequencies) {
		List<Integer> list = new ArrayList<Integer>(frequencies);
		Collections.sort(list,Collections.reverseOrder());
		
		TreeMap<Integer,Integer> zipf = new TreeMap<Integer,Integer>();
		for (int i = 0; i < list.size(); i++) {
			zipf.put(i, list.get(i));
		}
		
		return zipf;
	}
	
	private static class Distributions {
		
		private final Map<Character,Integer> letters = 
			new HashMap<Character, Integer>();
		
		private final Map<String,Integer> words = 
			new HashMap<String, Integer>();
		
		private final Map<Long,Integer> sizes = 
			new HashMap<Long, Integer>();
		
		
	}
	
	
}
