package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.UserDAO;
import org.example.educonnect1.client.models.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class SearchFriendCommand implements Command {
    private UserDAO userDAO;
    public SearchFriendCommand(UserDAO userDAO) { this.userDAO = userDAO; }
    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String name=(String) in.readObject();
        List<User> user=userDAO.findByName(name);
        out.writeObject(user);
        // Implementation for searching friends goes here
    }

}
