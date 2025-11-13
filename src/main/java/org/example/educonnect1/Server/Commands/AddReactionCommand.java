package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Command to add emoji reactions to messages
 * Receives: messageId, userId, reaction
 * Saves to message_reactions table and returns updated reaction counts
 */
public class AddReactionCommand implements Command {
    private MessageDAO messageDAO;

    public AddReactionCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int messageId = (int) in.readObject();
        int userId = (int) in.readObject();
        String reaction = (String) in.readObject();
        
        // Save the reaction
        boolean success = messageDAO.saveReaction(messageId, userId, reaction);
        
        if (success) {
            // Get updated reaction counts
            Map<String, Integer> reactions = messageDAO.getReactions(messageId);
            
            out.writeObject("SUCCESS");
            out.writeObject(reactions);
        } else {
            out.writeObject("FAILED");
        }
    }
}
