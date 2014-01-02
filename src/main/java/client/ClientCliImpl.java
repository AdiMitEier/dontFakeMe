package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import util.Config;
import util.FileUtils;
import cli.Command;
import cli.Shell;
import message.Response;
import message.request.*;
import message.response.*;
import message.response.LoginResponse.Type;
import model.DownloadTicket;
import model.FileModel;

public class ClientCliImpl implements IClientCli {
	
	private Config config;
	private Shell shell;
	private String dir;
	private String host;
	private int tcpPort;
	
	Socket clientSocket = null;
	
	private Thread shellThread;
	
	public static void main(String[] args) {
		new ClientCliImpl(new Config("client"), new Shell("client", System.out, System.in));
	}
	
	public ClientCliImpl(Config config, Shell shell) {
		System.out.println("Starting client");
		this.config = config;
		this.shell = shell;
		readConfig();
		this.shell.register(this);
		shellThread = new Thread(this.shell);
		shellThread.start();
	}
	
	private void readConfig() {
		tcpPort = config.getInt("proxy.tcp.port");
		host = config.getString("proxy.host");
		dir = config.getString("download.dir");
	}
	
	@Override
	@Command
	public LoginResponse login(String username, String password) throws IOException {
		if(clientSocket != null && !clientSocket.isClosed()) {
			System.out.println("You are already logged in!");
			return new LoginResponse(Type.WRONG_CREDENTIALS);
		}
		try {
			clientSocket = new Socket(host,tcpPort);
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(new LoginRequest(username, password));
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				LoginResponse response = (LoginResponse)input.readObject();
				if(response.getType() == Type.WRONG_CREDENTIALS) {
					clientSocket.close();
				}
				return response;
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new LoginResponse(Type.WRONG_CREDENTIALS);
			}
		} catch(IOException e) {
			System.out.println("Connection error");
			return new LoginResponse(Type.WRONG_CREDENTIALS);
		}
	}

	@Override
	@Command
	public Response credits() throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(new CreditsRequest());
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				return (Response)input.readObject();
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		}
	}

	@Override
	@Command
	public Response buy(long credits) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(new BuyRequest(credits));
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				return (Response)input.readObject();
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		}
	}

	@Override
	@Command
	public Response list() throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			ListRequest request = new ListRequest();
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(request);
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				return (Response)input.readObject();
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		}
	}

	@Override
	@Command
	public Response download(String filename) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(new DownloadTicketRequest(filename));
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				Object responseObj = input.readObject();
				if(responseObj instanceof DownloadTicketResponse) {
					DownloadTicketResponse response = (DownloadTicketResponse)responseObj;
					DownloadTicket ticket = response.getTicket();
					Socket socket;
					socket = new Socket(ticket.getAddress(),ticket.getPort());
					ObjectOutputStream fileServerOutput = new ObjectOutputStream(socket.getOutputStream());
					fileServerOutput.writeObject(new DownloadFileRequest(ticket));
					ObjectInputStream fileServerinput = new ObjectInputStream(socket.getInputStream());
					try {
						Object fileServerResponseObj = fileServerinput.readObject();
						if(fileServerResponseObj instanceof DownloadFileResponse) {
							DownloadFileResponse fileServerResponse = (DownloadFileResponse)fileServerResponseObj;
							if(!FileUtils.writeFile(dir,fileServerResponse.getTicket().getFilename(),fileServerResponse.getContent())) {
								socket.close();
								return new MessageResponse("Cannot write file");
							}
							socket.close();
							return fileServerResponse;
						} else if(fileServerResponseObj instanceof MessageResponse) {
							socket.close();
							return (MessageResponse)fileServerResponseObj;
						}
					} catch (ClassNotFoundException e) {
						socket.close();
						return new MessageResponse("Unexpected communication error appeared");
					}
					socket.close();
					return null;
				} else if(responseObj instanceof MessageResponse) {
					MessageResponse response = (MessageResponse)responseObj;
					return response;
				} else {
					return new MessageResponse("Unexpected communication error appeared");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		}
	}

	@Override
	@Command
	public MessageResponse upload(String filename) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		byte[] data = FileUtils.readFile(dir,filename);
		if(data == null) return new MessageResponse("Cannot read file");
		try {
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(new UploadRequest(filename, 0, data));
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				Object responseObj = input.readObject();
				if(responseObj instanceof MessageResponse) {
					return (MessageResponse)responseObj;
				} else {
					return new MessageResponse("Unexpected communication error appeared");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		}
	}

	@Override
	@Command
	public MessageResponse logout() throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(new LogoutRequest());
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				Object responseObj = input.readObject();
				if(responseObj instanceof MessageResponse) {
					clientSocket.close();
					return (MessageResponse)responseObj;
				} else {
					return new MessageResponse("Unexpected communication error appeared");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		}
	}

	@Override
	@Command
	public MessageResponse exit() throws IOException {
		System.out.println("Shutting down client now");
		if(clientSocket != null) clientSocket.close();
		shellThread.interrupt();
		shell.close();
		System.in.close();
		return new MessageResponse("Client exited");
	}
}
