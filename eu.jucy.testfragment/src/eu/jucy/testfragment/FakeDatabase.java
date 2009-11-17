package eu.jucy.testfragment;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uc.DCClient;
import uc.IUser;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.database.DBLogger;
import uc.database.IDatabase;
import uc.database.ILogEntry;
import uc.files.downloadqueue.DQEDAO;
import uc.files.filelist.HashedFile;

public class FakeDatabase implements IDatabase {

	public void addLogEntry(ILogEntry logentry) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void addOrUpdateDQE(DQEDAO dqe, boolean add) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void addOrUpdateFile(File file, HashValue tthroot,
			InterleaveHashes inter, Date hashed) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void addUpdateOrDeleteUser(IUser usr) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void addUserToDQE(IUser usr, HashValue dqe) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public int countLogentrys(HashValue entityID) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void deleteAllHashedFiles() {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void deleteDQE(DQEDAO dqe) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void deleteUserFromDQE(IUser usr, DQEDAO dqe) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public Map<File, HashedFile> getAllHashedFiles() {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public InterleaveHashes getInterleaves(HashValue tthroot) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public List<DBLogger> getLogentitys() {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public List<ILogEntry> getLogentrys(HashValue entityID, int max, int offset) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void init(File storagepath,DCClient dcc) throws Exception {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public Set<DQEDAO> loadDQEsAndUsers() {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void pruneLogentrys(HashValue entityID, Date before) {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public Map<File, HashedFile> pruneUnusedHashedFiles() {
		throw new IllegalStateException("Called method in Fake Database");
	}

	public void shutdown() {
		throw new IllegalStateException("Called method in Fake Database");
	}

}
