package secure;

import java.io.IOException;
import java.net.Socket;
import java.security.Key;

import javax.crypto.Cipher;

public class AESChannel extends Base64Channel{

	private byte[] ivparam;
	private Key secretkey;
	private Cipher encrypt;
	private Cipher decrypt;
	
	public AESChannel(IChannel base64Channel) {
		super(base64Channel);
	}
	public void setIvparam(byte[] ivparam) {
		this.ivparam = ivparam;
	}
	public void setSecretkey(Key secretkey) {
		this.secretkey = secretkey;
	}
	public void initencryptChipher(){
		//"AES/CTR/NoPadding"
		
		this.encrypt = Cipher.getInstance("AES/CTR/NoPadding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		encrypt.init(Cipher.ENCRYPT_MODE,this.secretkey,ivparam);
	}
	public void initdecryptChipher(){
		this.decrypt = Cipher.getInstance("AES/CTR/NoPadding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		//System.out.println(publickey);
		decrypt.init(Cipher.DECRYPT_MODE,this.secretkey,ivparam);
	}

	
//TODO
	
}
