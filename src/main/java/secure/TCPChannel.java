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
	}
	public void sendByteArray(byte[] array) throws IOException{
		this.output = new ObjectOutputStream(tcpchannelsocket.getOutputStream());
		this.output.writeObject(array);
	}
	public byte[] receiveByteArray() throws ClassNotFoundException, IOException{
		this.input = new ObjectInputStream(tcpchannelsocket.getInputStream());
		byte[] array = (byte[]) input.readObject();
		return array;
	}
	@Override
	public void closeChannel() throws Exception {
		this.input.close();
		this.output.close();
		if(tcpchannelsocket != null) tcpchannelsocket.close();
		
	}
}
