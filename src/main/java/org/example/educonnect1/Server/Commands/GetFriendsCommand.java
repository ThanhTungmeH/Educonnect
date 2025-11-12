package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.FriendDAO;
import org.example.educonnect1.client.models.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class GetFriendsCommand implements Command {
    private FriendDAO friendDAO;

    public GetFriendsCommand(FriendDAO friendDAO) {
        this.friendDAO = friendDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int userId = (int) in.readObject();
        
        List<User> friends = friendDAO.getFriends(userId);
        
        out.writeObject("SUCCESS");
        out.writeObject(friends);
    }
}
