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
	protected IChannel tcpChannel; //decorated
	
	public Base64Channel(IChannel base64Channel){
		this.tcpChannel = base64Channel;
	}
	public void sendByteArray(byte[] array) throws IOException {
		byte[] encoded = this.encodeBase64(array);
		tcpChannel.sendByteArray(encoded);
	}
	public byte[] receiveByteArray() throws ClassNotFoundException, IOException{
		byte[] rec = tcpChannel.receiveByteArray();
		return this.decodeBase64(rec);
	}

	@Override
	public void closeChannel() throws Exception {
		tcpChannel.closeChannel();	
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
