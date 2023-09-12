package Servers;

import Assets.StringAssets;
import Clients.Clients;
import LogWriter.Logger;
import Models.MovieModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerInst {
    private String serverId;
    private String serverName;
    private int serverRegPort;
    private int serverUDPPort;

    public ServerInst(String serverId) throws Exception {
        this.serverId = serverId;
        if (serverId.equals("ATW")) {
            serverName = StringAssets.ATWATER_SERVER;
            serverRegPort = Clients.SERVER_ATWATER;
            serverUDPPort = MovieManager.ATWATER_SERVER_PORT;
        } else if (serverId.equals("VER")) {
            serverName = StringAssets.VERDUN_SERVER;
            serverRegPort = Clients.SERVER_VERDUN;
            serverUDPPort = MovieManager.VERDUN_SERVER_PORT;
        } else if (serverId.equals("OUT")) {
            serverName = StringAssets.OUTREMONT_SERVER;
            serverRegPort = Clients.SERVER_OUTREMONT;
            serverUDPPort = MovieManager.OUTREMONT_SERVER_PORT;
        }
        MovieManager rObj = new MovieManager(serverId, serverName);
        Registry registry = LocateRegistry.createRegistry(serverRegPort);
        registry.bind(Clients.MOVIE_MANAGEMENT_REGISTERED_NAME, rObj);
        System.out.println(serverName + " Server is Up and Running");
        Logger.serverLog(serverId, " Server is Up and Running");

        Runnable task = () -> {
            listenForRequest(rObj, serverUDPPort, serverName, serverId);
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static void listenForRequest(MovieManager obj, int serverUdpPort, String serverName, String serverId) {
        DatagramSocket aSocket = null;
        String sendingResult = "";
        try {
            aSocket = new DatagramSocket(serverUdpPort);
            byte[] buffer = new byte[1000];
            System.out.println(serverName + " UDP Server Started at port " + aSocket.getLocalPort() + " ............");
            Logger.serverLog(serverId, " UDP Server Started at port " + aSocket.getLocalPort());
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                String sentence = new String(request.getData(), 0,
                        request.getLength());
                String[] parts = sentence.split(";");
                String method = parts[0];
                String customerID = parts[1];
                String movieName = parts[2];
                String movieId = parts[3];
                Integer qTickets = Integer.valueOf(parts[4]);
                //int noOfTickets = Integer.valueOf(parts[4]);
                if (method.equalsIgnoreCase("removeMovie")) {
                    Logger.serverLog(serverId, customerID, " UDP request received " + method + " ", " movieName: " + movieName + " movieId: " + movieId + " number of tickets: "+ qTickets+  " ", " ...");
                    String result = obj.removeMovieUDP(movieId, movieName, customerID);
                    sendingResult = result + ";";}
                else if (method.equalsIgnoreCase("listMovieAvailability")) {
                    Logger.serverLog(serverId, customerID, " UDP request received " + method + " ", " movieName: " + movieName + " movieId: " + movieId +" number of tickets: "+ qTickets+  " ", " ...");
                    String result = obj.listMovieAvailabilityUDP(movieName);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("bookMovie")) {
                    Logger.serverLog(serverId, customerID, " UDP request received " + method + " ", " movieName: " + movieName + " movieId: " + movieId + " number of tickets: "+ qTickets+ " ", " ...");
                    String result = obj.bookMoviesTickets(customerID, movieId, movieName,qTickets);
                    sendingResult = result + ";";
                } else if (method.equalsIgnoreCase("cancelMovie")) {
                    Logger.serverLog(serverId, customerID, " UDP request received " + method + " ", " movieId: " + movieId + " movieName: " + movieName +" number of tickets: "+ qTickets+  " ", " ...");
                    String result = obj.cancelMovieTickets(customerID, movieId, movieName,qTickets);
                    sendingResult = result + ";";
                }
                byte[] sendData = sendingResult.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendingResult.length(), request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
                Logger.serverLog(serverId, customerID, " UDP reply sent " + method + " ", " movieId: " + movieId + " movieName: " + movieName + " ", sendingResult);
            }


        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
