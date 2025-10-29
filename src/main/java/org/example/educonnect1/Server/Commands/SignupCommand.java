package org.example.educonnect1.Server.Commands;

import org.example.educonnect1.Server.Command;
import org.example.educonnect1.Server.dao.UserDAO;
import org.example.educonnect1.Server.utils.EmailUtil;
import org.example.educonnect1.client.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.Random;

public class SignupCommand implements Command {
    private UserDAO userDAO;
    public SignupCommand(UserDAO userDAO) { this.userDAO = userDAO; }

    @Override
    public void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String name = (String) in.readObject();
        String email = (String) in.readObject();
        String password = (String) in.readObject();
        User existingUser = userDAO.findByEmail(email);
        String code = generateVerificationCode(6);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        if(existingUser != null && existingUser.isVerified()) {
            out.writeObject("EMAIL_EXISTS");
            return;
        } else if(existingUser != null && !existingUser.isVerified()) {
            existingUser.setVerificationCode(code);
            existingUser.setVerificationExpiry(expiry);
            userDAO.updateVerification(existingUser);
            out.writeObject("RESENT_CODE");
        } else {
            User user = new User();
            user.setFullName(name);
            user.setEmail(email);
            user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));
            user.setVerified(false);
            user.setVerificationCode(code);
            user.setVerificationExpiry(expiry);

            if(userDAO.saveUser(user)) {
                out.writeObject("SUCCESS");
            } else {
                out.writeObject("FAILED");
                return;
            }
        }

        EmailUtil.sendEmail(email, "EduConnect - Verify your account",
                "Your verification code is: " + code);
    }

    private String generateVerificationCode(int digits) {
        Random rnd = new Random();
        int max = (int)Math.pow(10, digits)-1;
        int code = rnd.nextInt(max+1);
        return String.format("%0" + digits + "d", code);
    }
}
