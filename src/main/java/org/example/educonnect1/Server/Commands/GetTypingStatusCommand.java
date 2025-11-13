package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Command to get typing status for a conversation
 * Receives: conversationId
 * Returns list of users currently typing (updated within last 5 seconds)
 */
public class GetTypingStatusCommand implements Command {
    private MessageDAO messageDAO;

    public GetTypingStatusCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int conversationId = (int) in.readObject();
        
        // Get list of users currently typing
        List<Map<String, Object>> typingUsers = messageDAO.getTypingUsers(conversationId);
        
        out.writeObject("SUCCESS");
        out.writeObject(typingUsers);
    }
}
