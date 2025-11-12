package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.FriendDAO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class GetFriendRequestsCommand implements Command {
    private FriendDAO friendDAO;

    public GetFriendRequestsCommand(FriendDAO friendDAO) {
        this.friendDAO = friendDAO;
    }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        int userId = (int) in.readObject();
        
        List<Map<String, Object>> requests = friendDAO.getPendingRequests(userId);
        
        out.writeObject("SUCCESS");
        out.writeObject(requests);
    }
}
