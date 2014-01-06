package secure;

import message.Request;
import message.Response;

public interface IChannel {
	//Client
	public void sendMessageRequest(Request message) throws Exception;
	public Response  receiveMessageResponse() throws Exception;
	//Proxy
	public void sendMessageResponse(Request message) throws Exception;
	public Request receiveMessageRequest() throws Exception;
	public void closeChannel() throws Exception;
}
