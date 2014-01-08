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
import message.response.LoginResponse.Type;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;

public class RSAChannel extends Base64Channel{

	private Cipher encrypt;
	private Cipher decrypt;
	private Key key;
	public RSAChannel(IChannel base64Channel) {
		super(base64Channel);
	}
	/*public RSAChannel(IChannel base64Channel,Key publickey) {
		super(base64Channel);
		this.publickey=publickey;
	}*/
	public void setKey(Key key){
			this.key=key;
	}
	//CLIENT
	// !login <username> <client-challenge> <password>
	public void sendMessageRequest(Request message) throws IllegalBlockSizeException, BadPaddingException, Exception{
		if(message instanceof LoginRequest){
		byte[] encode = this.encodeBase64(((LoginRequest) message).getChallenge());
		String m = "!login "+((LoginRequest) message).getUsername()+ " " +new String(encode)+ " "+((LoginRequest) message).getPassword();
		//RSA MESSAGE
		initencryptChipher();
		byte[]rsa = encrypt.doFinal(m.getBytes());		
		//ENCODE BASE64
		this.sendByteArray(rsa);
		}
	}
	
	public Response receiveMessageResponse() throws ClassNotFoundException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
		
		byte[] rec = this.receiveByteArray();
		//RSA DECODE
		this.initdecryptChipher();
		byte[]rsa = decrypt.doFinal(rec);
		
		//BAS64 DECODE
		String all = new String(rsa);
		//System.out.println("Before Base 64 DeCODE: "+all);
		Scanner scan = new Scanner(all);
			//while(scan.hasNext()){
		String ok = scan.next();
		String chal = scan.next();
		String proxyc = scan.next();
		String secret = scan.next();
		String iv = scan.next();
			//}
			
		//GET CHALLENGE AND BASE 64 ENCODE
		//System.out.println("client recceive Before DECODE Base64 :"+chal);
		byte[] challenge = this.decodeBase64(chal.getBytes());
		//System.out.println("client recceive After DECODE Base64 :"+new String(challenge));
		//GET PROXY CHALLENGE AND ENCODE BASE 64
		byte[] proxychallenge = this.decodeBase64(proxyc.getBytes());
		//GET PROXY SECRET 
		byte[] secretkey = this.decodeBase64(secret.getBytes());
		//GET PROXY IV
		byte[] ivparam = this.decodeBase64(iv.getBytes());
			
		LoginResponse response = new LoginResponse(Type.OK);
		response.setClientchallenge(challenge);
		response.setProxychallenge(proxychallenge);
		response.setSecretkey(secretkey);
		response.setIvparameter(ivparam);
		
		return response;
	}
	
	//PROXY
	
	public void sendMessageResponse(Response message) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
	
		if(message instanceof LoginResponse){
			//GET CHALLENGE AND BASE 64 ENCODE
			byte[] challenge=this.encodeBase64(((LoginResponse) message).getClientchallenge());
			//byte[] challenge=((LoginResponse) message).getClientchallenge();
			//GET PROXY CHALLENGE AND ENCODE BASE 64
			byte[] proxychallenge = this.encodeBase64(((LoginResponse) message).getProxychallenge());
			//GET PROXY SECRET 
			byte[] secretkey = this.encodeBase64(((LoginResponse) message).getSecretkey());
			//GET PROXY IV
			byte[] ivparam = this.encodeBase64(((LoginResponse) message).getIvparameter());
			
			String m = "!ok "+new String(challenge)+ " " +new String(proxychallenge)+" "+new String(secretkey)+" "+new String(ivparam);
			//System.out.println("Before RSA AFTER BASE 64DECODE "+m);
			byte[]brsa = m.getBytes();
			//System.out.println("before rsa: "+brsa.length);
			//RSA & init Chipher
			this.initencryptChipher();
			byte[] rsa = encrypt.doFinal(brsa);
			//System.out.println("nach rsa: "+rsa.length);
			this.sendByteArray(rsa);
		}
	}
	//!login <username> <client-challenge> <password>
	public Request receiveMessageRequest() throws ClassNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
		//BASE 64 
		byte[] rec = this.receiveByteArray();
		// RSA DECODEN
		this.initdecryptChipher();
		byte[]rsa = this.decrypt.doFinal(rec);
		// TO OBJECT STRING !login <username> <client-challenge>
		String m = new String(rsa);
		Scanner scan = new Scanner(m);
		String login = scan.next();
		String username = scan.next();
		String challenge = scan.next();
		String pw = scan.next();
		
		byte[]chall = this.decodeBase64(challenge.getBytes());
		
		LoginRequest loginrequest = new LoginRequest(username,pw);
		loginrequest.setChallenge(chall);
		
		return loginrequest;
	}
	public byte[] generateSecretKey(){
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32]; //256 bit
		secureRandom.nextBytes(number);
		return number;
	}
	public byte[] generateSecureIV(){
		SecureRandom secureRandom = new SecureRandom(); 
		final byte[] number = new byte[16]; 
		secureRandom.nextBytes(number);
		return number;
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
		encrypt.init(Cipher.ENCRYPT_MODE,this.key);
	}
	public void initdecryptChipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		this.decrypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		//System.out.println(publickey);
		decrypt.init(Cipher.DECRYPT_MODE,this.key);
	}
	//TODO receive Message
	
}
