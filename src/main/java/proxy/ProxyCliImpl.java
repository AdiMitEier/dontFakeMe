package proxy;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.Key;
import java.security.PublicKey;

import javax.crypto.Mac;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

import cli.Command;
import cli.Shell;
import client.IClientRMI;
import util.*;
import message.Response;
import message.request.*;
import message.response.*;
import model.FileModel;
import model.FileServerInfo;
import model.FileServerModel;
import model.UserInfo;
import model.UserModel;

public class ProxyCliImpl implements IProxyCli, IProxyRMI {
	//TODO: version und upload sind schon mit hmac
	private Config proxyConfig;
	private Config userConfig;
	private Shell shell;
	private int tcpPort;
	private int udpPort;
	private int timeOut;
	private int checkPeriod;
	private Key secretKey;
	private List<String> userNames;
	private List<UserModel> users;
	private List<FileServerModel> fileServers;
	private Map<String,Integer> downloads;
	private int readQuorum = -1;
	private int writeQuorum = -1;
	private Config mcConfig;
	private String bindingName;
	private String proxyHost;
	private int proxyRMIPort;
	private String keysDir;
	private Registry registry;
	
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
		downloads = new ConcurrentHashMap<String,Integer>();
		readProxyConfig();
		readMCConfig();
		readUserConfig();
		initRMI();
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
		secretKey = FileUtils.readKeyFromFile(proxyConfig.getString("hmac.key"));
	}
	
	private void readMCConfig() {
		mcConfig = new Config("mc");
		bindingName = mcConfig.getString("binding.name");
		proxyHost = mcConfig.getString("proxy.host");
		proxyRMIPort = mcConfig.getInt("proxy.rmi.port");
		keysDir = mcConfig.getString("keys.dir");
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
	
	private void initRMI() {
		try {
			registry = LocateRegistry.createRegistry(proxyRMIPort);
			registry.rebind(bindingName, this);
			UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
						//System.out.println("Proxy: New client accepted");
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
						//System.out.println("Proxy: New fileserver on port " + receivedPort);
						fileServers.add(new FileServerModel(receivePacket.getAddress(),receivedPort,0,new Date(),true,new HashSet<FileModel>()));
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
			ListRequest lr = new ListRequest();
			lr.setHmac(secretKey);
			output.writeObject(lr);
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			try {
				Object responseObj = input.readObject();
				if(responseObj instanceof ListResponse) {
					ListResponse response = (ListResponse)responseObj;
					for(FileModel file : response.getFileNames()) {
						addToFileList(server,file);
					}
				}
				int errorcount = 0;
				while(responseObj instanceof HmacErrorResponse){
					errorcount++;
					Socket s = new Socket(server.getAddress(),server.getPort());
					ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
					o.writeObject(lr);
					ObjectInputStream i = new ObjectInputStream(s.getInputStream());
					responseObj = i.readObject();
					if(responseObj instanceof ListResponse){
						ListResponse response = (ListResponse)responseObj;
						for(FileModel file : response.getFileNames()) {
							addToFileList(server,file);
						}
						s.close();
						break;
					}
					if(errorcount > 4){
						System.out.println("Failed at verifing message Integrity. Debug:ListResponse");
						s.close();
						break;
					}
					s.close();
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
	
	//STAGE3
	public Key getSecretKey(){
		return this.secretKey;
	}
	
	//quorums always satisfy the following constraints:
	//readQuorum.size() + writeQuorum.size() > N
	//writeQuorum.size() > N/2
	//STAGE1
	public int getReadQuorum() {
		return readQuorum;

	}
	
	//quorums always satisfy the following constraints:
	//readQuorum.size() + writeQuorum.size() > N
	//writeQuorum.size() > N/2
	//STAGE1
	public int getWriteQuorum() {
		return writeQuorum;
	}
	
	public void initQuorums(int rQ, int wQ){
		int N = fileServers.size();
		if(N>1)
			readQuorum = (int)Math.ceil(N/2);
		else
			readQuorum = 1;
		writeQuorum = (int)Math.floor((N/2)+1);
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
		//System.out.println("Proxy: Credits of " + user.getName() + " is now: " + user.getCredits());
	}
	
	public synchronized void increaseUsage(FileServerModel server, long usage) {
		server.setUsage(server.getUsage() + usage);
		//System.out.println("Proxy: Usage of fileserver on port " + server.getPort() + " is now: " + server.getUsage());
	}
	
	public synchronized void addToFileList(FileServerModel server, FileModel file) {
		server.getFileList().add(file);
	}
	
	public synchronized void increaseDownloads(String fileName) {
		if(!downloads.containsKey(fileName)) {
			downloads.put(fileName, 1);
		} else {
			downloads.put(fileName, downloads.get(fileName)+1);
		}
		notifySubscriptions(fileName);
	}
	
	public synchronized void notifySubscriptions(String fileName) {
		for(UserModel user : users) {
			if(user.getSubscriptions().containsKey(fileName)) {
				int downloadsSinceSubscription = getDownloads(fileName) - user.getSubscriptions().get(fileName)[1];
				int downloadsToNotify = user.getSubscriptions().get(fileName)[0];
				if(downloadsSinceSubscription < downloadsToNotify) continue;
				try {
					user.getClientObject().notifySubscription(fileName,downloadsToNotify);
					user.getSubscriptions().remove(fileName);
				} catch (RemoteException e) {
					System.out.println("Failed to notify client, clearing its subscriptions");
					user.getSubscriptions().clear();
				}
			}
		}
	}

	public List<FileServerModel> getFileServersWithLowestUsage(int amount) {
		List<FileServerModel> lowestUsage = new ArrayList<FileServerModel>(fileServers);
		for(int i = 1; i< lowestUsage.size();i++){
			FileServerModel tmp = lowestUsage.get(i);
            int j = i;
            while(j>0 && lowestUsage.get(j-1).getUsage()>tmp.getUsage()){
            	lowestUsage.set(j,lowestUsage.get(j-1));
                j--;
            }
            lowestUsage.set(j,tmp);
        }
		return lowestUsage.subList(0,amount);
	}
	
	public synchronized Set<FileModel> getCombinedFileList() {
		Set<FileModel> combinedFileList = new HashSet<FileModel>();
		for(FileServerModel server : getFileServers()) {
			if(server.isOnline()) {
				for(FileModel file : server.getFileList()){
					boolean alreadyInList = false;
					for(FileModel fileCombinedList : combinedFileList){
						if(file.getFilename().equals(fileCombinedList.getFilename()))
							alreadyInList = true;
					}
					if(!alreadyInList){
						combinedFileList.add(file);
					}
				}
			}
		}
		return combinedFileList;
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
		//System.out.println("Shutting down proxy now");
		timer.cancel();
		datagramSocket.close();
		serverSocket.close();
		for(Socket socket : clientSockets) {
			socket.close();
		}
		worker.shutdown();
		try {
			registry.unbind(bindingName);
			UnicastRemoteObject.unexportObject(this,false);
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shellThread.interrupt();
		shell.close();
		System.in.close();
		return new MessageResponse("Proxy exited");
	}

	@Override
	public MessageResponse readQuorum() {
		return new MessageResponse("Read-Quorum is set to "+String.valueOf(readQuorum)+".");
	}

	@Override
	public MessageResponse writeQuorum() throws RemoteException {
		return new MessageResponse("Write-Quorum is set to "+String.valueOf(writeQuorum)+".");
	}

	@Override
	public TopThreeDownloadsResponse topThreeDownloads() throws RemoteException {
		ValueComparator vc =  new ValueComparator(downloads);
        TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(vc);
        sortedMap.putAll(downloads);
        TopThreeDownloadsResponse response = new TopThreeDownloadsResponse();
        if(sortedMap.keySet().size()>0) {
	        response.setFile1((String)sortedMap.keySet().toArray()[0]);
	        response.setDownloads1((Integer)sortedMap.values().toArray()[0]);
        }
        if(sortedMap.keySet().size()>1) {
	        response.setFile2((String)sortedMap.keySet().toArray()[1]);
	        response.setDownloads2((Integer)sortedMap.values().toArray()[1]);
        }
        if(sortedMap.keySet().size()>2) {
	        response.setFile3((String)sortedMap.keySet().toArray()[2]);
	        response.setDownloads3((Integer)sortedMap.values().toArray()[2]);
        }
		return response;
	}
	
	class ValueComparator implements Comparator<String> {
	    Map<String, Integer> base;
	    public ValueComparator(Map<String, Integer> base) {
	        this.base = base;
	    }
    
	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        }
	    }
	}

	@Override
	public MessageResponse subscribe(IClientRMI client, String userName, String fileName, int number) throws RemoteException {
		for(FileModel file : getCombinedFileList()) {
			if(file.getFilename().equals(fileName)) {
				UserModel user = getUser(userName);
				if(user == null) return new MessageResponse("User "+userName+" does not exist");
				if(user.getSubscriptions().put(fileName,new Integer[]{number,getDownloads(fileName)}) != null) return new MessageResponse("Already subscribed");
				else {
					user.setClientObject(client);
					return new MessageResponse("Successfully subscribed for file: "+fileName);
				}
			}
		}
		return new MessageResponse("File does not exist");
	}
	
	private UserModel getUser(String userName) {
		for(UserModel user : users) {
			if(user.getName().equals(userName)) return user;
		}
		return null;
	}
	
	private int getDownloads(String fileName) {
		if(!downloads.containsKey(fileName)) return 0;
		else return downloads.get(fileName);
	}

	@Override
	public ProxyPublicKeyResponse getProxyPublicKey() throws RemoteException {
		String pathToPublicKey = keysDir+"/proxy.pub.pem";
		PEMReader in;
		try {
			in = new PEMReader(new FileReader(pathToPublicKey));
			PublicKey publicKey = (PublicKey) in.readObject();
			in.close();
			return new ProxyPublicKeyResponse(publicKey);
		} catch (FileNotFoundException e) {
			return new ProxyPublicKeyResponse("Key not found");
		} catch (IOException e) {
			return new ProxyPublicKeyResponse("Could not read public key");
		} 
	}

	@Override
	public MessageResponse setUserPublicKey(String userName, PublicKey key) throws RemoteException {
		String pathToPublicKey = keysDir+"/"+userName+".pub.pem";
		PEMWriter out;
		try {
			out = new PEMWriter(new FileWriter(pathToPublicKey));
			out.writeObject(key);
			out.close();
			return new MessageResponse("Successfully transmitted public key of user: "+userName+".");
		} catch (IOException e) {
			return new MessageResponse("Could not write public key");
		} 
	}
}
