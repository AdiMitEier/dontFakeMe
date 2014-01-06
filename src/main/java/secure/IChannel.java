package secure;

import message.Request;
import message.Response;

public interface IChannel {
	public void sendMessage(Request message) throws Exception;
	public Response  receiveMessage() throws Exception;
	public void closeChannel() throws Exception;
}
