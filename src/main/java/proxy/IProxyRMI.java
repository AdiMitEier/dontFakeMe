package proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

import client.IClientRMI;
import message.Response;
import message.response.MessageResponse;
import message.response.ProxyPublicKeyResponse;
import message.response.TopThreeDownloadsResponse;

public interface IProxyRMI extends Remote {
	MessageResponse readQuorum() throws RemoteException;
	MessageResponse writeQuorum() throws RemoteException;
	TopThreeDownloadsResponse topThreeDownloads() throws RemoteException;
	MessageResponse subscribe(IClientRMI client, String userName, String fileName, int number) throws RemoteException;
	ProxyPublicKeyResponse getProxyPublicKey() throws RemoteException;
	MessageResponse setUserPublicKey(String userName, PublicKey key) throws RemoteException;
}
