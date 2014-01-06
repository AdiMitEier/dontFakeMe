package secure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import message.Request;
import message.Response;
import message.Request.*;
import message.Response.*;
import message.request.LoginRequest;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RSAChannel extends Base64Channel{

	private Cipher encrypt;
	private Cipher decrypt;
	private Key publickey;
	
	public RSAChannel(IChannel base64Channel) {
		super(base64Channel);
	}
	public RSAChannel(Socket s,Key publickey) throws IOException {
		super(s);
		this.publickey = publickey;
	}
	//CLIENT
	@Override
	public void sendMessageRequest(Request message) throws Exception {
		//CHALLENGE for login
		byte[] encode = this.encodeBase64(this.generateSecureChallenge());
		if(message instanceof LoginRequest){
			message = new LoginRequest(((LoginRequest) message).getUsername(),((LoginRequest) message).getPassword(),encode);
		}
		
		//RSA MESSAGE
		initencryptChipher();
		byte[]rsa = encrypt.doFinal(this.toByteArray(message));
	
		//ENCODE BASE64
		byte[] messagearray = this.encodeBase64(rsa);
		this.output = new ObjectOutputStream(tcpchannelsocket.getOutputStream());
		this.output.writeObject(messagearray);
	}
	@Override
	public Response receiveMessageResponse() throws Exception {
		return null;
		
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
	public byte[] generateSecureChallenge(){
		// generates a 32 byte secure random number 
		SecureRandom secureRandom = new SecureRandom(); 
		final byte[] number = new byte[32]; 
		secureRandom.nextBytes(number);
		return number;
	}
	public void initencryptChipher() throws Exception{
		this.encrypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		encrypt.init(Cipher.ENCRYPT_MODE,this.publickey);
	}
	public void initdecryptChipher() throws Exception{
		this.decrypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		encrypt.init(Cipher.DECRYPT_MODE,this.publickey);
	}
	//TODO receive Message
	
}
