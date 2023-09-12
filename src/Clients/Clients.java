package Clients;

import Assets.StringAssets;
import Interface.MovieServer;
import LogWriter.Logger;
import Models.MovieModel;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Clients {
    public static final int USER_TYPE_CUSTOMER = 1;
    public static final int USER_TYPE_ADMIN = 2;
    public static final int CUSTOMER_BOOK_MOVIE = 1;
    public static final int CUSTOMER_GET_MOVIE_SCHEDULE = 2;
    public static final int CUSTOMER_CANCEL_MOVIE = 3;
    public static final int CUSTOMER_LOGOUT = 4;
    public static final int ADMIN_ADD_MOVIE = 1;
    public static final int ADMIN_REMOVE_MOVIE = 2;
    public static final int ADMIN_LIST_MOVIE_AVAILABILITY = 3;
    public static final int ADMIN_BOOK_MOVIE = 4;
    public static final int ADMIN_GET_BOOKING_SCHEDULE = 5;
    public static final int ADMIN_CANCEL_MOVIE = 6;
    public static final int ADMIN_LOGOUT = 7;
    public static final int SERVER_ATWATER = 2964;
    public static final int SERVER_VERDUN = 2965;
    public static final int SERVER_OUTREMONT = 2966;
    public static final String MOVIE_MANAGEMENT_REGISTERED_NAME = "MOVIE_MANAGEMENT";

    static Scanner input;

    public static void main(String[] args) throws IOException{
        init();
    }

    public static void init() throws IOException{
        input = new Scanner(System.in);
        String userId;
        System.out.println("Enter your UserID: ");
        userId=input.next().trim().toUpperCase();
        Logger.clientLog(userId, "login attempt");
        switch (checkUserType(userId)){
            case USER_TYPE_CUSTOMER:
                try {
                    System.out.println("Customer Login successful (" + userId + ")");
                    Logger.clientLog(userId, " Customer Login successful");
                    customer(userId, getServerPort(userId.substring(0, 3)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case USER_TYPE_ADMIN:
                try {
                    System.out.println("Admin Login successful (" + userId + ")");
                    Logger.clientLog(userId, " Admin Login successful");
                    admin(userId, getServerPort(userId.substring(0, 3)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:
                System.out.println("UserID is not in correct format. Please enter correct UserID");
                Logger.clientLog(userId, " UserID is not in correct format");
                Logger.deleteALogFile(userId);
                init();
        }
    }

    public static int checkUserType(String userId){
        if (userId.length() == 8) {
            if (userId.substring(0, 3).equalsIgnoreCase("ATW") ||
                    userId.substring(0, 3).equalsIgnoreCase("OUT") ||
                    userId.substring(0, 3).equalsIgnoreCase("VER")) {
                if (userId.substring(3, 4).equalsIgnoreCase("C")) {
                    return USER_TYPE_CUSTOMER;
                } else if (userId.substring(3, 4).equalsIgnoreCase("A")) {
                    return USER_TYPE_ADMIN;
                }
            }
        }
        return 0;
    }

    private static int getServerPort(String branchAcronym) {
        if (branchAcronym.equalsIgnoreCase("ATW")) {
            return SERVER_ATWATER;
        } else if (branchAcronym.equalsIgnoreCase("VER")) {
            return SERVER_VERDUN;
        } else if (branchAcronym.equalsIgnoreCase("OUT")) {
            return SERVER_OUTREMONT;
        }
        return 1;
    }

    private static void printMenu(int userType) {
        System.out.println("*************************************");
        System.out.println("Please choose an option below:");
        if (userType == USER_TYPE_CUSTOMER) {
            System.out.println("1.Book Movie Tickets");
            System.out.println("2.Get Booking Schedule");
            System.out.println("3.Cancel Movie");
            System.out.println("4.Logout");
        } else if (userType == USER_TYPE_ADMIN) {
            System.out.println("1.Add Movie");
            System.out.println("2.Remove Movie");
            System.out.println("3.List Movie Availability");
            System.out.println("4.Book Movie");
            System.out.println("5.Get Movie Schedule");
            System.out.println("6.Cancel Movie");
            System.out.println("7.Logout");
        }
    }

    private static void customer(String customerID, int serverPort) throws Exception {
        if (serverPort == 1) {
            return;
        }
        Registry registry = LocateRegistry.getRegistry(serverPort);
        MovieServer remoteObject = (MovieServer) registry.lookup(MOVIE_MANAGEMENT_REGISTERED_NAME);
        boolean repeat = true;
        printMenu(USER_TYPE_CUSTOMER);
        int menuSelection = input.nextInt();
        String movieName;
        String movieID;
        String serverResponse;
        int numberOfTickets;
        switch (menuSelection) {
            case CUSTOMER_BOOK_MOVIE:
                movieName = promptForMovieType();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(customerID, " attempting to book Movie");
                serverResponse = remoteObject.bookMoviesTickets(customerID, movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " bookMovie", " movieID: " + movieID + " movieName: " + movieName + " Number of tickets: "+numberOfTickets+" ", serverResponse);
                break;
            case CUSTOMER_GET_MOVIE_SCHEDULE:
                Logger.clientLog(customerID, " attempting to getMovieSchedule");
                serverResponse = remoteObject.getBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " bookMovie", " null ", serverResponse);
                break;
            case CUSTOMER_CANCEL_MOVIE:
                movieName = promptForMovieType();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(customerID, " attempting to cancelEvent");
                serverResponse = remoteObject.cancelMovieTickets(customerID, movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(customerID, " cancelMovie", " movieID: " + movieID + " movieName: " + movieName + " Number of tickets: "+numberOfTickets+" ", serverResponse);
                break;
            case CUSTOMER_LOGOUT:
                repeat = false;
                Logger.clientLog(customerID, " attempting to Logout");
                init();
                break;
        }
        if (repeat) {
            customer(customerID, serverPort);
        }
    }

    private static void admin(String movieAdminId, int serverPort) throws Exception {
        if (serverPort == 1) {
            return;
        }
        Registry registry = LocateRegistry.getRegistry(serverPort);
        MovieServer remoteObject = (MovieServer) registry.lookup(MOVIE_MANAGEMENT_REGISTERED_NAME);
        boolean repeat = true;
        printMenu(USER_TYPE_ADMIN);
        String customerID;
        String movieName;
        String movieID;
        String serverResponse;
        int numberOfTickets;
        int menuSelection = input.nextInt();
        switch (menuSelection) {
            case ADMIN_ADD_MOVIE:
                movieName = promptForMovieType();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(movieAdminId, " attempting to addMovie");
                serverResponse = remoteObject.addMovieSlots(movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminId, " addMovie", " movieId: " + movieID + " movieName: " + movieName + " movieCapacity: " + numberOfTickets + " ", serverResponse);
                break;
            case ADMIN_REMOVE_MOVIE:
                movieName = promptForMovieType();
                movieID = promptForMovieID();
                Logger.clientLog(movieAdminId, " attempting to removeMovie");
                serverResponse = remoteObject.removeMovieSlots(movieID, movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminId, " removeMovie", " movieId: " + movieID + " movieName: " + movieName + " ", serverResponse);
                break;
            case ADMIN_LIST_MOVIE_AVAILABILITY:
                movieName = promptForMovieType();
                Logger.clientLog(movieAdminId, " attempting to listMovieAvailability");
                serverResponse = remoteObject.listMovieShowsAvailability(movieName);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminId, " listMovieAvailability", " movieName: " + movieName + " ", serverResponse);
                break;
            case ADMIN_BOOK_MOVIE:
                customerID = askForCustomerIDFromAdmin(movieAdminId.substring(0, 3));
                movieName = promptForMovieType();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(movieAdminId, " attempting to bookMovie");
                serverResponse = remoteObject.bookMoviesTickets(customerID, movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminId, " bookMovie", " customerID: " + customerID + " movieId: " + movieID + " movieName: " + movieName + " movieCapacity: "+numberOfTickets+" ", serverResponse);
                break;
            case ADMIN_GET_BOOKING_SCHEDULE:
                customerID = askForCustomerIDFromAdmin(movieAdminId.substring(0, 3));
                Logger.clientLog(movieAdminId, " attempting to getBookingSchedule");
                serverResponse = remoteObject.getBookingSchedule(customerID);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminId, " getBookingSchedule", " customerID: " + customerID + " ", serverResponse);
                break;
            case ADMIN_CANCEL_MOVIE:
                customerID = askForCustomerIDFromAdmin(movieAdminId.substring(0, 3));
                movieName = promptForMovieType();
                movieID = promptForMovieID();
                numberOfTickets = promptForNumberOfTickets();
                Logger.clientLog(movieAdminId, " attempting to cancelMovie");
                serverResponse = remoteObject.cancelMovieTickets(customerID, movieID, movieName, numberOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(movieAdminId, " cancelMovie", " customerID: " + customerID + " movieId: " + movieID + " movieName: " + movieName +" numberOfTickets: " + numberOfTickets +  " ", serverResponse);
                break;
            case ADMIN_LOGOUT:
                repeat = false;
                Logger.clientLog(movieAdminId, "attempting to Logout");
                init();
                break;
        }
        if (repeat) {
            admin(movieAdminId, serverPort);
        }
    }
    private static String askForCustomerIDFromAdmin(String branchAcronym) {
        System.out.println("Please enter a customerID(Within " + branchAcronym + " Server):");
        String userID = input.next().trim().toUpperCase();
        if (checkUserType(userID) != USER_TYPE_CUSTOMER || !userID.substring(0, 3).equals(branchAcronym)) {
            return askForCustomerIDFromAdmin(branchAcronym);
        } else {
            return userID;
        }
    }

    private static String promptForMovieType() {
        System.out.println("*************************************");
        System.out.println("Please choose a Movie name from below:");
        System.out.println("1.Avatar");
        System.out.println("2.Avengers");
        System.out.println("3.Titanic");
        input.nextLine();
        int movieIndex = input.nextInt();
        switch (movieIndex) {
            case 1:
                return StringAssets.AVATAR_MOVIE;
            case 2:
                return StringAssets.AVENGERS_MOVIE;
            case 3:
                return StringAssets.TITANIC_MOVIE;
        }
        return promptForMovieType();
    }

    private static String promptForMovieID() {
        System.out.println("*************************************");
        System.out.println("Please enter the MovieID");
        String eventID = input.next().trim().toUpperCase();
        if (eventID.length() == 10) {
            if (eventID.substring(0, 3).equalsIgnoreCase("ATW") ||
                    eventID.substring(0, 3).equalsIgnoreCase("OUT") ||
                    eventID.substring(0, 3).equalsIgnoreCase("VER")) {
                if (eventID.substring(3, 4).equalsIgnoreCase("M") ||
                        eventID.substring(3, 4).equalsIgnoreCase("A") ||
                        eventID.substring(3, 4).equalsIgnoreCase("E")) {
                    return eventID;
                }
            }
        }
        return promptForMovieID();
    }

    private static int promptForNumberOfTickets(){
        System.out.println("*************************************");
        System.out.println("Please enter the number of tickets: ");
        int numberOfTickets = Integer.parseInt(input.next().trim());
        return numberOfTickets;
    }

    private static int promptForCapacity() {
        System.out.println("*************************************");
        System.out.println("Please enter the Movie capacity:");
        return input.nextInt();
    }


}
