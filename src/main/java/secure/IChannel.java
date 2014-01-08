package secure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import message.Request;
import message.Response;

public interface IChannel {
	/*//Client
	public void sendMessageRequest(Request message) throws Exception;
	public Response  receiveMessageResponse() throws Exception;
	//Proxy
	public void sendMessageResponse(Request message) throws Exception;
	public Request receiveMessageRequest() throws Exception;*/
	
	public void sendByteArray(byte[] array) throws IOException;
	public byte[] receiveByteArray() throws ClassNotFoundException, IOException;
	
	public void closeChannel() throws Exception;
}
