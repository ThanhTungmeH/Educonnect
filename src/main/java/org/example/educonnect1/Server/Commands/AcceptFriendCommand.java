package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.FriendDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class AcceptFriendCommand implements Command {
    private FriendDAO friendDAO;

    public AcceptFriendCommand(FriendDAO friendDAO) {
        this.friendDAO = friendDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int requestId = (int) in.readObject();
        int userId = (int) in.readObject();
        
        boolean success = friendDAO.acceptFriendRequest(requestId, userId);
        
        if (success) {
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("FAILED");
        }
    }
}
