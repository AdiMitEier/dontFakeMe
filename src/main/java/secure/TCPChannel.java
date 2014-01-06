package secure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import message.Request;
import message.Response;



public class TCPChannel implements IChannel {

	private Socket tcpchannelsocket;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	
	public TCPChannel(Socket s) throws IOException{
		this.tcpchannelsocket = s;
		this.output = new ObjectOutputStream(tcpchannelsocket.getOutputStream());
		this.input =  new ObjectInputStream(tcpchannelsocket.getInputStream());
	}
	//CLIENT
	@Override
	public void sendMessageRequest(Request message) throws Exception {
		this.output = new ObjectOutputStream(tcpchannelsocket.getOutputStream());
		this.output.writeObject(message);
	}

	@Override
	public Response receiveMessageResponse() throws Exception {
		this.input = new ObjectInputStream(tcpchannelsocket.getInputStream());
		return (Response)input.readObject();
	}

	@Override
	public void closeChannel() throws Exception {
		this.input.close();
		this.output.close();
		if(tcpchannelsocket != null) tcpchannelsocket.close();
		
	}
	//PROXY
	@Override
	public void sendMessageResponse(Request message) throws Exception {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Request receiveMessageRequest() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
