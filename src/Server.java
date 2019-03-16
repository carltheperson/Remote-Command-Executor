import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {

	private int port = 5566;

	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ServerSocket server;
	private Socket connection;

	public static void main(String[] args) throws IOException {
		System.out.println("Remote Command Executor - server\n");
		Server mainServer = new Server();
		while (true) {
			try {
				mainServer.start();
			} catch (Exception e) {
				mainServer.closeConnection();
				System.out.println("Unprevantlbe error. Restarting\n");
			}
		}
	}

	private void start() {
		try {
			server = new ServerSocket(port);
			connect();
			setupStreams();
			System.out.println("connected to: " + connection.getLocalAddress());
		} catch (IOException e) {
			System.out.println("Error connecting");
		}

		while (true) {
			sendOutput(executeCommand(getInput()));
		}
	}

	private void connect() {
		try {
			connection = server.accept();
		} catch (IOException e) {
			System.out.println("Error connecting to client");
		}
	}

	private void setupStreams() {
		try {
			output = new ObjectOutputStream(connection.getOutputStream());
			output.flush();
			input = new ObjectInputStream(connection.getInputStream());
		} catch (IOException e) {
			System.out.println("Error setting up streams");
		}
	}

	private String getInput() {
		try {
			String message = (String) input.readObject();
			System.out.println("\nClient: " + message);
			return message;

		} catch (ClassNotFoundException | IOException e) {
			System.out.println("\nClient disconnected");
			closeConnection();
			start();
			return "";
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

	private void closeConnection() {
		try {
			input.close();
			connection.close();
			server.close();
		} catch (IOException e) {
			System.out.println("Error closing connection");
		}
	}

	private String executeCommand(String command) {
		try {

			// If the client is requesting to get a file
			if (command.startsWith("get-file")) {
				command = command.split(" ")[1];
				System.out.println("Client is requesting file: " + command);

				// Checks if file exists
				File file = new File(command);
				if (!file.exists()) {
					System.out.println("Error file not found");
					sendOutput("FILE-NOT-FOUND");
					return "Error file not found";
				} else {
					sendOutput("FILE-FOUND");
				}

				sendFile(command);

				// Reestablish normal connection
				connect();
				setupStreams();

				return null;

			}
			// If the client is requesting to send a file
			else if (command.startsWith("send-file")) {
				command = command.split(" ")[1];
				System.out.println("Client is sending file: " + command);

				reciveFile(command);

				// Reestablish normal connection
				connect();
				setupStreams();

				sendOutput("FILE-RECIVED");

				return null;

			} else if (command.equals("FILE-RECIVED")) {
				System.out.println("Client recived file");
				return "";
			} else if (command.equals("FILE-NOT-FOUND")) {
				return "";
			}
			// Executes the linux shell command
			else {
				Process p = Runtime.getRuntime().exec(command);
				p.waitFor();
				BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()));

				String line = "";
				String output = "";
				while ((line = buffer.readLine()) != null) {
					output += line + "\n";
				}

				output = output.trim();

				if (output != "") {
					System.out.println(output);
				}
				if (output.contains("\n")) {
					output = "\n" + output;
				}

				return output;
			}

		} catch (IOException | InterruptedException e) {
			System.out.println("Execute error");
			return "Execute error";
		}
	}

	// -------- sending and receiving files ----------//

	// Sends file to client
	private void sendFile(String file) {

		try {
			InputStream fileIn = new FileInputStream(file);
			OutputStream fileOut = (OutputStream) connection.getOutputStream();

			writeOut(fileIn, fileOut);

			try {
				fileOut.close();
				fileIn.close();

			} catch (IOException e) {
				System.out.println("Error closing file streams");
			}
		} catch (IOException e) {
			System.out.println("Error reciving file");
		}

	}

	// Recives file from client
	private void reciveFile(String file) {

		try {
			OutputStream fileOut = new FileOutputStream(file);
			InputStream fileIn = connection.getInputStream();

			try {

				writeOut(fileIn, fileOut);
				fileOut.close();
				fileIn.close();
				System.out.println("File recived");

			} catch (IOException e) {
				System.out.println("Error closing file streams");
			}

		} catch (IOException e) {
			System.out.println("Error reciving file");
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
