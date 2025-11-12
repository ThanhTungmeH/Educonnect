package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class GetConversationsCommand implements Command {
    private MessageDAO messageDAO;

    public GetConversationsCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int userId = (int) in.readObject();
        
        List<Map<String, Object>> conversations = messageDAO.getUserConversations(userId);
        
        out.writeObject("SUCCESS");
        out.writeObject(conversations);
    }
}
