package org.example.educonnect1.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Command {
    void execute(ObjectInputStream in, ObjectOutputStream out) throws Exception;
}