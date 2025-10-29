package org.example.educonnect1.Server.Commands;


import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.UserDAO;
import org.example.educonnect1.client.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class LoginCommand implements Command {
    private UserDAO userDAO;
    public LoginCommand(UserDAO userDAO) { this.userDAO = userDAO; }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String email = (String) in.readObject();
        String password = (String) in.readObject();
        User user = userDAO.findByEmail(email);
        if(user != null && BCrypt.checkpw(password, user.getPassWord()) && user.isVerified()) {
            out.writeObject("SUCCESS");
            out.writeObject(user);
        } else if(user != null && !user.isVerified()) {
            out.writeObject("NOT_VERIFIED");
        } else {
            out.writeObject("FAILED");
        }
    }
}