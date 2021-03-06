package proxy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Hex;

import secure.AESChannel;
import secure.IChannel;
import secure.RSAChannel;
import secure.TCPChannel;
import util.ChecksumUtils;
import message.Request;
import message.Response;
import message.request.*;
import message.response.*;
import message.response.LoginResponse.Type;
import model.DownloadTicket;
import model.FileModel;
import model.FileServerModel;
import model.UserModel;

import java.security.KeyPair; 
import java.security.PrivateKey; 

import org.bouncycastle.openssl.PEMReader; 
import org.bouncycastle.openssl.PasswordFinder;

public class ProxyImpl implements IProxy, Runnable {
	
	private Socket socket;
	ProxyCliImpl proxyCli;
	UserModel currentUser = null;
	Key privateKey;
	
	public ProxyImpl(Socket socket, ProxyCliImpl proxyCli) {
		this.socket = socket;
		this.proxyCli = proxyCli;

		privateKey = proxyCli.getPrivateKey();
	}
	
	@Override
	public void run() {
		IChannel tcpchannel = null;
		try{
		tcpchannel = new TCPChannel(socket);
		} catch(IOException e){
			e.printStackTrace();
		}
		IChannel channel = new RSAChannel(tcpchannel);
		((RSAChannel)channel).setKey(privateKey);
		//
		// RSA VERFAHREN 
		//
		Request request = null;
		try{
			request = ((RSAChannel) channel).receiveMessageRequest();
			
			if((request instanceof LoginRequest)&&(channel instanceof RSAChannel)) {
				//SEND RESPONSE 
				LoginResponse response = new LoginResponse(Type.OK);
				response.setClientchallenge(((LoginRequest) request).getChallenge());
				byte[] secretkey = ((RSAChannel) channel).generateSecretKey();
				byte[] ivparam = ((RSAChannel) channel).generateSecureIV();
				byte[] proxychallenge = ((RSAChannel) channel).generateSecureChallenge();
				response.setSecretkey(secretkey);
				response.setIvparameter(ivparam);
				response.setProxychallenge(proxychallenge);
				((RSAChannel) channel).sendMessageResponse(response);
				
				// SWITCH TO AES CHANNEL
				// init AES Chann with paramterrs 
				channel = new AESChannel(tcpchannel);
				((AESChannel)channel).setSecretkey(secretkey);
				((AESChannel)channel).setIvparam(ivparam);
				
				Request reqmessage=((AESChannel)channel).receiveMessageRequest();
				byte[] pchal = ((LoginRequest)reqmessage).getChallenge();
				
				if(new String(proxychallenge).equals(new String(pchal))){
					
					//System.out.println("Proxychallenge erhalten juhuu");
					//LOGIN USER
					LoginResponse resp = this.login(((LoginRequest)request));
					((AESChannel)channel).sendMessageResponse(resp);
					 
				}
			}
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFoundException, really?");
			e.printStackTrace();
		} catch (IOException e) {
			if(currentUser != null) currentUser.setOnline(false);
			System.out.println("Shut down client proxy instance");
			return;
		} catch (InvalidKeyException e) {
			System.out.println("Unexpected security error appeared");
			return;
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Unexpected security error appeared");
			return;
		} catch (NoSuchPaddingException e) {
			System.out.println("Unexpected security error appeared");
			return;
		} catch (IllegalBlockSizeException e) {
			System.out.println("Unexpected security error appeared");
			return;
		} catch (BadPaddingException e) {
			System.out.println("Unexpected security error appeared");
			return;
		} catch (InvalidAlgorithmParameterException e) {
			System.out.println("Unexpected security error appeared");
			return;
		}
		
		while(true) {
			
				try{
					request = ((AESChannel) channel).receiveMessageRequest();
					
					/*if((request instanceof LoginRequest)&&(channel instanceof RSAChannel)) {
						//SEND RESPONSE 
						LoginResponse response = new LoginResponse(Type.OK);
						response.setClientchallenge(((LoginRequest) request).getChallenge());
						byte[] secretkey = ((RSAChannel) channel).generateSecretKey();
						byte[] ivparam = ((RSAChannel) channel).generateSecureIV();
						byte[] proxychallenge = ((RSAChannel) channel).generateSecureChallenge();
						response.setSecretkey(secretkey);
						response.setIvparameter(ivparam);
						response.setProxychallenge(proxychallenge);
						((RSAChannel) channel).sendMessageResponse(response);
						
						// SWITCH TO AES CHANNEL
						// init AES Chann with paramterrs 
						channel = new AESChannel(tcpchannel);
						((AESChannel)channel).setSecretkey(secretkey);
						((AESChannel)channel).setIvparam(ivparam);
						
						Request reqmessage=((AESChannel)channel).receiveMessageRequest();
						byte[] pchal = ((LoginRequest)reqmessage).getChallenge();
						
						if(new String(proxychallenge).equals(new String(pchal))){
							
							System.out.println("Proxychallenge erhalten juhuu");
							//LOGIN USER
							LoginResponse resp = this.login(((LoginRequest)request));
							((AESChannel)channel).sendMessageResponse(resp);
							 
						}
					}*/
					if(request instanceof LoginRequest){
						//TODO 
					}
					else if(request instanceof LogoutRequest) {
						//ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						//output.writeObject(logout());
						((AESChannel) channel).sendMessageResponse(logout());
					}
					else if(request instanceof CreditsRequest) {
						//ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						//output.writeObject(credits());
						((AESChannel) channel).sendMessageResponse(credits());
					}
					else if(request instanceof BuyRequest) {
						//ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						//output.writeObject(buy((BuyRequest)requestObj));
						((AESChannel) channel).sendMessageResponse(buy((BuyRequest)request));
					}
					else if(request instanceof ListRequest) {
						//ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						//output.writeObject(list());
						((AESChannel) channel).sendMessageResponse(list());
					}
					else if(request instanceof UploadRequest) {
						//ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						//output.writeObject(upload((UploadRequest)requestObj));
						((AESChannel) channel).sendMessageResponse(upload((UploadRequest)request));
					}
					else if(request instanceof DownloadTicketRequest) {
						//ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
						//output.writeObject(download((DownloadTicketRequest)requestObj));
						((AESChannel) channel).sendMessageResponse(download((DownloadTicketRequest)request));
					}
					
				} catch (ClassNotFoundException e) {
					System.out.println("ClassNotFoundException, really?");
					e.printStackTrace();
				} catch (IOException e) {
					if(currentUser != null) currentUser.setOnline(false);
					System.out.println("Shut down client proxy instance");
					return;
				} catch (InvalidKeyException e) {
					System.out.println("Unexpected security error appeared");
					return;
				} catch (NoSuchAlgorithmException e) {
					System.out.println("Unexpected security error appeared");
					return;
				} catch (NoSuchPaddingException e) {
					System.out.println("Unexpected security error appeared");
					return;
				} catch (IllegalBlockSizeException e) {
					System.out.println("Unexpected security error appeared");
					return;
				} catch (BadPaddingException e) {
					System.out.println("Unexpected security error appeared");
					return;
				} catch (InvalidAlgorithmParameterException e) {
					System.out.println("Unexpected security error appeared");
					return;
				}
		}
	}

