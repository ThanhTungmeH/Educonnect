package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.FriendDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class AddFriendCommand implements Command {
    private FriendDAO friendDAO;

    public AddFriendCommand(FriendDAO friendDAO) {
        this.friendDAO = friendDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int senderId = (int) in.readObject();
        int receiverId = (int) in.readObject();
        
        boolean success = friendDAO.sendFriendRequest(senderId, receiverId);
        
        if (success) {
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("FAILED");
        }
    }
}
