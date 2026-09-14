package com.ass1.client;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.ass1.server.ServerInterface;

// The client reads a input file, sends it to the server using RMI and writes results to an output file.
// Line parsing (Query.parseLine / Query.readQueries) lives in Query.java, next to this file.
public class Client {
    public static void main(String[] args) throws Exception {
        // TODO: Read in the inputFilePath and outpout. read query file, wait for T ms, do RMI call and measurements, store the results, write results to file, calculate total, average and so on.
        // TODO: Change this Temporary Placeholder
        System.out.println("[CLIENT] --> Not implemented yet!");
    }
}

// TODO: Use this from the group session?
// public class Client {
//     public static void main(String[] args) {
//         try {
//             Registry registry = LocateRegistry.getRegistry();
//             ServerInterface server = (ServerInterface) registry.lookup("server");
//             System.out.println(server.Add(10,20));
//         } catch (RemoteException | NotBoundException e) {
//             e.printStackTrace();
//         }
//     }
// }
