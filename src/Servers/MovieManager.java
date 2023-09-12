package Servers;

import Assets.StringAssets;
import Interface.MovieServer;
import LogWriter.Logger;
import Models.ClientsModel;
import Models.MovieModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MovieManager extends UnicastRemoteObject implements MovieServer {
    public static final int ATWATER_SERVER_PORT = 7878;
    public static final int VERDUN_SERVER_PORT = 8989;
    public static final int OUTREMONT_SERVER_PORT = 9090;
    public static final int MINVALUE = Integer.MIN_VALUE;
    private String serverId;
    private String serverName;
    private Map<String, Map<String, MovieModel>> moviesEvents;
    private Map<String, Map<String, List<String>>> clientMovies;
    private Map<String, ClientsModel> serverClients;
    private Map<String, Integer> movieBookings;


    protected MovieManager(String serverId, String serverName) throws RemoteException {
        super();
        this.serverId = serverId;
        this.serverName = serverName;
        moviesEvents = new ConcurrentHashMap<>();
        moviesEvents.put(StringAssets.AVATAR_MOVIE,new ConcurrentHashMap<>());
        moviesEvents.put(StringAssets.AVENGERS_MOVIE,new ConcurrentHashMap<>());
        moviesEvents.put(StringAssets.TITANIC_MOVIE,new ConcurrentHashMap<>());
        clientMovies = new ConcurrentHashMap<>();
        serverClients = new ConcurrentHashMap<>();
        movieBookings = new ConcurrentHashMap<>();
    }

    private static int getServerPort(String serverBranch){
        if(serverBranch.equalsIgnoreCase("ATW")){
            return ATWATER_SERVER_PORT;
        }
        else if(serverBranch.equalsIgnoreCase("VER")){
            return VERDUN_SERVER_PORT;
        } else if (serverBranch.equalsIgnoreCase("OUT")) {
            return OUTREMONT_SERVER_PORT;
        }
        return 1;
    }

    @Override
    public String addMovieSlots(String movieId, String movieName, int bookingCapacity) throws RemoteException {
        String response;
        Date date = new Date();
        String strDateFormat = "yyMMdd";
        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
        String dateToday = dateFormat.format(date);
        int today = Integer.parseInt(dateToday);
        String dateOfMovie = movieId.substring(8,10)+""+movieId.substring(6,8)+""+movieId.substring(4,6);
        int movieDate = Integer.parseInt(dateOfMovie);
        if(movieDate-today>7 || movieDate-today<0){
            response = "FAILURE: Movie Slot Can only be added for a week from current date";
            try {
                Logger.serverLog(movieId, "null", " RMI addMovie ", " movieId: " + movieId + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            }catch (IOException e){
                e.printStackTrace();
            }
            return response;
        }
        if (moviesEvents.get(movieName).containsKey(movieId)) {
            if (moviesEvents.get(movieName).get(movieId).getMovieCapacity() <= bookingCapacity) {
                moviesEvents.get(movieName).get(movieId).setMovieCapacity(bookingCapacity);
                response = "SUCCESS: Movie" + movieId + " New Capacity is " + bookingCapacity;
                try {
                    Logger.serverLog(movieId, "null", " RMI addMovie ", " movieId: " + movieId + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            else{
                response = "FAILURE: Movie Capacity already more than "+ bookingCapacity;
                try {
                    Logger.serverLog(movieId, "null", " RMI addMovie ", " movieId: " + movieId + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            return response;
        }
        if(MovieModel.findMovieServer(movieId).equals(serverName)){
            MovieModel movieModel = new MovieModel(movieName, movieId, bookingCapacity);
            Map<String, MovieModel> moviesHashMap = moviesEvents.get(movieName);
            moviesHashMap.put(movieId, movieModel);
            moviesEvents.put(movieName, moviesHashMap);
            response = "SUCCESS: Movie "+movieId+" added successfully.";
            try {
                Logger.serverLog(movieId, "null", " RMI addMovie ", " movieId: " + movieId + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            response = "FAILURE: Cannot add Movie to other Servers";
            try {
                Logger.serverLog(movieId, "null", " RMI addMovie ", " movieId: " + movieId + " movieName: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return response;
    }


    @Override
    public String removeMovieSlots(String movieId, String movieName) throws RemoteException {
        String response;
        if(MovieModel.findMovieServer(movieId).equals(serverName)){
            if(moviesEvents.get(movieName).containsKey(movieId)){
                List<String> clientsList = moviesEvents.get(movieName).get(movieId).getRegisteredClients();
                moviesEvents.get(movieName).remove(movieId);
                addCustomersToNextMovieSlot(movieId, movieName, clientsList);
                response = "SUCCESS: Movie Slot Removed";
                try {
                    Logger.serverLog(movieId, "null", " RMI removeMovieSlots ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                response = "FAILURE: Movie Slot with Id: "+movieId+" does not exist";
                try {
                    Logger.serverLog(movieId, "null", " RMI removeMovieSlots ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }
        else {
            response = "FAILURE: Cannot Remove Movie Slot from servers other than " + serverName;
            try {
                Logger.serverLog(movieId, "null", " RMI removeMovieSlots ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    private void addCustomersToNextMovieSlot(String movieId, String movieName, List<String> clientsList) throws RemoteException {
        for (String customerID : clientsList) {
            if (customerID.substring(0, 3).equals(movieId.substring(0,3))) {
                clientMovies.get(customerID).get(movieName).remove(movieId);
                String nextSameEventResult = getNextSameEvent(moviesEvents.get(movieName).keySet(), movieName, movieId);
                if (nextSameEventResult.equals("FAILURE")) {
                    return;
                } else {
                    bookMoviesTickets(customerID, nextSameEventResult, movieName,0);
                }
            } else {
                sendUDPMessage(getServerPort(customerID.substring(0, 3)), "removeEvent", customerID, movieName, movieId,MINVALUE );
            }
        }
    }

    private String getNextSameEvent(Set<String> keySet, String movieName, String movieId) {
        List<String> sortedIDs = new ArrayList<String>(keySet);
        sortedIDs.add(movieId);
        Collections.sort(sortedIDs, new Comparator<String>() {
            @Override
            public int compare(String ID1, String ID2) {
                Integer timeSlot1 = 0;
                switch (ID1.substring(3, 4).toUpperCase()) {
                    case "M":
                        timeSlot1 = 1;
                        break;
                    case "A":
                        timeSlot1 = 2;
                        break;
                    case "E":
                        timeSlot1 = 3;
                        break;
                }
                Integer timeSlot2 = 0;
                switch (ID2.substring(3, 4).toUpperCase()) {
                    case "M":
                        timeSlot2 = 1;
                        break;
                    case "A":
                        timeSlot2 = 2;
                        break;
                    case "E":
                        timeSlot2 = 3;
                        break;
                }
                Integer date1 = Integer.parseInt(ID1.substring(8, 10) + ID1.substring(6, 8) + ID1.substring(4, 6));
                Integer date2 = Integer.parseInt(ID2.substring(8, 10) + ID2.substring(6, 8) + ID2.substring(4, 6));
                int dateCompare = date1.compareTo(date2);
                int timeSlotCompare = timeSlot1.compareTo(timeSlot2);
                if (dateCompare == 0) {
                    return ((timeSlotCompare == 0) ? dateCompare : timeSlotCompare);
                } else {
                    return dateCompare;
                }
            }
        });
        int index = sortedIDs.indexOf(movieId) + 1;
        for (int i = index; i < sortedIDs.size(); i++) {
            if (!moviesEvents.get(movieName).get(sortedIDs.get(i)).isHouseful()) {
                return sortedIDs.get(i);
            }
        }
        return "FAILURE";
    }

    @Override
    public String listMovieShowsAvailability(String movieName) throws RemoteException {
        String response;
        Map<String,MovieModel> slots = moviesEvents.get(movieName);
        StringBuffer sb = new StringBuffer();
        sb.append(serverName+" Server "+ movieName+":\n");
        if(slots.size()==0){
            sb.append("No Movie Slots for Movie: "+ movieName);
        }
        else{
            for(MovieModel movies: slots.values()){
                sb.append(" "+movies.toString()+" || ");
            }
            sb.append("\n=====================================\n");
        }
        String server1, server2;
        if(serverId.equals("ATW")){
            server1 = sendUDPMessage(VERDUN_SERVER_PORT,"listMovieAvailability","null",movieName,"null",MINVALUE);
            server2 = sendUDPMessage(OUTREMONT_SERVER_PORT,"listMovieAvailability","null",movieName,"null",MINVALUE);
        }
        else if(serverId.equals("VER")){
            server1 = sendUDPMessage(ATWATER_SERVER_PORT,"listMovieAvailability","null",movieName,"null",MINVALUE);
            server2 = sendUDPMessage(OUTREMONT_SERVER_PORT,"listMovieAvailability","null",movieName,"null",MINVALUE);
        }
        else
        {
            server1 = sendUDPMessage(ATWATER_SERVER_PORT,"listMovieAvailability","null",movieName,"null",MINVALUE);
            server2 = sendUDPMessage(VERDUN_SERVER_PORT,"listMovieAvailability","null",movieName,"null",MINVALUE);
        }
        sb.append(server1).append(server2);

        response = sb.toString();
        try {
            Logger.serverLog(serverId, "null", " RMI listMovieShowsAvailability ", " movieName: " + movieName +  " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String bookMoviesTickets(String customerId, String movieId, String movieName, int numberOfTickets) throws RemoteException {
        String response;
        if (!serverClients.containsKey(customerId)) {
            addNewCustomerToClients(customerId);
        }
        if (MovieModel.findMovieServer(movieId).equals(serverName)) {
            MovieModel bookedEvent = moviesEvents.get(movieName).get(movieId);
            if ((!bookedEvent.isHouseful()) && numberOfTickets <= bookedEvent.getRemainingCapacity()) {
                if (clientMovies.containsKey(customerId)) {
                    if (clientMovies.get(customerId).containsKey(movieName)) {
                        if (!clientMovies.get(customerId).get(movieName).contains(movieId)) {
                            //    for(int i = 0;i<numberOfTickets;i++){
                            clientMovies.get(customerId).get(movieName).add(movieId);
                            //    }



                        } else {
                            response = "FAILURE: Movie " + movieId + " Already Booked";
                            try {
                                Logger.serverLog(serverId, customerId, " RMI bookMovieTicket ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }
                    } else {
                        List<String> temp = new ArrayList<>();
                        temp.add(movieId);
                        clientMovies.get(customerId).put(movieName, temp);

                    }
                } else {
                    Map<String, List<String>> temp = new ConcurrentHashMap<>();
                    List<String> temp2 = new ArrayList<>();
                    temp2.add(movieId);
                    temp.put(movieName, temp2);
                    clientMovies.put(customerId, temp);
//                    /movieBookings.put(customerId+movieId+movieName,numberOfTickets);
                }
                if (moviesEvents.get(movieName).get(movieId).addRegisteredClientId(customerId) == MovieModel.SUCCESS) {
                    response = "SUCCESS: Movie " + movieId + " Booked Successfully"+ " For "+numberOfTickets+" Tickets";
                } else if (moviesEvents.get(movieName).get(movieId).addRegisteredClientId(customerId) == MovieModel.HOUSE_FULL) {
                    response = "FAILED: Movie " + movieId + " Does not have "+numberOfTickets+" Tickets available!";
                } else {
                    response = "FAILED: Cannot Add You To Event " + movieId;
                }
                try {
                    Logger.serverLog(serverId, customerId, " RMI bookMovieTicket ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                response = "FAILED: Movie " + movieId + " Does not have "+numberOfTickets+" Tickets available!";
                try {
                    Logger.serverLog(serverId, customerId, " RMI bookMovieTicket ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            movieBookings.put(customerId+movieId+movieName,numberOfTickets);
            System.out.println(movieBookings);
            return response;
        } else {
            if (!exceedWeeklyLimit(customerId)) {
                String serverResponse = sendUDPMessage(getServerPort(movieId.substring(0, 3)), "bookMovie", customerId, movieName, movieId,numberOfTickets);
                if (serverResponse.startsWith("SUCCESS:")) {
                    if (clientMovies.get(customerId).containsKey(movieName)) {
                        clientMovies.get(customerId).get(movieName).add(movieId);
                    } else {
                        List<String> temp = new ArrayList<>();
                        temp.add(movieId);
                        clientMovies.get(customerId).put(movieName, temp);
                    }
                }
                try {
                    Logger.serverLog(serverId, customerId, " RMI bookMovieTicket ", " movieId: " + movieId + " movieName: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                movieBookings.put(customerId+movieId+movieName,numberOfTickets);
                System.out.println(movieBookings);
                return serverResponse;
            } else {
                response = "FAILURE: Unable to Book Movie For This Week In Another Servers(Max Weekly Limit = 3)";
                try {
                    Logger.serverLog(serverId, customerId, " RMI bookMovieTicket ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }
    private boolean exceedWeeklyLimit(String customerId){
        int count=0;
        for(String index: clientMovies.get(customerId).keySet()){
            for(String mIndex : clientMovies.get(customerId).get(index))
            {
                if(!mIndex.substring(0, 3).equals(customerId.substring(0, 3))){
                    count++;
                    if(count>=3){
                        return true;
                    }
                }
            }
        }
        return false;
    }




    @Override
    public String getBookingSchedule(String customerId) throws RemoteException {
        String response;
        if (!serverClients.containsKey(customerId)) {
            addNewCustomerToClients(customerId);
            response = "Booking Schedule Empty For " + customerId;
            try {
                Logger.serverLog(serverId, customerId, " RMI getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        Map<String, List<String>> movies = clientMovies.get(customerId);
        if (movies.size() == 0) {
            response = "Booking Schedule Empty For " + customerId;
            try {
                Logger.serverLog(serverId, customerId, " RMI getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        StringBuffer builder = new StringBuffer();
        for (String movieNames :
                movies.keySet()) {
            builder.append(movieNames + ":\n");
            for (String movieId :
                    movies.get(movieNames)) {

                builder.append(movieId).append("\t").append(movieBookings.get(customerId+movieId+movieNames));
            }
            builder.append("\n=====================================\n");
        }
        response = builder.toString();
        try {
            Logger.serverLog(serverId, customerId, " RMI getBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String cancelMovieTickets(String customerId, String movieId, String movieName, int numberOfTickets) throws RemoteException {
        String response;
        int qty = movieBookings.get(customerId+movieId+movieName);
        if (MovieModel.findMovieServer(movieId).equals(serverName)) {
            if (customerId.substring(0, 3).equals(serverId)) {
                if (!serverClients.containsKey(customerId)) {
                    addNewCustomerToClients(customerId);
                    response = "FAILED: You " + customerId + " Have not booked " + movieId;
                    try {
                        Logger.serverLog(serverId, customerId, " RMI cancelMovieTickets ", " movieId: " + movieId + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if(numberOfTickets>qty){
                    response = "FAILURE: You don't have "+numberOfTickets+" Tickets booked.";
                }
                else if(numberOfTickets<qty){
                    movieBookings.put(customerId+movieId+movieName,qty-numberOfTickets);
                    response = "SUCCESS: "+numberOfTickets+" Movie Tickets cancelled for "+customerId;
                }

                else{
                    if (clientMovies.get(customerId).get(movieName).remove(movieId)) {


                        moviesEvents.get(movieName).get(movieId).removeRegisteredClientId(customerId);
                        movieBookings.remove(customerId+movieId+movieName);
                        clientMovies.get(customerId).get(movieName).remove(movieId);
                        response = "SUCCESS: MOVIE " + movieId + " Canceled for " + customerId;
                        try {
                            Logger.serverLog(serverId, customerId, " RMI cancelMovieTickets ", " movieID: " + movieId + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        response = "FAILURE: You " + customerId + " Are Not Registered in " + movieId;
                        try {
                            Logger.serverLog(serverId, customerId, " RMI cancelMovieTickets ", " movieID: " + movieId + " movieName: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            else {
                if (moviesEvents.get(movieName).get(movieId).removeRegisteredClientId(customerId)) {
                    movieBookings.remove(customerId+movieId+movieName);
                    response = "SUCCESS: Movie " + movieId + " Cancelled for " + customerId;
                    try {
                        Logger.serverLog(serverId, customerId, " RMI cancelMovieTickets ", " movieID: " + movieId + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    response = "FAILURE: You " + customerId + " Are Not Registered in " + movieId;
                    try {
                        Logger.serverLog(serverId, customerId, " RMI cancelMovieTickets ", " movieID: " + movieId + " movieName: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response;
        } else {
            if (customerId.substring(0, 3).equals(serverId)) {
                if (!serverClients.containsKey(customerId)) {
                    addNewCustomerToClients(customerId);
                } else {
                    if (clientMovies.get(customerId).get(movieName).remove(movieId)) {
                        return sendUDPMessage(getServerPort(movieId.substring(0, 3)), "cancelMovie", customerId, movieName, movieId,numberOfTickets);
                    }
                }
            }
            return "FAILURE: You " + customerId + " Are Not Registered in " + movieId;
        }

    }

    private String sendUDPMessage(int serverPort, String method, String customerId, String movieName, String  movieId, Integer value) {
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + customerId + ";" + movieName + ";" + movieId+";"+value;
        try {
            Logger.serverLog(serverId, customerId, " UDP request sent " + method + " ", " movieId: " + movieId + " movieName: " + movieName + " ", " ... ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            String[] parts = result.split(";");
            result = parts[0];
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        try {
            Logger.serverLog(serverId, customerId, " UDP reply received" + method + " ", " movieId: " + movieId + " movieName: " + movieName + " ", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    public String removeMovieUDP(String oldMovieId, String movieName, String customerId) {
        if (!serverClients.containsKey(customerId)) {
            addNewCustomerToClients(customerId);
            return "FAILURE: You " + customerId + " Are Not Registered in " + oldMovieId;
        } else {
            if (clientMovies.get(customerId).get(movieName).remove(oldMovieId)) {
                return "SUCCESS: Event " + oldMovieId + " Was Removed from " + customerId + " Schedule";
            } else {
                return "FAILURE: You " + customerId + " Are Not Registered in " + oldMovieId;
            }
        }
    }

    public String listMovieAvailabilityUDP(String movieName) {
        Map<String, MovieModel> movies = moviesEvents.get(movieName);
        StringBuffer builder = new StringBuffer();
        builder.append("\n");
        builder.append("\n"+serverName + " Server " + movieName + ":\n");
        if (movies.size() == 0) {
            builder.append("No Events of Type " + movieName);
        } else {
            for (MovieModel movie :
                    movies.values()) {
                builder.append(movie.toString() + " || ");
            }
        }
        builder.append("\n=====================================\n");
        return builder.toString();
    }


    public void addNewCustomerToClients(String customerId) {
        ClientsModel newCustomer = new ClientsModel(customerId);
        serverClients.put(newCustomer.getClientId(), newCustomer);
        clientMovies.put(newCustomer.getClientId(), new ConcurrentHashMap<>());
    }


}
