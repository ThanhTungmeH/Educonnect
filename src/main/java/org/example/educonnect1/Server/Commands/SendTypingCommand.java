package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Command to handle typing status updates
 * Receives: conversationId, userId, isTyping
 * Updates typing_status table and can broadcast to other users
 */
public class SendTypingCommand implements Command {
    private MessageDAO messageDAO;

    public SendTypingCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int conversationId = (int) in.readObject();
        int userId = (int) in.readObject();
        boolean isTyping = (boolean) in.readObject();
        
        // Update typing status in database
        boolean success = messageDAO.updateTypingStatus(conversationId, userId, isTyping);
        
        if (success) {
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("FAILED");
        }
    }
}
