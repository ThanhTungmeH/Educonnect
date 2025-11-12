package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.MessageDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class GetMessagesCommand implements Command {
    private MessageDAO messageDAO;

    public GetMessagesCommand(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int conversationId = (int) in.readObject();
        int limit = (int) in.readObject();
        
        List<Map<String, Object>> messages = messageDAO.getMessages(conversationId, limit);
        
        out.writeObject("SUCCESS");
        out.writeObject(messages);
    }
}
