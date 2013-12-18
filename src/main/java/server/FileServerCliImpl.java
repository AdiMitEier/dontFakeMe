package server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.ChecksumUtils;
import util.Config;
import util.FileUtils;
import cli.Command;
import cli.Shell;
import message.Response;
import message.request.*;
import message.response.*;
import model.DownloadTicket;

public class FileServerCliImpl implements IFileServerCli, IFileServer{

	private Config config;
	private Shell shell;
	
	private int tcpPort;
	private int udpPort;
	private int alive;
	private String host;
	private String dir;
	
	private DatagramSocket datagramSocket;
	private ServerSocket serverSocket;
	
	private Thread shellThread;
	 
	private ExecutorService worker = Executors.newCachedThreadPool();
	
	private Timer timer;
	
	public static void main(String[] args) {
		if(args.length > 0) {
			System.out.println(args[0]);	
		}
		new FileServerCliImpl(new Config(args[0]), new Shell(args[0], System.out, System.in));
	}
	
	public FileServerCliImpl(Config config, Shell shell) {
		System.out.println("Starting fileserver");
		this.config = config;
		this.shell = shell;
		readConfig();
		timer = new Timer();
        timer.schedule(new AliveMessages(), 0l, (long)alive);
        worker.execute(new TcpListener(this));
        this.shell.register(this);
        shellThread = new Thread(this.shell);
		shellThread.start();
	}
	
	private void readConfig() {
		tcpPort = config.getInt("tcp.port");
		udpPort = config.getInt("proxy.udp.port");
		alive = config.getInt("fileserver.alive");
		host = config.getString("proxy.host");
		dir = config.getString("fileserver.dir");
	}
	
	private class AliveMessages extends TimerTask {
		private byte[] sendData = new byte[12];
		public void run() {
			try {
				datagramSocket = new DatagramSocket();
				InetAddress IPAddress = InetAddress.getByName(host);
				String sentence = "!alive " + String.valueOf(tcpPort);
				sendData = sentence.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, udpPort);
				datagramSocket.send(sendPacket);
				datagramSocket.close();
			} catch (IOException e) {
				System.out.println("AliveMessages stopped");
				return;
			}	
		}
	}
	
	private class TcpListener implements Runnable {
		
		private FileServerCliImpl fileServerCli;
		
		public TcpListener(FileServerCliImpl fileServerCli) {
			this.fileServerCli = fileServerCli;
		}
		
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(tcpPort);
				while(true) {
					try {
						Socket socket = serverSocket.accept();
						ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
						Object requestObj = input.readObject();
						if(requestObj instanceof ListRequest) {
							ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
							output.writeObject(fileServerCli.list());
						}
						else if(requestObj instanceof UploadRequest) {
							ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
							output.writeObject(fileServerCli.upload((UploadRequest)requestObj));
						}
						else if(requestObj instanceof InfoRequest) {
							ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
							output.writeObject(info((InfoRequest)requestObj));
						}
						else if(requestObj instanceof VersionRequest) {
							ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
							output.writeObject(version((VersionRequest)requestObj));
						}
						else if(requestObj instanceof DownloadFileRequest) {
							ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
							output.writeObject(download((DownloadFileRequest)requestObj));
						}
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
					}
				} 		
			} catch (IOException e) {
				System.out.println("FileServer: TcpListener shutdown");
				return;
			}
		}
	}
	
	@Override
	@Command
	public MessageResponse exit() throws IOException {
		System.out.println("Shutting down fileserver now");
		datagramSocket.close();
		timer.cancel();
		serverSocket.close();
		worker.shutdown();
		shellThread.interrupt();
		shell.close();
		System.in.close();
		return new MessageResponse("FileServer exited");
	}

	@Override
	public Response list() throws IOException {
		Set<String> list = new HashSet<String>();
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles(); 
		for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()) 
			{
				list.add(listOfFiles[i].getName());
			}
		}
		return new ListResponse(list);
	}

	@Override
	public Response download(DownloadFileRequest request) throws IOException {
		DownloadTicket ticket = request.getTicket();
		File file = new File(dir+"/"+ticket.getFilename());
		if(ChecksumUtils.verifyChecksum(ticket.getUsername(), file, 1, ticket.getChecksum())) {
			byte[] data = FileUtils.readFile(dir,ticket.getFilename());
			if(data == null) return new MessageResponse("Cannot read file");
			return new DownloadFileResponse(ticket,data);
		} else {
			return new MessageResponse("Failed to verify checksum");
		}
	}

	@Override
	public Response info(InfoRequest request) throws IOException {
		byte[] data = FileUtils.readFile(dir,request.getFilename());
		if(data == null) return new MessageResponse("File does not exist!");
		else {
			return new InfoResponse(request.getFilename(),data.length);
		}
	}

	@Override
	public Response version(VersionRequest request) throws IOException {
		return new VersionResponse(request.getFilename(),1);
	}

	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		if(!FileUtils.writeFile(dir,request.getFilename(),request.getContent())) {
			return new MessageResponse("Cannot write file");
		}
		return new MessageResponse("Succesfully uploaded file to fileserver on port " + tcpPort);
	}
}
