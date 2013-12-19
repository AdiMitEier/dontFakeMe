package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import util.ChecksumUtils;
import message.Response;
import message.request.*;
import message.response.*;
import message.response.LoginResponse.Type;
import model.DownloadTicket;
import model.FileModel;
import model.FileServerModel;
import model.UserModel;

public class ProxyImpl implements IProxy, Runnable {
	
	private Socket socket;
	ProxyCliImpl proxyCli;
	UserModel currentUser = null;
	
	public ProxyImpl(Socket socket, ProxyCliImpl proxyCli) {
		this.socket = socket;
		this.proxyCli = proxyCli;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
				Object requestObj = input.readObject();
				if(requestObj instanceof LoginRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(login((LoginRequest)requestObj));
				}
				else if(requestObj instanceof LogoutRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(logout());
				}
				else if(requestObj instanceof CreditsRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(credits());
				}
				else if(requestObj instanceof BuyRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(buy((BuyRequest)requestObj));
				}
				else if(requestObj instanceof ListRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(list());
				}
				else if(requestObj instanceof UploadRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(upload((UploadRequest)requestObj));
				}
				else if(requestObj instanceof DownloadTicketRequest) {
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(download((DownloadTicketRequest)requestObj));
				}
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFoundException, really?");
			} catch (IOException e) {
				if(currentUser != null) currentUser.setOnline(false);
				System.out.println("Shut down client proxy instance");
				return;
			}
		}
	}

	@Override
	public LoginResponse login(LoginRequest request) throws IOException {
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
			Set<FileModel> combinedFileList = new HashSet<FileModel>();
			for(FileServerModel server : proxyCli.getFileServers()) {
				if(server.isOnline()) {
					combinedFileList.addAll(server.getFileList());
				}
			}
			return new ListResponse(combinedFileList);
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	public Response download(DownloadTicketRequest request) throws IOException {
		if(currentUser != null) {
			FileServerModel selectedServer = null;
			for(FileServerModel server : proxyCli.getFileServers()) {
				if(server.isOnline()) {
					if(server.getFileList().contains(request.getFilename())) {
						if(selectedServer == null || selectedServer.getUsage()>server.getUsage()) {
							selectedServer = server;
							System.out.println("Proxy: selected server on port "+selectedServer.getPort()+" Usage: "+selectedServer.getUsage());
						}
					}
				}
			}
			if(selectedServer == null) {
				return new MessageResponse("File not available!");
			} else {
				long fileSize = 0;
				int version = 0;
				try {
					Socket socket = new Socket(selectedServer.getAddress(),selectedServer.getPort());
					ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(new InfoRequest(request.getFilename()));
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof InfoResponse) {
							InfoResponse response = (InfoResponse)responseObj;
							fileSize = response.getSize();
						} else if(responseObj instanceof MessageResponse) {
							MessageResponse response = (MessageResponse)responseObj;
							System.out.println(response.toString());
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
					socket = new Socket(selectedServer.getAddress(),selectedServer.getPort());
					output = new ObjectOutputStream(socket.getOutputStream());
					output.writeObject(new VersionRequest(request.getFilename()));
					input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof VersionResponse) {
							VersionResponse response = (VersionResponse)responseObj;
							version = response.getVersion();
						} else if(responseObj instanceof MessageResponse) {
							MessageResponse response = (MessageResponse)responseObj;
							System.out.println(response.toString());
						}
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
						socket.close();
						return new MessageResponse("ClassNotFoundException, really?");
					} finally {
						socket.close();
					}
				} catch(IOException e) {
					return new MessageResponse("Could not connect to fileserver.");
				}
				proxyCli.increaseUsage(selectedServer, fileSize);
				proxyCli.changeCredits(currentUser, -fileSize);
				DownloadTicket ticket = new DownloadTicket(currentUser.getName(), request.getFilename(), ChecksumUtils.generateChecksum(currentUser.getName(), request.getFilename(), version, fileSize), selectedServer.getAddress(), selectedServer.getPort());
				return new DownloadTicketResponse(ticket);
			}
		}
		return new MessageResponse("You have to login first to perform this action!");
	}

	@Override
	//STAGE1
	public MessageResponse upload(UploadRequest request) throws IOException {
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
					output.writeObject(new VersionRequest(request.getFilename()));
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					try {
						Object responseObj = input.readObject();
						if(responseObj instanceof VersionResponse) {
							VersionResponse response = (VersionResponse)responseObj;
							if(response.getVersion()>mostRecentVersionNumber){
								mostRecentVersionNumber = response.getVersion();
							}
						}
					} catch (ClassNotFoundException e) {
						System.out.println("ClassNotFoundException, really?");
						socket.close();
						return new MessageResponse("ClassNotFoundException, really?");
					}
					socket.close();
				}
			}
			
			//uploading file with new version number
			for(FileServerModel server : proxyCli.getFileServersWithLowestUsage(proxyCli.getWriteQuorum())) {
				if(server.isOnline()) {
					request.setVersion(mostRecentVersionNumber);
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
							proxyCli.addToFileList(server, new FileModel(request.getFilename(),mostRecentVersionNumber));
							System.out.println("Proxy: "+response.toString());
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
			currentUser = null;
			return new MessageResponse("Successfully logged out.");
		}
		return new MessageResponse("You have to login first to perform this action!");
	}
}
