package client;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import secure.TCPChannel;

import proxy.IProxyRMI;
import secure.AESChannel;
import secure.IChannel;
import secure.RSAChannel;
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

public class ClientCliImpl implements IClientCli, IClientRMI {
	
	private Config config;
	private Shell shell;
	private String dir;
	private String host;
	private int tcpPort;
	private Config mcConfig;
	private String bindingName;
	private String proxyHost;
	private int proxyRMIPort;
	private String keysDir;
	private String publicKeyDir;
	private String loggedInUserName;
	Registry registry;
	IProxyRMI proxyRMI;
	IChannel channel;
	IChannel tcpchannel;
	
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
		readMCConfig();
		this.shell.register(this);
		shellThread = new Thread(this.shell);
		shellThread.start();
		try {
			UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getLoggedInUserName() {
		return loggedInUserName;
	}

	private void readConfig() {
		tcpPort = config.getInt("proxy.tcp.port");
		host = config.getString("proxy.host");
		dir = config.getString("download.dir");
		publicKeyDir = config.getString("proxy.key");
	}
	
	private void readMCConfig() {
		mcConfig = new Config("mc");
		bindingName = mcConfig.getString("binding.name");
		proxyHost = mcConfig.getString("proxy.host");
		proxyRMIPort = mcConfig.getInt("proxy.rmi.port");
		keysDir = mcConfig.getString("keys.dir");
	}
	
	private boolean initRMI() {
		if(proxyRMI != null) return true;
		try {
			registry = LocateRegistry.getRegistry(proxyHost,proxyRMIPort);
			proxyRMI = (IProxyRMI)registry.lookup(bindingName);
			return true;
		} catch (RemoteException e) {
			System.out.println("No proxy available, cannot init RMI now");
		} catch (NotBoundException e) {
			System.out.println("No proxy available, cannot init RMI now");
		}
		return false;
	}
	
	@Override
	@Command
	public LoginResponse login(String username, String password) throws IOException{
		if(clientSocket != null && !clientSocket.isClosed()) {
			System.out.println("You are already logged in!");
			return new LoginResponse(Type.WRONG_CREDENTIALS);
		}
		try {
			
			PEMReader in = new PEMReader(new FileReader(publicKeyDir)); 
			PublicKey publicKey = (PublicKey) in.readObject();
			in.close();
			
			clientSocket = new Socket(host,tcpPort);
			//Socket socket = new Socket(host,tcpPort);
			this.tcpchannel = new TCPChannel(clientSocket);
			//RSAChannel channel = new RSAChannel(tcpchannel,publicKey);
			this.channel = new RSAChannel(tcpchannel);
			((RSAChannel)channel).setKey(publicKey);
			
			//try {
				//LOGIN REQEUST ERSTELLEN MIT CHALLENGE NUMBER
				LoginRequest loginreq=new LoginRequest(username, password);
				byte[] loginchallenge = ((RSAChannel) channel).generateSecureChallenge();
				loginreq.setChallenge(loginchallenge);
				
				//SENDE LOGIN REQUEST
				((RSAChannel) channel).sendMessageRequest(loginreq);
				
				//EMPFANGE RESPONSE
				Response response = ((RSAChannel) channel).receiveMessageResponse();
				if(response instanceof LoginResponse)
				{
					response = (LoginResponse)response;
					if(((LoginResponse) response).getType()==LoginResponse.Type.OK)
					{
						String challenge = new String(((LoginResponse)response).getClientchallenge());
						if(new String(loginchallenge).equals(challenge))
						{
							System.out.println("Response Erhalten & Challenge richtig");
							
							//SWITCH TO AES CHANNEL & SEND PROXY CHALLENGE BACK
							byte[] proxychallenge = ((LoginResponse)response).getProxychallenge();
							byte[] ivparam = ((LoginResponse)response).getIvparameter();
							byte[] secret = ((LoginResponse)response).getSecretkey();
							//AES CHANNEL
							channel = new AESChannel(tcpchannel);
							((AESChannel)channel).setSecretkey(secret);
							((AESChannel)channel).setIvparam(ivparam);
							// SENDE PROXY CHALLENGE 
							LoginRequest loginproxychallenge = new LoginRequest(username,password);
							loginproxychallenge.setChallenge(proxychallenge);
							((AESChannel)channel).sendMessageRequest(loginproxychallenge);
							
							/*Response reslogin=((AESChannel)channel).receiveMessageResponse();
							if(reslogin  instanceof LoginResponse){
								if(((LoginResponse) reslogin).getType()==LoginResponse.Type.SUCCESS){
									System.out.println("succes fully logged in ");
								}
							}*/
							
							//EMPFANGE LOGIN RESPONSE 
							Response res = ((AESChannel)channel).receiveMessageResponse();
							LoginResponse responsex = (LoginResponse)res;
							
							if(responsex.getType() == Type.WRONG_CREDENTIALS)
							{
								clientSocket.close();
							}
							loggedInUserName = username;
							return responsex;
						}
				}
			}
			
		}catch(IOException e) {
				System.out.println("Connection error");
				e.printStackTrace();
				return new LoginResponse(Type.WRONG_CREDENTIALS);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new LoginResponse(Type.WRONG_CREDENTIALS);
		/*	
		try {
				//EMPFANGE LOGIN RESPONSE 
				Response res;
				try {
					res = ((AESChannel)channel).receiveMessageResponse();
					LoginResponse responsefinal = (LoginResponse)res;

					if(responsefinal.getType() == Type.WRONG_CREDENTIALS) {
						clientSocket.close();
					}
					loggedInUserName = username;
					return responsefinal;
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (IllegalBlockSizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BadPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidAlgorithmParameterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		} catch(IOException e) {
			System.out.println("Connection error");
			e.printStackTrace();
			return new LoginResponse(Type.WRONG_CREDENTIALS);
		} */
	}

	@Override
	@Command
	public Response credits() throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			//ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			//output.writeObject(new CreditsRequest());
			//ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			((AESChannel)channel).sendMessageRequest(new CreditsRequest());
			try {
				return (Response)((AESChannel)channel).receiveMessageResponse();
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MessageResponse("Unexpected communication error appeared");
	}

	@Override
	@Command
	public Response buy(long credits) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			//ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			//output.writeObject(new BuyRequest(credits));
			//ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			((AESChannel)channel).sendMessageRequest(new BuyRequest(credits));
			try {
				return (Response) ((AESChannel)channel).receiveMessageResponse();
				//return (Response)input.readObject();
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MessageResponse("Unexpected communication error appeared");
	}

	@Override
	@Command
	public Response list() throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			ListRequest request = new ListRequest();
			//ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			//output.writeObject(request);
			((AESChannel) channel).sendMessageRequest(request);
			
			//ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			try {
				return (Response)((AESChannel) channel).receiveMessageResponse();
				//return (Response)input.readObject();
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MessageResponse("Unexpected communication error appeared");
	}

	@Override
	@Command
	public Response download(String filename) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			//ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			//output.writeObject(new DownloadTicketRequest(filename));
			//ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			((AESChannel) channel).sendMessageRequest(new DownloadTicketRequest(filename));
			
			try {
				//Object responseObj = input.readObject();
				Response responseObj=((AESChannel) channel).receiveMessageResponse();
				
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
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MessageResponse("Unexpected communication error appeared");
	}

	@Override
	@Command
	public MessageResponse upload(String filename) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		byte[] data = FileUtils.readFile(dir,filename);
		if(data == null) return new MessageResponse("Cannot read file");
		try {
			//ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			//output.writeObject(new UploadRequest(filename, 0, data));
			//ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			//TODO
			((AESChannel) channel).sendMessageRequest(new UploadRequest(filename, 0, data));
			try {
				//Object responseObj = input.readObject();
				Response responseObj = ((AESChannel) channel).receiveMessageResponse();
				if(responseObj instanceof MessageResponse) {
					return (MessageResponse)responseObj;
				} else {
					return new MessageResponse("Unexpected communication error appeared");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MessageResponse("Connection error");
	}

	@Override
	@Command
	public MessageResponse logout() throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		try {
			//ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			//output.writeObject(new LogoutRequest());
			//ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			((AESChannel)channel).sendMessageRequest(new LogoutRequest());
			try {
				//Object responseObj = input.readObject();
				Response responseObj =((AESChannel)channel).receiveMessageResponse();
				if(responseObj instanceof MessageResponse) {
					clientSocket.close();
					return (MessageResponse)responseObj;
				} else {
					return new MessageResponse("Unexpected communication error appeared");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
				return new MessageResponse("Unexpected communication error appeared");
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch(IOException e) {
			return new MessageResponse("Connection error");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new MessageResponse("Unexpected communication error appeared");
	}

	@Override
	@Command
	public MessageResponse exit() throws IOException {
		//System.out.println("Shutting down client now");
		logout();
		loggedInUserName = null;
		if(clientSocket != null) clientSocket.close();
		UnicastRemoteObject.unexportObject(this,false);
		shellThread.interrupt();
		shell.close();
		System.in.close();
		return new MessageResponse("Client exited");
	}

	@Override
	@Command
	public MessageResponse readQuorum() throws IOException {
		if(initRMI()) return proxyRMI.readQuorum();
		else return new MessageResponse("Cannot connect to Proxy via RMI");
	}

	@Override
	@Command
	public MessageResponse writeQuorum() throws IOException {
		if(initRMI()) return proxyRMI.writeQuorum();
		else return new MessageResponse("Cannot connect to Proxy via RMI");
	}

	@Override
	public void notifySubscription(String file, int number) throws RemoteException {
		System.out.println("Notification: "+file+" got downloaded "+number+" times!.");
	}

	@Override
	@Command
	public MessageResponse subscribe(String fileName, int number) throws IOException {
		if(clientSocket == null) return new MessageResponse("Please login to perform this action");
		if(number <= 0) return new MessageResponse("Number must be greater than 0");
		if(initRMI()) return proxyRMI.subscribe(this,loggedInUserName,fileName,number);
		else return new MessageResponse("Cannot connect to Proxy via RMI");
	}

	@Override
	@Command
	public Response topThreeDownloads() throws IOException {
		if(initRMI()) return proxyRMI.topThreeDownloads();
		else return new MessageResponse("Cannot connect to Proxy via RMI");
	}

	@Override
	@Command
	public MessageResponse getProxyPublicKey() throws IOException {
		if(initRMI()) {
			ProxyPublicKeyResponse response = proxyRMI.getProxyPublicKey();
			if(!response.hasKey()) return new MessageResponse(response.toString());
			else {
				String pathToPublicKey = keysDir+"/proxy.pub.pem";
				PEMWriter out = new PEMWriter(new FileWriter(pathToPublicKey)); 
				out.writeObject(response.getKey());
				out.close();
				return new MessageResponse("Successfully received public key of Proxy.");
			}
		}
		else return new MessageResponse("Cannot connect to Proxy via RMI");
	}

	@Override
	@Command
	public MessageResponse setUserPublicKey(String userName) throws IOException {
		if(initRMI()) {
			String pathToPublicKey = keysDir+"/"+userName+".pub.pem";
			PEMReader in;
			try {
				in = new PEMReader(new FileReader(pathToPublicKey));
				PublicKey publicKey = (PublicKey) in.readObject();
				in.close();
				return proxyRMI.setUserPublicKey(userName, publicKey);
			} catch (FileNotFoundException e) {
				return new MessageResponse("Key for user "+userName+" not found");
			}
		}
		else return new MessageResponse("Cannot connect to Proxy via RMI");
	}
}
