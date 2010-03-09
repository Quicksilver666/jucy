import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;



import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelSftp.LsEntry;


public class UploadFile {

	private final String username, password,host,fingerprint;

	
	public static void main(String... test) throws IOException {
		String targetDir ="/home/www/UC/NewVersion";
		File sourceDir = new File("C:\\tmp\\UCStuff\\build");
		System.out.println("please provide pass for root@jucy.eu:");
		String pass = new BufferedReader(new InputStreamReader(System.in)).readLine();
		UploadFile ulf = new UploadFile("root",pass, "jucy.eu","22:53:4e:0c:9d:a0:09:f4:78:06:2c:d3:13:0c:ac:d3");
		ulf.upload(sourceDir, targetDir);
	}
	
	public UploadFile(String username, String password,String host,String fingerprint) {
		super();
		this.username = username;
		this.password = password;
		this.host = host;
		this.fingerprint = fingerprint;
	}
	
	public boolean upload(File sourceFolder,String targetFolder) {
		JSch jsch = new JSch();
		int port = 22;
		Session session = null;
		Channel channel = null;
		try {
			session = jsch.getSession(username, host, port);

			UserInfo ui = new UPFileUserInfo();
			session.setUserInfo(ui);

			session.connect();

			channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp c = (ChannelSftp) channel;
			
			long before = System.currentTimeMillis();
			long total = rekUpload(sourceFolder,targetFolder,c);
			
			long timeTotal = System.currentTimeMillis() - before;
			
			System.out.println(String.format( "Totally uploaded: %.2f MiB in %2$tH:%2$tM:%2$tS", (double)total/(1024*1024),timeTotal ));
			c.disconnect();
			
			return true;
			
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (SftpException e) {
			e.printStackTrace();
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private long rekUpload(File source,String path,ChannelSftp c) throws SftpException {
		long total = 0;
		List<LsEntry> existing = c.ls(path);
		for (File f:source.listFiles()) {
			if (f.isFile()) {
				c.put(f.getPath(), path+"/"+f.getName(),new MyProgress());
				total+=f.length();
				
			} else if (f.isDirectory()) {
				String dirPath = path+"/"+f.getName();
				boolean exists= false;
				for (LsEntry lse:existing) {
					if (lse.getFilename().equals(f.getName())) {
						exists = true;
					}
				}
				if (!exists) {
					c.mkdir(dirPath);
				}
				total+=rekUpload(f, dirPath, c);
			}
		}
		return total;
	}
	

	
	class UPFileUserInfo implements UserInfo {

		@Override
		public String getPassphrase() {
			return null;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public boolean promptPassword(String message) {
			System.out.println("[ppass]"+message);
			return true;
		}

		@Override
		public boolean promptPassphrase(String message) {
			System.out.println("[ppassp]"+message);
			return false;
		}

		@Override
		public boolean promptYesNo(String message) {
			System.out.println("[promt]"+message);
			if (message.contains("fingerprint") && fingerprint != null) {
				boolean matches =  message.contains(fingerprint);
				if (!matches) {
					System.err.println("Bad fingerprint found!");
				}
				return matches;
			}
			return true;
		}

		@Override
		public void showMessage(String message) {
			System.out.println("[show]"+message);
		}
		
	}
	
	public static class MyProgress implements SftpProgressMonitor {
		long current = 0;

		@Override
		public void init(int op, String src, String dest, long max) {
			System.out.print("[up]"+src+" Size: "+(max/1024)+" KiB ");
			current = 0;
		}

		@Override
		public boolean count(long count) {
			int before = (int) (current /(1024*1024));
			current+= count;
			int after = (int) (current /(1024*1024));
			if (before < after) {
				System.out.print(".");
			}
			return true;
		}

		@Override
		public void end() {
			System.out.println(" done");
		}
		
	}
	
}
