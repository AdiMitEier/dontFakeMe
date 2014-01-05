package secure;

import java.io.IOException;
import java.net.Socket;

public class RSAChannel extends Base64Channel{

	public RSAChannel(IChannel base64Channel) {
		super(base64Channel);
		// TODO Auto-generated constructor stub
	}
	public RSAChannel(Socket s) throws IOException {
		super(s);
		// TODO Auto-generated constructor stub
	}
	
}
