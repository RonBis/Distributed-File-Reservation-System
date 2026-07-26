package application;

import comm.Transport;
import comm.message.LocalMessage;

import java.util.Scanner;

public final class ConsoleController implements Runnable {

    private final int siteId;
    private final Transport transport;

    public ConsoleController(int siteId, Transport transport) {
        this.siteId = siteId;
        this.transport = transport;

        new Thread(this, "Site " + siteId + " [Console]").start();
    }

    @Override
    public void run() {
        final Scanner scanner = new Scanner(System.in);

        printHelp();

        while (true) {
            System.out.print("> ");

            final String line = scanner.nextLine().trim();
            if (line.isEmpty())
                continue;

            final String[] args = line.split("\\s+");

            try {
                switch (args[0].toLowerCase()) {
                    case "lock" -> {
                        requireArgs(args, 2);
                        transport.postLocalMessage(
                                new LocalMessage.ReqResourceLockMsg(
                                        siteId,
                                        Integer.parseInt(args[1])
                                )
                        );
                    }

                    case "release" -> {
                        requireArgs(args, 2);
                        transport.postLocalMessage(
                                new LocalMessage.ReqReleaseResourceMsg(
                                        siteId,
                                        Integer.parseInt(args[1])
                                )
                        );
                    }

                    case "status" -> transport.postLocalMessage(
                            new LocalMessage.PrintStatusMsg(siteId)
                    );

                    case "exit", "quit" -> transport.postLocalMessage(
                            new LocalMessage.ExitMsg(siteId)
                    );

                    case "help" -> printHelp();

                    default -> System.out.println("Unknown command.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid resource id.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void requireArgs(String[] args, int expected) {
        if (args.length < expected)
            throw new IllegalArgumentException("Too few arguments.");
    }

    private static void printHelp() {
        System.out.println("""
                Commands:
                  lock <resource-id>
                  release <resource-id>
                  status
                  help
                  exit
                """);
    }
}