package secure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import message.Request;
import message.Response;
import org.bouncycastle.util.encoders.Base64;

public abstract class Base64Channel implements IChannel {
	protected IChannel base64Channel; //decorated
	
	protected Socket tcpchannelsocket;
	protected ObjectOutputStream output;
	protected ObjectInputStream input;
	
	public Base64Channel(IChannel base64Channel){
		this.base64Channel = base64Channel;
	}
	
	public Base64Channel(Socket s) throws IOException{
		this.tcpchannelsocket = s;
		this.output = new ObjectOutputStream(tcpchannelsocket.getOutputStream());
		this.input =  new ObjectInputStream(tcpchannelsocket.getInputStream());
	}
	//CLIENT
	@Override
	public void sendMessageRequest(Request message) throws Exception {
		byte[] messagearray = this.encodeBase64(this.toByteArray(message));
		this.output = new ObjectOutputStream(tcpchannelsocket.getOutputStream());
		this.output.writeObject(messagearray);
	}

	@Override
	public Response receiveMessageResponse() throws Exception {
		this.input = new ObjectInputStream(tcpchannelsocket.getInputStream());
		byte[] messagearray = (byte[]) input.readObject();
		messagearray = this.decodeBase64(messagearray);
		return (Response)this.byteArraytoObject(messagearray);
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
	@Override
	public void closeChannel() throws Exception {
		this.input.close();
		this.output.close();
		if(tcpchannelsocket != null) tcpchannelsocket.close();
		
	}
	/**
	 * Writes the given Object to a byte array
	 * @param Object o
	 * @return byte[]
	 * @throws Exception
	 */
	public byte[] toByteArray(Object o) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		byte[] array = baos.toByteArray();
		return array;
	}
	/**
	 * Writes the given byte Array to Object
	 * @param byte[] code
	 * @return Object
	 * @throws Exception
	 */
	public Object byteArraytoObject(byte[] code) throws Exception{
		ByteArrayInputStream in = new ByteArrayInputStream(code);
		ObjectInputStream oin = new ObjectInputStream(in);
		return oin.readObject();
	}
	/**
	 * Encodes Byte Array Base 64
	 * @param byte[] code
	 * @return byte[] encoded
	 */
	public byte[] encodeBase64(byte[] code){
		return Base64.encode(code);
	}
	/**
	 * Decodes Byte Array Base 64
	 * @param byte[] code
	 * @return byte[] decoded
	 */
	public byte[] decodeBase64(byte[] code){
		return Base64.decode(code);
	}
}
