package util;

import java.util.logging.*;

public final class Log {

    private Log() {}

    public static Logger getLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);

        if (logger.getHandlers().length == 0) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.ALL);

            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format(
                            "%-25s %-7s %s%n",
                            Thread.currentThread().getName(),
                            record.getLevel().getName(),
                            record.getMessage()
                    );
                }
            });

            logger.addHandler(handler);
            logger.setLevel(Level.ALL);
        }

        return logger;
    }
}