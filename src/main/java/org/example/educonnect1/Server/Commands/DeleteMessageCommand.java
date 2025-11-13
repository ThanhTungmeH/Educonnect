package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Command to delete a message (soft delete)
 * Receives: messageId, userId
 * Checks permission (only sender can delete)
 * Sets deleted = true and replaces content with '[Message deleted]'
 */
public class DeleteMessageCommand implements Command {
    private MessageDAO messageDAO;

    public DeleteMessageCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int messageId = (int) in.readObject();
        int userId = (int) in.readObject();
        
        // Attempt to delete the message (soft delete)
        // This will check if the user owns the message
        boolean success = messageDAO.deleteMessage(messageId, userId);
        
        if (success) {
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("FAILED");
            out.writeObject("Cannot delete: not your message");
        }
    }
}
