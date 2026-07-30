package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public final class Log {

    private static final LogMode LOG_MODE = LogMode.FILE;

    private static String LOG_FILE;
    private static Handler SHARED_HANDLER;

    private static int siteId;

    static {
        try {
            Files.createDirectories(Path.of("logs"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory.", e);
        }
    }

    private Log() {
    }

    public static synchronized void initialize(int siteId) {
        Log.siteId = siteId;

        if (SHARED_HANDLER != null)
            throw new IllegalStateException("Logger already initialized.");

        try {
            LOG_FILE = "logs/site-" + siteId + "-" +
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + ".log";

            SHARED_HANDLER = createHandler();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize logger.", e);
        }
    }

    public static Logger getLogger(String name) {
        if (SHARED_HANDLER == null)
            throw new IllegalStateException("Call Log.initialize() before getLogger().");

        final Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        boolean alreadyAdded = false;
        for (Handler handler : logger.getHandlers()) {
            if (handler == SHARED_HANDLER) {
                alreadyAdded = true;
                break;
            }
        }

        if (!alreadyAdded)
            logger.addHandler(SHARED_HANDLER);

        return logger;
    }

    private static Handler createHandler() throws IOException {
        final Handler handler = switch (LOG_MODE) {
            case FILE -> new FileHandler(LOG_FILE, true) {
                @Override
                public synchronized void publish(LogRecord record) {
                    super.publish(record);
                    flush();
                }
            };
            case STDOUT -> new ConsoleHandler();
        };

        handler.setLevel(Level.ALL);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format(
                        "Site %-2d [%-20s]  %-7s  %s%n",
                        siteId,
                        record.getLoggerName(),
                        record.getLevel().getName(),
                        record.getMessage()
                );
            }
        });

        return handler;
    }

    private enum LogMode {FILE, STDOUT}
}
