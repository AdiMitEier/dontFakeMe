package secure;

import java.io.IOException;
import java.net.Socket;
import javax.crypto.Cipher;

public class AESChannel extends Base64Channel{

	private byte[] ivparam;
	private byte[] secretkey;
	
	public AESChannel(IChannel base64Channel) {
		super(base64Channel);
	}
	public void setIvparam(byte[] ivparam) {
		this.ivparam = ivparam;
	}
	public void setSecretkey(byte[] secretkey) {
		this.secretkey = secretkey;
	}

	
//TODO
	
}
