package main;




import constant.ServerConstant;
import dao.impl.ClientDaoImpl;
import dao.impl.SessionDaoImpl;
import model.*;
import model.message.Response;
import service.ClientService;
import service.SessionService;
import util.LogInParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class Server {
    Client currentClient;
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(ServerConstant.PORT)) {
            while (true) {
                SocketConnector socketConnector = new SocketConnector(serverSocket);
                new Thread(()->{
                    String request = socketConnector.readLine();
                    Response response;
                    response = findClient(request);
                    socketConnector.writeLine(response.toString());
                    response = createSession("", currentClient);
                    socketConnector.writeLine(response.toString());
                }).start();
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static Response findClient(String request) {
        Client currentClient;
        ClientService clientService = new ClientService();
        ClientDaoImpl clientDaoImpl = new ClientDaoImpl();
        int clientCounter = clientDaoImpl.getLastClient();
        Client.setCounter(clientCounter);
        int token = (1 + (int) (Math.random() * 100000));

        for (Client c: clientService.getAllClients()) {
            if (request.contains(c.getLogin()) && request.contains(c.getPassword() + "")) {
                currentClient = c;
                currentClient.setClientToken(token);
                clientService.updateClient(currentClient);
                return new Response(0,"authorization and token set is successful");
            }
        }
        currentClient = new Client(token, LogInParser.parseLogin(request), LogInParser.parsePassword(request), null);
        clientService.createClient(currentClient);

        return new Response(0,"connection successful");
    }

    public static Response createSession(String host, Client client) {
        int token = (1 + (int) (Math.random() * 100000));
        SessionService sessionService = new SessionService();
        SessionDaoImpl sessionDaoImpl = new SessionDaoImpl();
        int sessionCounter = sessionDaoImpl.getLastSessionId();
        Session.setCounter(sessionCounter);

        List<Client> clients = new ArrayList<>();
        clients.add(client);

        Session session = new Session(new Date(), ServerConstant.PORT, true, ServerConstant.IP,
                host, token, clients);
        sessionService.createSession(session);
        return new Response(0,"session established");
    }

}
