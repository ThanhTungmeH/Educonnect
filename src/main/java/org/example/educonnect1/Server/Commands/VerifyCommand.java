package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.UserDAO;
import org.example.educonnect1.client.models.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class VerifyCommand implements Command {
    private UserDAO userDAO;
    public VerifyCommand(UserDAO userDAO) { this.userDAO = userDAO; }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String email = (String) in.readObject();
        String code = (String) in.readObject();
        User user = userDAO.findByEmail(email);

        if(user == null) {
            out.writeObject("USER_NOT_FOUND");
            return;
        }

        if(code.equals(user.getVerificationCode())) {
            userDAO.markVerified(email);
            out.writeObject("SUCCESS");
        } else {
            out.writeObject("INVALID_CODE");
        }
    }
}
