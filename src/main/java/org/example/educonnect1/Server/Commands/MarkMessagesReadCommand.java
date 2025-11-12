package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MarkMessagesReadCommand implements Command {
    private MessageDAO messageDAO;

    public MarkMessagesReadCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int conversationId = (int) in.readObject();
        int userId = (int) in.readObject();
        
        boolean success = messageDAO.markMessagesAsRead(conversationId, userId);
        
        if (success) {
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("FAILED");
        }
    }
}
