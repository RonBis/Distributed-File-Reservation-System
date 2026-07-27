package comm.message;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract sealed class AbstractMessage implements Serializable permits LocalMessage, ResourceMessage {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int sender, recipient;

    public int getSender() {
        return sender;
    }

    public int getRecipient() {
        return recipient;
    }

    protected AbstractMessage(int sender, int recipient) {
        this.sender = sender;
        this.recipient = recipient;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append('<')
                .append(getClass().getSimpleName())
                .append(", sender: ").append(sender)
                .append(", recipient: ").append(recipient);

        for (Field field : getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;

            field.setAccessible(true);

            try {
                sb.append(", ")
                        .append(field.getName())
                        .append(": ")
                        .append(field.get(this));
            } catch (IllegalAccessException ignored) {
            }
        }

        sb.append('>');
        return sb.toString();
    }
}
