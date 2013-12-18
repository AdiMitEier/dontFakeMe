package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Command;
import cli.Shell;
import util.*;
import message.Response;
import message.request.*;
import message.response.*;
import model.FileServerInfo;
import model.FileServerModel;
import model.UserInfo;
import model.UserModel;

public class ProxyCliImpl implements IProxyCli {
	
	private Config proxyConfig;
	private Config userConfig;
	private Shell shell;
	private int tcpPort;
	private int udpPort;
	private int timeOut;
	private int checkPeriod;
	private List<String> userNames;
	private List<UserModel> users;
	private List<FileServerModel> fileServers;
	private List<FileServerModel> readQuorum;
	private List<FileServerModel> writeQuorum;
	
	private DatagramSocket datagramSocket;
	private ServerSocket serverSocket;
	
	private List<Socket> clientSockets;
	
	private Thread shellThread;
	 
	private ExecutorService worker = Executors.newCachedThreadPool();
	
	private Timer timer;
	
	public static void main(String[] args) {
		new ProxyCliImpl(new Config("proxy"), new Shell("proxy", System.out, System.in));
	}
	
	public ProxyCliImpl(Config config, Shell shell) {
		System.out.println("Starting proxy");
		proxyConfig = config;
		this.shell = shell;
		fileServers = new ArrayList<FileServerModel>();
		users = new ArrayList<UserModel>();
		clientSockets = new ArrayList<Socket>();
		readProxyConfig();
		readUserConfig();
		worker.execute(new TcpListener(this));
		worker.execute(new UdpListener());
		timer = new Timer();
        timer.schedule(new CheckForTimeout(), checkPeriod, checkPeriod);
		this.shell.register(this);
		shellThread = new Thread(this.shell);
		shellThread.start();
	}
	
	private void readProxyConfig() {
		tcpPort = proxyConfig.getInt("tcp.port");
		udpPort = proxyConfig.getInt("udp.port");
		timeOut = proxyConfig.getInt("fileserver.timeout");
		checkPeriod = proxyConfig.getInt("fileserver.checkPeriod");
	}
	
	private void readUserConfig() {
		Properties prop = new Properties();
		try {
			prop.load(ProxyCliImpl.class.getClassLoader().getResourceAsStream("user.properties"));
			userNames = new ArrayList<String>();
			for(Object e : prop.keySet()) {
				String newUserName = e.toString().split("\\.")[0];
				if(!userNames.contains(newUserName)) {
					userNames.add(newUserName);
				}
	        }
		} catch (IOException e) {
			System.out.println("Cannot read user config");
			return;
		}
		
		userConfig = new Config("user");
		for(String userName : userNames) {
			users.add(new UserModel(userName, userConfig.getString(userName+".password"),false,userConfig.getInt(userName+".credits")));
		}
	}
	
	private class TcpListener implements Runnable {
		
		private ProxyCliImpl proxyCli;
		
