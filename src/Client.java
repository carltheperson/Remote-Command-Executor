import java.util.Scanner;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

	private int port = 5566;
	private String ip = "localhost";

	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Socket connection;

	private Scanner in = new Scanner(System.in);

	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("Remote Command Executor - client\n");
		Client mainClient = new Client();
		mainClient.start();
	}

	private void start() throws UnknownHostException, IOException {

		connectToServer();
		setupStreams();
		System.out.println("connected to: " + connection.getLocalAddress());

		while (true) {
			sendCommand(getUserInput());
			System.out.println("Server: " + getInput() + "\n");
		}
	}

	private void connectToServer() {
		try {
			connection = new Socket(ip, port);
		} catch (IOException ioException) {
			System.out.println("Error couldn't connect");
			System.exit(0);
		}
	}

	private void setupStreams() {
		try {
			output = new ObjectOutputStream(connection.getOutputStream());
			output.flush();
			input = new ObjectInputStream(connection.getInputStream());
		} catch (IOException ioException) {
			System.out.println("Error setting up streams");
		}
	}

	private void sendOutput(String theOutput) {
		if (theOutput != null) {
			try {
				output.writeObject(theOutput);
				output.flush();
			} catch (IOException e) {
				System.out.println("Error sending output");
			}
		}
	}

	private String getInput() {
		try {
			String message = (String) input.readObject();
			return message;
		} catch (EOFException e) {
			System.out.println("Server disconnected");
			System.exit(0);
			return "";
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("Error getting server input");
			return "";
		}
	}

	public String getUserInput() {

		String userInput = "";
		while (userInput.equals("")) {
			System.out.print(":");
			userInput = in.nextLine();
		}

		return userInput;
	}

	private void sendCommand(String command) {
		// If the user is requesting to get a file
		if (command.startsWith("get-file")) {
			command = command.split(" ")[1];
			System.out.println("Requesting file from sever: " + command);
			
			// If file exists
			if (reciveFile(command)) {
				// Reestablish normal connection
				connectToServer();
				setupStreams();
				
				sendOutput("FILE-RECIVED");
			}
			
			return;
			
		}
		// If the user is requesting to send a file
		else if (command.startsWith("send-file")) {
			command = command.split(" ")[1];
			System.out.println("Sending file to server: " + command);
			
			// Checks if file exists
			File file = new File(command);
			if (!file.exists()) {
				sendOutput("FILE-NOT-FOUND");
				System.out.println("Error file not found");
				return;
			} else {
				sendOutput("FILE-FOUND");
			}
			
			sendFile(command);
			
			connectToServer();
			setupStreams();
			
			return;


		}
		
		sendOutput(command);
	}

	// -------- sending and receiving files ----------//

	// Receives file from server
	// Returns false if the file doesn't exist
	private boolean reciveFile(String file) {

		try {

			sendOutput("get-file " + file);
				
			// Checks if server found the file
			if (!getInput().equals("FILE-FOUND")) {
				return false;
			}

			OutputStream fileOut = new FileOutputStream(file);
			InputStream fileIn = connection.getInputStream();

			writeOut(fileIn, fileOut);

			try {
				fileOut.close();
				fileIn.close();
				System.out.println("File recived");
			} catch (IOException e) {
				System.out.println("Error closing file streams");
			}

		} catch (IOException e) {
			System.out.println("Error reciving file");
		}

		return true;
	}

	// Sends file to server
	private void sendFile(String file) {

		try {
			InputStream fileIn = new FileInputStream(file);
			OutputStream fileOut = connection.getOutputStream();

			sendOutput("send-file " + file);

			try {
				writeOut(fileIn, fileOut);
				fileIn.close();
				fileOut.close();

			} catch (IOException e) {
				System.out.println("Error closing file streams");
			}

		} catch (IOException e) {
			System.out.println("Error sending file");
		}
	}

	// Writes out file
	public void writeOut(InputStream in, OutputStream out) {
		try {
			byte[] buf = new byte[10000];
			int len = 0;
			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
		} catch (IOException e) {
			System.out.println("Error writing out file");
		}
	}
}
