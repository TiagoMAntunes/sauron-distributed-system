package pt.tecnico.sauron.spotter;

import java.util.Scanner;

import pt.tecnico.sauron.silo.client.SiloServerFrontend;

public class SpotterApp {
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 2) {
			System.out.printf("Usage: java %s <address> <port>%n", SpotterApp.class.getName());
			System.exit(0);
		}

		final String host = args[0];
		final int port = Integer.valueOf(args[1]);

		try (SiloServerFrontend frontend = new SiloServerFrontend(host, port); Scanner sc = new Scanner(System.in)) {
			boolean end = false;
			while(!end) {

				System.out.print("$ ");

				String line[] = sc.nextLine().split(" ");

				switch(line[0]) {
					case "spot":
					case "trail":
					case "ping":
					case "clear":
					case "init":
					case "help":
						System.out.println("Available commands: ");
						System.out.println("spot <type> <identifier> - Find the last observation of an object or person with the specified ID");
						System.out.println("trail <type> <identifier> - Find the path followed by an object or person with the specified ID");
						System.out.println("ping <name> - Checks if the server is responding by saying hi");
						System.out.println("clear - Resets the server to default status");
						System.out.println("init - to be done"); //TODO
						System.out.println("help - Displays this menu");
						System.out.println("exit - Exits the program");
						break;
					case "exit":
						end = true;
						break;
					default:
						System.out.println("Invalid command. Write help to get the list of available commands");
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Closing...");
		}

	}

}
