package message.request;

import javax.crypto.Mac;


import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;


import org.bouncycastle.util.encoders.Base64;

import message.Request;

/**
 * Requests a {@link model.DownloadTicket} in order to download a file from a file server.
 * <p/>
 * <b>Request (client to proxy)</b>:<br/>
 * {@code !download &lt;filename&gt;}<br/>
 * <b>Response (proxy to client):</b><br/>
 * {@code !download &lt;ticket&gt;}<br/>
 *
 * @see message.response.DownloadTicketResponse
 */
public class DownloadTicketRequest implements Request {
	private static final long serialVersionUID = 1183675324570817315L;

	private final String file;
	private byte[] base64hash;//STAGE3

	public DownloadTicketRequest(String file) {
		this.file = file;
	}

	public String getFilename() {
		return file;
	}
	
	//STAGE3
	public void setHmac(Key key){
		Mac hmac = null;
		try {
			hmac = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			// TODO vernuenftiges handling
			e.printStackTrace();
		}
		try {
			hmac.init(key);
		} catch (InvalidKeyException e) {
			// TODO vernuenftiges handling
			e.printStackTrace();
		}
		byte[] message = this.toString().getBytes();
		hmac.update(message);
		byte[] hash = hmac.doFinal();
		base64hash = Base64.encode(hash);
	}
	
	public byte[] getHmac(){
		return base64hash;
	}

	@Override
	public String toString() {
		return "!download " + getFilename();
	}
}
