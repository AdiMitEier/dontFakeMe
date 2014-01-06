package message.response;

import java.security.PublicKey;

import message.Response;

public class ProxyPublicKeyResponse implements Response {
	private static final long serialVersionUID = 4550680230065708876L;

	private PublicKey key;
	private String message;

	public ProxyPublicKeyResponse(PublicKey key) {
		this.key = key;
	}
	
	public ProxyPublicKeyResponse(String message) {
		this.message = message;
	}

	public PublicKey getKey() {
		return key;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean hasKey() {
		return key!=null;
	}

	@Override
	public String toString() {
		return this.key!=null ? getKey().toString() : getMessage();
	}
}
