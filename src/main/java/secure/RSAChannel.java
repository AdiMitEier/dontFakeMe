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
	
	public Response receiveMessageResponse() throws ClassNotFoundException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
		/*//BASE 64 
		byte[] rec = this.receiveByteArray();
		// RSA DECODEN
		this.initdecryptChipher();
		byte[]rsa = this.decrypt.doFinal(rec);
		// TO OBJECT
		Response reg = (Response)this.byteArraytoObject(rsa);
		if(reg instanceof LoginResponse){
			reg=(LoginResponse)reg;
			//DECODE CLIENT CHALLENGE
			((LoginResponse) reg).setClientchallenge(this.decodeBase64(((LoginResponse) reg).getClientchallenge()));
			//DECODE PROXY CHALLENGE
			((LoginResponse) reg).setProxychallenge(this.decodeBase64(((LoginResponse) reg).getProxychallenge()));
			//DECODE SECRET KEY
			((LoginResponse) reg).setSecretkey(this.decodeBase64(((LoginResponse) reg).getSecretkey()));
			//DECODE IV PARAM
			((LoginResponse) reg).setIvparameter(this.decodeBase64(((LoginResponse) reg).getIvparameter()));
		}
		return reg;*/
		/*
		//BASE 64 
		byte[] rec = this.receiveByteArray();
		// TO OBJECT
		Response reg = (Response)this.byteArraytoObject(rec);
		
		if(reg instanceof LoginResponse){
			reg=(LoginResponse)reg;
			
			//DECODE CLIENT CHALLENGE RSA & BASE 64
			this.initdecryptChipher();
			byte[] challenge = ((LoginResponse) reg).getClientchallenge();
			((LoginResponse) reg).setClientchallenge(this.decodeBase64(decrypt.doFinal(challenge)));
			
			//DECODE PROXY CHALLENGE RSA & BASE 64
			byte[] proxychallenge = ((LoginResponse) reg).getProxychallenge();
			((LoginResponse) reg).setProxychallenge(this.decodeBase64(decrypt.doFinal(proxychallenge)));
			
			//DECODE SECRET KEY RSA & BASE 64
			byte[] secretkey = ((LoginResponse) reg).getSecretkey();
			((LoginResponse) reg).setSecretkey(this.decodeBase64(decrypt.doFinal(secretkey)));
			
			//DECODE IV PARAM RSA & BASE 64
			byte[] ivparam = ((LoginResponse) reg).getIvparameter();
			((LoginResponse) reg).setIvparameter(this.decodeBase64(decrypt.doFinal(ivparam)));
		}
		return reg;*/
		/*byte[] rec = this.receiveByteArray();
		// TO OBJECT
		Response message = (Response)this.byteArraytoObject(rec); 
		
		if(message instanceof LoginResponse){
			//RSA DECODE
			String all = new String(((LoginResponse) message).getMessage());
			this.initdecryptChipher();
			byte[]rsa = decrypt.doFinal(all.getBytes());
			//BAS64 DECODE
			all = new String(rsa);
			Scanner scan = new Scanner(all);
			//while(scan.hasNext()){
				String chal = scan.next();
				String proxyc = scan.next();
				String secret = scan.next();
				String iv = scan.next();
			//}
			
			//GET CHALLENGE AND BASE 64 ENCODE
			byte[] challenge = this.decodeBase64(chal.getBytes());
			//GET PROXY CHALLENGE AND ENCODE BASE 64
			byte[] proxychallenge = this.decodeBase64(proxyc.getBytes());
			//GET PROXY SECRET 
			byte[] secretkey = this.decodeBase64(secret.getBytes());
			//GET PROXY IV
			byte[] ivparam = this.decodeBase64(iv.getBytes());
			
			String m = new String(challenge)+ " " +new String(proxychallenge)+" "+new String(secretkey)+" "+new String(ivparam);
			
			((LoginResponse) message).setMessage(m.getBytes());
		}
		return message;*/
		
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
		/*if(message instanceof LoginResponse){
		//GET CHALLENGE AND BASE 64 ENCODE
		byte[] challenge=((LoginResponse) message).getClientchallenge();
		((LoginResponse) message).setClientchallenge(this.encodeBase64(challenge));
		//BASE 64 ENCODE PROXY CHALLENGE
		((LoginResponse) message).setProxychallenge(this.encodeBase64(this.generateSecureChallenge()));
		//BASE 64 ENCODE PROXY SECRET KEY
		((LoginResponse) message).setSecretkey(this.encodeBase64(this.generateSecretKey()));
		//BASE 64 ENCODE PROXY IV PARAMETER
		((LoginResponse) message).setIvparameter(this.encodeBase64(this.generateSecureIV()));
		//RSA ENCODEN & toArray
		byte[] array = this.toByteArray(message);
		this.initencryptChipher();
		byte[]rsa=encrypt.doFinal(array);
		//ENCODE BASE 64 & SEND
		this.tcpChannel.sendByteArray(rsa);*/
		
		/*if(message instanceof LoginResponse){
			
			//GET CHALLENGE AND BASE 64 ENCODE
			byte[] challenge=this.encodeBase64(((LoginResponse) message).getClientchallenge());
			
			//RSA CHALLENGE & init Chipher
			this.initencryptChipher();
			((LoginResponse) message).setClientchallenge(encrypt.doFinal(challenge));
			
			//BASE 64 ENCODE & RSA PROXY CHALLENGE
			byte[] proxychallenge = this.encodeBase64(this.generateSecureChallenge());
			((LoginResponse) message).setProxychallenge(encrypt.doFinal(proxychallenge));
			
			//BASE 64 ENCODE & RSA PROXY SECRET KEY
			byte[] secretkey = this.encodeBase64(this.generateSecretKey());
			((LoginResponse) message).setSecretkey(encrypt.doFinal(secretkey));
			
			//BASE 64 ENCODE & RSA PROXY IV PARAMETER
			byte[] ivparam = this.encodeBase64(this.generateSecureIV());
			((LoginResponse) message).setIvparameter(encrypt.doFinal(ivparam));
			
			//toArray
			byte[] array = this.toByteArray(message);
			System.out.println("Länge" + array.length);
			//ENCODE BASE 64 & SEND
			this.tcpChannel.sendByteArray(array);
		}*/
		/*
		if(message instanceof LoginResponse){
			
			//GET CHALLENGE AND BASE 64 ENCODE
			byte[] challenge=this.encodeBase64(((LoginResponse) message).getMessage());
			//GET PROXY CHALLENGE AND ENCODE BASE 64
			byte[] proxychallenge = this.encodeBase64(this.generateSecureChallenge());
			//GET PROXY SECRET 
			byte[] secretkey = this.encodeBase64(this.generateSecretKey());
			//GET PROXY IV
			byte[] ivparam = this.encodeBase64(this.generateSecureIV());
			
			String m = new String(challenge)+ " " +new String(proxychallenge)+" "+new String(secretkey)+" "+new String(ivparam);
			
			byte[]brsa = m.getBytes();
			System.out.println("before rsa :"+brsa.length);
			//RSA & init Chipher
			this.initencryptChipher();
			((LoginResponse) message).setMessage(encrypt.doFinal(brsa));
			System.out.println("Message: "+((LoginResponse)message).toString());
			//toArray
			byte[] array = this.toByteArray(message);
			System.out.println("Länge Message" + array.length);
			
			//ENCODE BASE 64 & SEND
			//this.tcpChannel.sendByteArray(array);//TODO
			this.sendByteArray(array);
		}*/
		if(message instanceof LoginResponse){
			//GET CHALLENGE AND BASE 64 ENCODE
			byte[] challenge=this.encodeBase64(((LoginResponse) message).getClientchallenge());
			//byte[] challenge=((LoginResponse) message).getClientchallenge();
			//GET PROXY CHALLENGE AND ENCODE BASE 64
			byte[] proxychallenge = this.encodeBase64(this.generateSecureChallenge());
			//GET PROXY SECRET 
			byte[] secretkey = this.encodeBase64(this.generateSecretKey());
			//GET PROXY IV
			byte[] ivparam = this.encodeBase64(this.generateSecureIV());
			
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
	
	public Request receiveMessageRequest() throws ClassNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
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
		encrypt.init(Cipher.ENCRYPT_MODE,this.publickey);
	}
	public void initdecryptChipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		this.decrypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		//System.out.println(publickey);
		decrypt.init(Cipher.DECRYPT_MODE,this.publickey);
	}
	//TODO receive Message
	
}
