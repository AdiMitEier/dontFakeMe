package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import proxy.IProxyCli;
import server.IFileServerCli;
import util.ComponentFactory;
import util.Config;
import util.Util;
import cli.Shell;
import cli.TestInputStream;
import cli.TestOutputStream;
import client.IClientCli;

public class TestingFramework {
	static ComponentFactory componentFactory = new ComponentFactory();
	IProxyCli proxy;
	IFileServerCli server;
	IClientCli subscribeClient;
	Config config;
	int clients;
	int uploadsPerMin;
	int downloadsPerMin;
	int fileSizeKB;
	double overwriteRatio;
	int testDurationSec;
	Timer testTimer;
	Timer downloadTimer;
	AtomicInteger downloads = new AtomicInteger(0);
	AtomicInteger uploads = new AtomicInteger(0);
	List<IClientCli> downloadClients;
	List<IClientCli> uploadClients;

	public static void main(String[] args) {
		try {
			new TestingFramework();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TestingFramework() throws Exception {
		readConfig();
		System.out.println("-----------------------------------------------------");
		System.out.println("Generating test file");
		System.out.println("-----------------------------------------------------");
		generateTestFile();
		System.out.println("Component startup");
		System.out.println("-----------------------------------------------------");
		proxy = componentFactory.startProxy(new Config("proxy"), new Shell(
				"proxy", new TestOutputStream(System.out),
				new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);

		server = componentFactory.startFileServer(new Config("fs1"), new Shell(
				"fs1", new TestOutputStream(System.out),
				new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);

		subscribeClient = componentFactory.startClient(new Config("client"), new Shell(
				"client", new TestOutputStream(System.out),
				new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);

		downloadClients = new ArrayList<IClientCli>();
		for(int i=0; i<clients; i++) {
			downloadClients.add(componentFactory.startClient(new Config("client"), new Shell(
				"client", new TestOutputStream(System.out),
				new TestInputStream())));
		}
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP*3);
		System.out.println("-----------------------------------------------------");
		System.out.println("Starting test");
		System.out.println("-----------------------------------------------------");
		subscribeClient.login("alice", "12345");
		subscribeClient.upload("testFile.txt");
		subscribeClient.subscribe("testFile.txt", 100);
		
		for(IClientCli client : downloadClients) {
			client.login("bill", "23456");
			client.buy(10000000);
		}
		
		testTimer = new Timer();
		testTimer.schedule(new EndTestTask(), testDurationSec*1000, testDurationSec*1000);
		
		downloadTimer = new Timer();
		for(IClientCli client : downloadClients) {
			if(downloadsPerMin>0)
				downloadTimer.schedule(new DownloadTask(client,false), 0l, (long) 60000 / downloadsPerMin);
			if(uploadsPerMin>0)
				downloadTimer.schedule(new UploadTask(client,false), 0l, (long) 60000 / uploadsPerMin);
		}	
	}

	private void readConfig() {
		config = new Config("loadtest");
		clients = config.getInt("clients");
		uploadsPerMin = config.getInt("uploadsPerMin");
		downloadsPerMin = config.getInt("downloadsPerMin");
		fileSizeKB = config.getInt("fileSizeKB");
		overwriteRatio = Double.parseDouble(config.getString("overwriteRatio"));
		testDurationSec = config.getInt("testDurationSec");
		System.out.println("Clients: " + clients);
		System.out.println("UploadsPerMin: " + uploadsPerMin);
		System.out.println("DownloadsPerMin: " + downloadsPerMin);
		System.out.println("FileSizeKB: " + fileSizeKB);
		System.out.println("OverwriteRatio: " + overwriteRatio);
		System.out.println("testDurationSec: " + testDurationSec);
	}
	
	private void generateTestFile() throws IOException  {
		String newline = System.getProperty("line.separator");
		Writer output = null;
		File file = new File("files/client/testFile.txt");
		file.delete();
		output = new BufferedWriter(new FileWriter(file, true));
		output.write("");
		output.write(newline);

        long size = getFileSize("files/client/testFile.txt");
		while(size < fileSizeKB*1000){
			output.write("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			output.write(newline);
            output.flush();
            size = getFileSize("files/client/testFile.txt");
		}
		output.close();
	}
	
	private long getFileSize(String filename) {
		File file = new File(filename);        
		if (!file.exists() || !file.isFile()) {
			System.out.println("File does not exist");
			return -1;
		}
		return file.length();
	}

	private class DownloadTask extends TimerTask {

		IClientCli client;
		boolean output;

		DownloadTask(IClientCli client, boolean output) {
			this.client = client;
			this.output = output;
		}

		public void run() {
			try {
				String response = client.download("testFile.txt").toString();
				if(output) System.out.println(client.getLoggedInUserName() + ": "
						+ response);
				if(response.contains("!data")) downloads.incrementAndGet();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class UploadTask extends TimerTask {

		IClientCli client;
		boolean output;

		UploadTask(IClientCli client, boolean output) {
			this.client = client;
			this.output = output;
		}

		public void run() {
			try {
				String response = client.upload("testFile.txt").toString();
				if(output) System.out.println(client.getLoggedInUserName() + ": "
						+ response);
				if(response.contains("successfully")) uploads.incrementAndGet();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class EndTestTask extends TimerTask {
		public void run() {
			System.out.println("-----------------------------------------------------");
			System.out.println("Ending test");
			System.out.println("-----------------------------------------------------");
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			testTimer.cancel();
			downloadTimer.cancel();
			try {
				proxy.exit();
				server.exit();
				subscribeClient.exit();
				for(IClientCli client : downloadClients) {
					client.exit();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP/2);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("-----------------------------------------------------");
			System.out.println("SUCCESSFUL DOWNLOADS: " + downloads.get());
			System.out.println("SUCCESSFUL UPLOADS: " + uploads.get());
			System.out.println("-----------------------------------------------------");
		}
	}
}
