package secure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import message.Request;
import message.Response;
import message.Request.*;
import message.Response.*;
import message.request.LoginRequest;
import message.response.LoginResponse;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RSAChannel extends Base64Channel{

	private Cipher encrypt;
	private Cipher decrypt;
	private Key publickey;
	
	public RSAChannel(IChannel base64Channel,Key publickey) {
		super(base64Channel);
		this.publickey=publickey;
	}
	
	//CLIENT
	
	public void sendMessageRequest(Request message) throws IllegalBlockSizeException, BadPaddingException, Exception{
		//CHALLENGE for login & BASE64
		byte[] encode = this.encodeBase64(this.generateSecureChallenge());
		if(message instanceof LoginRequest){
			((LoginRequest) message).setChallenge(encode);
		}
		
		//RSA MESSAGE(
		initencryptChipher();
		byte[]rsa = encrypt.doFinal(this.toByteArray(message));		
	
		//ENCODE BASE64
		this.sendByteArray(rsa);
	}
	
	public Response receiveMessageResponse() throws Exception {
		/*//DECODE BASE64
		this.input = new ObjectInputStream(tcpchannelsocket.getInputStream());
		byte[] messagearray = (byte[]) input.readObject();
		messagearray = this.decodeBase64(messagearray);
		
		//DECODE RSA
		initdecryptChipher();
		byte[]rsa = decrypt.doFinal(messagearray);
		
		//DECODE BASE64 CHALLENGE LOGIN
		Response res = (Response)this.byteArraytoObject(rsa);
		
		if(res instanceof LoginResponse){
			LoginResponse response = (LoginResponse)res;
			//byte[] challenge = res.
			//TODO 
		}*/
		//return (Response)res;
		return null;
	}
	
	//PROXY
	
	public void sendMessageResponse(Response message) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public Request receiveMessageRequest() throws Exception {
		//BASE 64 
		byte[] rec = this.receiveByteArray();
		// RSA DECODEN
		this.initdecryptChipher();
		byte[]rsa = this.decrypt.doFinal(rec);
		// TO OBJECT
		Request reg = (Request)this.byteArraytoObject(rsa);
		if(reg instanceof LoginRequest){
			reg=(LoginRequest)reg;
			byte[]challenge= ((LoginRequest) reg).getChallenge();
			((LoginRequest) reg).setChallenge(this.decodeBase64(challenge));
		}
		
		return reg;
	}
	public byte[] generateSecureChallenge(){
		// generates a 32 byte secure random number 
		SecureRandom secureRandom = new SecureRandom(); 
		final byte[] number = new byte[32]; 
		secureRandom.nextBytes(number);
		return number;
	}
	public void initencryptChipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
		this.encrypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		encrypt.init(Cipher.ENCRYPT_MODE,this.publickey);
	}
	public void initdecryptChipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		this.decrypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		encrypt.init(Cipher.DECRYPT_MODE,this.publickey);
	}
	//TODO receive Message
	
}
