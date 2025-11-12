package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SendMessageCommand implements Command {
    private MessageDAO messageDAO;

    public SendMessageCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int senderId = (int) in.readObject();
        int receiverId = (int) in.readObject();
        String content = (String) in.readObject();
        
        // Get or create conversation
        int conversationId = messageDAO.getOrCreateConversation(senderId, receiverId);
        
        // Save message
        boolean success = messageDAO.saveMessage(conversationId, senderId, content);
        
        if (success) {
            out.writeObject("SUCCESS");
            out.writeObject(conversationId);
        } else {
            out.writeObject("FAILED");
        }
    }
}