	@Override
	public LoginResponse login(LoginRequest request) throws IOException {
		//System.out.println(proxyCli.getReadQuorum() + " "+ proxyCli.getWriteQuorum());
		for(UserModel user : proxyCli.getUsers()) {
			if(user.getName().compareTo(request.getUsername()) == 0 && user.getPassword().compareTo(request.getPassword()) == 0) {
				user.setOnline(true);
				currentUser = user;
				return new LoginResponse(Type.SUCCESS);
			}
		}
		return new LoginResponse(Type.WRONG_CREDENTIALS);
	}

	@Override
	public Response credits() throws IOException {
		if(currentUser != null) {
			return new CreditsResponse(currentUser.getCredits());
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	public Response buy(BuyRequest credits) throws IOException {
		if(currentUser != null) {
			proxyCli.changeCredits(currentUser, credits.getCredits());
			return new BuyResponse(currentUser.getCredits());
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	public Response list() throws IOException {
		if(currentUser != null) {
			return new ListResponse(proxyCli.getCombinedFileList());
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	public Response download(DownloadTicketRequest request) throws IOException {
		int version = 0;
		//request.setHmac(proxyCli.getSecretKey()); //STAGE3
		if(currentUser != null) {
			FileServerModel selectedServer = null;

			
			//if no quorums are set up than all fileservers are asked
			if(proxyCli.getReadQuorum()==-1 || proxyCli.getWriteQuorum()==-1){

				for(FileServerModel server : proxyCli.getFileServers()) {
					if(server.isOnline()) {
						boolean hasFile = false;
						for(FileModel file : server.getFileList()){
							if(file.getFilename().equals(request.getFilename()))
								hasFile = true;
						}
						if(hasFile) {
							if(selectedServer == null || selectedServer.getUsage()>server.getUsage()) {
								selectedServer = server;
								//System.out.println("Proxy: selected server on port "+selectedServer.getPort()+" Usage: "+selectedServer.getUsage());
							}
						}
					}
				}
			} else {
				//if there are quorums than NR fileservers are asked for the highest version of the file
				//the server with the highest version is the selected server
				//STAGE 1
				for(FileServerModel server : proxyCli.getFileServersWithLowestUsage(proxyCli.getReadQuorum())){
					Socket socket = new Socket(server.getAddress(),server.getPort());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					VersionRequest vr = new VersionRequest(request.getFilename());
					vr.setHmac(proxyCli.getSecretKey());
					output.writeObject(vr);
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof VersionResponse) {
							VersionResponse response = (VersionResponse)responseObj;
							int tmpVersion = response.getVersion();
							if(tmpVersion >= version){
								version = tmpVersion;
								selectedServer = server;
							}
						} else if(responseObj instanceof MessageResponse) {
							MessageResponse response = (MessageResponse)responseObj;
							//System.out.println(response.toString());
						}
						int errorcount = 0;
						while(responseObj instanceof HmacErrorResponse){
							errorcount++;
							Socket s = new Socket(server.getAddress(),server.getPort());
							ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
							o.writeObject(vr);
							ObjectInputStream i = new ObjectInputStream(s.getInputStream());
							responseObj = i.readObject();
							if(responseObj instanceof VersionResponse){
								VersionResponse response = (VersionResponse)responseObj;
								int tmpVersion = response.getVersion();
								if(tmpVersion >= version){
									version = tmpVersion;
									selectedServer = server;
								}
								s.close();
								break;
							}
							if(errorcount > 4){
								System.out.println("Failed at verifing message Integrity. Debug:VersionResponse");
								s.close();
								return new MessageResponse("Failed at verifying message Integrity. Debug:VersionResponse");
							}
							s.close();
						}
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
						socket.close();
						return new MessageResponse("ClassNotFoundException, really?");
					} finally {
						socket.close();
					}
				}
			}
			
			if(selectedServer == null) {
				return new MessageResponse("File not available!");
			} else {
				long fileSize = 0;
				try {
					Socket socket = new Socket(selectedServer.getAddress(),selectedServer.getPort());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					InfoRequest ir = new InfoRequest(request.getFilename());
					ir.setHmac(proxyCli.getSecretKey());
					output.writeObject(ir);
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof InfoResponse) {
							InfoResponse response = (InfoResponse)responseObj;
							fileSize = response.getSize();
						} else if(responseObj instanceof MessageResponse) {
							MessageResponse response = (MessageResponse)responseObj;
							//System.out.println(response.toString());
						}
						int errorcount = 0;
						while(responseObj instanceof HmacErrorResponse){
							errorcount++;
							Socket s = new Socket(selectedServer.getAddress(),selectedServer.getPort());
							ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
							o.writeObject(ir);
							ObjectInputStream i = new ObjectInputStream(s.getInputStream());
							responseObj = i.readObject();
							if(responseObj instanceof InfoResponse){
								InfoResponse response = (InfoResponse)responseObj;
								fileSize = response.getSize();
								s.close();
								break;
							}
							if(errorcount > 4){
								System.out.println("Failed at verifing message Integrity. Debug:InfoResponse");
								s.close();
								return new MessageResponse("Failed at verifying message Integrity. Debug:InfoResponse");
							}
							s.close();
						}
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
						socket.close();
						return new MessageResponse("ClassNotFoundException, really?");
					} finally {
						socket.close();
					}
					if(currentUser.getCredits() < fileSize) {
						return new MessageResponse("You don't have enough credits to download this file!");
					}
				} catch(IOException e) {
					return new MessageResponse("Could not connect to fileserver.");
				}
				proxyCli.increaseUsage(selectedServer, fileSize);
				proxyCli.changeCredits(currentUser, -fileSize);
				proxyCli.increaseDownloads(request.getFilename());
				DownloadTicket ticket = new DownloadTicket(currentUser.getName(), request.getFilename(), ChecksumUtils.generateChecksum(currentUser.getName(), request.getFilename(), version, fileSize), selectedServer.getAddress(), selectedServer.getPort());
				return new DownloadTicketResponse(ticket);
			}
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	//STAGE1
	public MessageResponse upload(UploadRequest request) throws IOException {
		
		//System.out.println(request.toString());
		if(currentUser != null) {
			int mostRecentVersionNumber = 0;
			
			if(proxyCli.getReadQuorum()==-1 || proxyCli.getWriteQuorum()==-1){
				proxyCli.initQuorums(proxyCli.getReadQuorum(),proxyCli.getWriteQuorum());
			}
			
			//checking the most recent file version number for the file in the upload request
			for(FileServerModel server : proxyCli.getFileServersWithLowestUsage(proxyCli.getReadQuorum())){
				if(server.isOnline()){
					Socket socket;
					socket = new Socket(server.getAddress(),server.getPort());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					VersionRequest vr = new VersionRequest(request.getFilename());
					vr.setHmac(proxyCli.getSecretKey());
					output.writeObject(vr);
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof VersionResponse) {
							VersionResponse response = (VersionResponse)responseObj;
							if(response.getVersion()>mostRecentVersionNumber){
								mostRecentVersionNumber = response.getVersion();
							}
						}
						//if something is wrong with the integrity, the proxy will try to
						//resend the request for 5 times, if that fails upload is aborted
						int errorcount = 0;
						while(responseObj instanceof HmacErrorResponse){
							errorcount++;
							Socket s = new Socket(server.getAddress(),server.getPort());
							ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
							o.writeObject(vr);
							ObjectInputStream i = new ObjectInputStream(s.getInputStream());
							responseObj = i.readObject();
							if(responseObj instanceof VersionResponse){
								VersionResponse response = (VersionResponse)responseObj;
								if(response.getVersion()>mostRecentVersionNumber){
									mostRecentVersionNumber = response.getVersion();
								}
								s.close();
								break;
							}
							if(errorcount > 4){
								System.out.println("Failed at verifing message Integrity. Debug:VersionResponse");
								s.close();
								return new MessageResponse("Failed at verifying message Integrity. Debug:VersionResponse");
							}
							s.close();
						}
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
						socket.close();
						return new MessageResponse("ClassNotFoundException, really?");
					}
					socket.close();
				}
			}
			request.setVersion(mostRecentVersionNumber);
			request.setHmac(proxyCli.getSecretKey()); //STAGE3
			
			//uploading file with new version number
			for(FileServerModel server : proxyCli.getFileServersWithLowestUsage(proxyCli.getWriteQuorum())) {
				if(server.isOnline()) {
					Socket socket;
					socket = new Socket(server.getAddress(),server.getPort());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(request);
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof MessageResponse) {
							MessageResponse response = (MessageResponse)responseObj;
							proxyCli.increaseUsage(server,request.getContent().length);
							proxyCli.addToFileList(server, new FileModel(request.getFilename(),mostRecentVersionNumber+1));
							//System.out.println("Proxy: "+response.toString());
						}
						int errorcount = 0;
						while(responseObj instanceof HmacErrorResponse){
							errorcount++;
							Socket s = new Socket(server.getAddress(),server.getPort());
							ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
							o.writeObject(request);
							ObjectInputStream i = new ObjectInputStream(s.getInputStream());
							responseObj = i.readObject();
							if(responseObj instanceof MessageResponse){
								MessageResponse response = (MessageResponse)responseObj;
								proxyCli.increaseUsage(server,request.getContent().length);
								proxyCli.addToFileList(server, new FileModel(request.getFilename(),mostRecentVersionNumber+1));
								//System.out.println("Proxy: "+response.toString());
								s.close();
								break;
							}
							if(errorcount > 4){
								System.out.println("Failed at verifing message Integrity. Debug:UploadResponse");
								s.close();
								return new MessageResponse("Failed at verifying message Integrity. Debug:UploadResponse");
							}
							s.close();
						}	
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
						socket.close();
						return new MessageResponse("ClassNotFoundException, really?");
					}
					socket.close();
				}
			}
			proxyCli.changeCredits(currentUser, request.getContent().length*2);
			return new MessageResponse("File successfully uploaded. You now have " + currentUser.getCredits() + " credits!");
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	public MessageResponse logout() throws IOException {
		if(currentUser != null) {
			currentUser.setOnline(false);
			currentUser.getSubscriptions().clear();
			currentUser = null;
			return new MessageResponse("Successfully logged out.");
		}
		return new MessageResponse("You have to login first to perform this action!");
	}
}
