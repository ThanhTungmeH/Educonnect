package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Command to edit a message
 * Receives: messageId, userId, newContent
 * Checks permission (only sender can edit) and time limit (15 minutes)
 * Updates messages table and sets edited = true
 */
public class EditMessageCommand implements Command {
    private MessageDAO messageDAO;

    public EditMessageCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int messageId = (int) in.readObject();
        int userId = (int) in.readObject();
        String newContent = (String) in.readObject();
        
        // Attempt to edit the message
        // This will check ownership and time limit
        boolean success = messageDAO.editMessage(messageId, userId, newContent);
        
        if (success) {
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("FAILED");
            out.writeObject("Cannot edit: either not your message or time limit exceeded (15 minutes)");
        }
    }
}