		public TcpListener(ProxyCliImpl proxyCli) {
			this.proxyCli = proxyCli;
		}
		
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(tcpPort);
				while(true) {
						Socket socket = serverSocket.accept();
						clientSockets.add(socket);
						System.out.println("Proxy: New client accepted");
						worker.execute(new ProxyImpl(socket, proxyCli));
				}
			} catch (IOException e) {
				System.out.println("Proxy: TcpListener shutdown");
				return;
			}
		}
	}
	
	private class UdpListener implements Runnable {
		private byte[] receiveData = new byte[12];
		@Override
		public void run() {
			try {
				datagramSocket = new DatagramSocket(udpPort);
				while(true) {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					datagramSocket.receive(receivePacket);
					int receivedPort = Integer.parseInt(new String(receivePacket.getData()).split(" ")[1]);
					int indexOfFileServer = getIndexOfFileServer(receivedPort);
					if( indexOfFileServer != -1) {
						FileServerModel server = fileServers.get(indexOfFileServer);
						server.setAlive(new Date());
						if(!server.isOnline()) {
							server.setOnline(true);
							refreshFileListOfServer(server.getAddress(),server.getPort());
						}
					} else {
						System.out.println("Proxy: New fileserver on port " + receivedPort);
						fileServers.add(new FileServerModel(receivePacket.getAddress(),receivedPort,0,new Date(),true,new HashSet<String>()));
						refreshFileListOfServer(receivePacket.getAddress(),receivedPort);
					}
				}
			} catch (IOException e) {
				System.out.println("Proxy: UdpListener shutdown");
				return;
			}
		}
		
		private void refreshFileListOfServer(InetAddress address, int port) throws IOException {
			FileServerModel server = fileServers.get(getIndexOfFileServer(port));
			server.getFileList().clear();
			Socket socket = new Socket(address,port);
			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			output.writeObject(new ListRequest());
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			try {
				Object responseObj = input.readObject();
				if(responseObj instanceof ListResponse) {
					ListResponse response = (ListResponse)responseObj;
					for(String file : response.getFileNames()) {
						addToFileList(server,file);
					}
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
			} finally {
				socket.close();
			}
		}
	}
	
	private class CheckForTimeout extends TimerTask {
        public void run() {
        	for(FileServerModel fileServer : fileServers) {
        		if(fileServer.getAlive().getTime()+timeOut < new Date().getTime()) {
        			fileServer.setOnline(false);
        		}
        	}
        }
    }
	
	public List<UserModel> getUsers() {
		return users;
	}
	
	public List<FileServerModel> getFileServers() {
		return fileServers;
	}
	
	public List<FileServerModel> getReadQuorum() {
		return readQuorum;
	}
	
	public List<FileServerModel> getWriteQuorum() {
		return writeQuorum;
	}
	
	//quorums always satisfy the following constraints:
	//readQuorum.size() + writeQuorum.size() > N
	//writeQuorum.size() > N/2
	public void buildQuorums(){

		int N = fileServers.size();
		readQuorum = new ArrayList<FileServerModel>();
		writeQuorum = new ArrayList<FileServerModel>();
		
		if(N%2==0){
			for(int i = 0;i<N/2;i++)
				readQuorum.add(fileServers.get(i));
			for(int i = N/2-1;i<N;i++)
				writeQuorum.add(fileServers.get(i));
		} else{
			for(int i = 0;i<Math.ceil(N/2);i++)
				readQuorum.add(fileServers.get(i));
			for(int i = (int) Math.ceil((N/2)-1);i<N;i++)
				writeQuorum.add(fileServers.get(i));
		}
	}

	private int getIndexOfFileServer(int port) {
		for(FileServerModel fileServer : fileServers) {
			if(fileServer.getPort() == port) {
				return fileServers.indexOf(fileServer);
			}
		}
		return -1;
	}
	
	public synchronized void changeCredits(UserModel user, long credits) {
		user.setCredits(user.getCredits() + credits);
		System.out.println("Proxy: Credits of " + user.getName() + " is now: " + user.getCredits());
	}
	
	public synchronized void increaseUsage(FileServerModel server, long usage) {
		server.setUsage(server.getUsage() + usage);
		System.out.println("Proxy: Usage of fileserver on port " + server.getPort() + " is now: " + server.getUsage());
	}
	
	public synchronized void addToFileList(FileServerModel server, String fileName) {
		server.getFileList().add(fileName);
	}

	@Override
	@Command
	public Response fileservers() throws IOException {
		List<FileServerInfo> fileServerInfoList = new ArrayList<FileServerInfo>();
		for(FileServerModel server : fileServers) {
			FileServerInfo info = new FileServerInfo(server.getAddress(), server.getPort(), server.getUsage(), server.isOnline());
			fileServerInfoList.add(info);
		}
		return new FileServerInfoResponse(fileServerInfoList);
	}

	@Override
	@Command
	public Response users() throws IOException {
		List<UserInfo> userInfoList = new ArrayList<UserInfo>();
		for(UserModel user : users) {
			UserInfo info = new UserInfo(user.getName(),user.getCredits(),user.isOnline());
			userInfoList.add(info);
		}
		return new UserInfoResponse(userInfoList);
	}

	@Override
	@Command
	public MessageResponse exit() throws IOException {
		System.out.println("Shutting down proxy now");
		timer.cancel();
		datagramSocket.close();
		serverSocket.close();
		for(Socket socket : clientSockets) {
			socket.close();
		}
		worker.shutdown();
		shellThread.interrupt();
		shell.close();
		System.in.close();
		return new MessageResponse("Proxy exited");
	}
}
