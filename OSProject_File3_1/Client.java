/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package OSProject_File3_1;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Client {
    
    public static final String IPSERVER = "127.0.0.1";
    public static final int PORT = 3030;
    private static final String CLIENT_FILEPATH = "D:\\code SU\\OS_Netbean\\files\\client\\";
    private Scanner scanner;
    private BufferedReader inTextFromServer;
    private PrintWriter outTextToServer;
    private static final int THREAD_COUNT = 10;
    private ExecutorService threadPool;
    
    public Client(){
        try( Socket clientSocket = new Socket(IPSERVER , PORT) ){
            System.out.println("This is Client Site");
            System.out.println("Client has connected to server -> (" + clientSocket + ")");
            //Create object
            this.scanner = new Scanner(System.in);
            this.inTextFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //Read data from server
            this.outTextToServer = new PrintWriter(clientSocket.getOutputStream(),true); //used to send the data to server -> println()
            //Loop run
            String command = "";
            while(!clientSocket.isClosed()){
                System.out.println("----------------------------------------");
                System.out.println("-- All Command --");
                System.out.println("Type /filelist to see file list in server");
                System.out.println("Type /request to download file from server");
                System.out.println("Type /exit to exit the program");
                System.out.println("Enter your command : ");
                
                command = scanner.nextLine(); //type text from keyboard
                switch (command) {
                    case "/filelist":
                        //method filelist
                        outTextToServer.println(command); // send command to server
                        String allFilesInServer;
                        while( (allFilesInServer = inTextFromServer.readLine() ) != null ){
                            if ("EOF".equals(allFilesInServer)) {
                                break;
                            }
                            System.out.println(allFilesInServer);
                        }
                        break;
                    case "/request": //use thread here
                        //method download file
                        outTextToServer.println(command); // send command to server
                        System.out.println("Enter specific filename that you want to download");
                        String filename = scanner.nextLine();
                        outTextToServer.println(filename); //send filename to server
                         
                        String messageFromServer = inTextFromServer.readLine(); //receive Long/String from server
                        long fileSize = 0;
                        String errorFromServer = "";
                        if(messageFromServer.equals("Long")){
                            fileSize = Long.parseLong(inTextFromServer.readLine());
                        }
                        else if(messageFromServer.equals("String")){
                            errorFromServer = inTextFromServer.readLine();
                        }
 
//                        long fileSize = Long.parseLong(inTextFromServer.readLine()); //receive message from server
                        
                        if(fileSize > 0){
                            System.out.println("FileSize : " + fileSize);
                            this.threadPool = Executors.newFixedThreadPool(THREAD_COUNT); //create threadpool has 10 thread 
                            try{
                                long partSize = fileSize / THREAD_COUNT; // Divide file into amount of Thread count parts
                                for (int i = 0; i < THREAD_COUNT; i++) {
                                    long startByte = i * partSize; //0
                                    long endByte = (i == THREAD_COUNT-1) ? fileSize : startByte + partSize; //1023
    //                                Socket clientSocketForThread = new Socket(IPSERVER,3030);
                                    threadPool.execute(new Downloader(IPSERVER, PORT, filename, startByte, endByte));
                                }
                            } finally {
                                threadPool.shutdown();
                                try {
                                    threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); //break
                                } catch (InterruptedException e) {
                                    System.err.println("Error from Threadpool awaitTermination");
                                    e.printStackTrace();
                                }
                            }
                        }
                        else{
                            System.err.println("Server said : " + errorFromServer);
                        }
                        break;
                    case "/exit":
                        outTextToServer.println(command);
                        //shutdown();
                        clientSocket.close();
                        break;
                    default:
                        System.out.println("Wrong command , Please try again");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error from Client()");
            e.printStackTrace();
        }
    }
    
    private static class Downloader implements Runnable{
        
        private String filename;
        private long startByte;
        private long endByte;
        private final String IPSERVER;
        private final int SERVERPORT;
        private String command;
        
        
        public Downloader(String IPSERVER,int PORT, String filename, long startByte, long endByte){
            this.filename = filename;
            this.startByte = startByte;
            this.endByte = endByte;
            this.IPSERVER = IPSERVER;
            this.SERVERPORT = PORT;
        }
        
        @Override
        public void run(){
            try (Socket threadSocket = new Socket(IPSERVER,SERVERPORT);
                 RandomAccessFile fileToDisk = new RandomAccessFile(CLIENT_FILEPATH + filename, "rw");
                 BufferedInputStream inByteFromServer = new BufferedInputStream(threadSocket.getInputStream());
                 PrintWriter outTextToServer = new PrintWriter(threadSocket.getOutputStream(), true)) {

                outTextToServer.println("download");//send request command
                outTextToServer.println(filename); 
                outTextToServer.println(startByte); 
                outTextToServer.println(endByte); 
                
                System.out.println("RANGE " + Thread.currentThread().getName() + " [" + startByte + " - " + endByte +"]");
                fileToDisk.seek(startByte);
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                long partSize = endByte-startByte; //Forward English 4.mkv
//                System.out.println();
                while ( totalBytesRead < partSize && (bytesRead = inByteFromServer.read(buffer)) != -1) {
                    if (totalBytesRead + bytesRead > partSize) {
                        bytesRead = (int)(partSize - totalBytesRead);
                    }
                    totalBytesRead += bytesRead;
                    fileToDisk.write(buffer, 0, bytesRead); 

                }
                
                if(totalBytesRead == partSize){
                    System.out.println(Thread.currentThread().getName() + " Final Downloaded part from: " + startByte + " to " + endByte + " [real position("+ (startByte+1) +")] , got : " +totalBytesRead + " partSize: " + partSize);   
                }
                else{
                    System.err.println(Thread.currentThread().getName() + " Final Downloaded part from: " + startByte + " to " + endByte +"  [real position("+ (startByte+1) +")] , got : " +totalBytesRead + " partSize: " + partSize);   
                }
            } catch (IOException e) {
                System.err.println("Error from Downloader method run()");
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        new Client();
    }

}
