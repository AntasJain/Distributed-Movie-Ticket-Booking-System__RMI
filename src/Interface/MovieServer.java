package Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;
public interface MovieServer extends Remote {
    String addMovieSlots(String movieId, String movieName, int bookingCapacity) throws RemoteException;
    String removeMovieSlots(String movieId, String movieName) throws RemoteException;
    String listMovieShowsAvailability(String movieName) throws RemoteException;
    String bookMoviesTickets(String customerId, String movieId, String movieName, int numberOfTickets) throws RemoteException;
    String getBookingSchedule(String customerId) throws RemoteException;
    String cancelMovieTickets(String customerId, String movieId, String movieName, int numberOfTickets) throws RemoteException;

}
