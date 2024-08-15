
package OSProject_File3_1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;


public class Server {

    public static final String FILES_PATH = "D:\\code SU\\OS_Netbean\\files\\server\\";    
    public static final int PORT = 3030;
    private static ArrayList<Socket> clientsList;
    
    public Server(){
        clientsList = new ArrayList<Socket>();
        try (ServerSocket serverSocket = new ServerSocket(PORT)){ //Server opened
            System.out.println("This is Server , start on port " + PORT );
            while(true){
                //Method acceptConnection
                Socket clientSocket = serverSocket.accept();
                if(clientSocket.isConnected()){
                    clientsList.add(clientSocket); // add client to ArrayList
                    System.out.println("New client has connected to the server -> " + clientSocket + " #" +clientsList.size());
//                    System.out.println("Amount of Client Connected : " + clientsList.size() );
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            }

        } catch (IOException e) {
            System.err.println("Error from Server()");
//            e.printStackTrace();
        }
    }
    
    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private BufferedReader inTextFromClient;
        private PrintWriter outTextToClient;
        
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            //(BufferedReader inTextFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); /*read data from client*/
            //PrintWriter outTextToClient = new PrintWriter(clientSocket.getOutputStream(), true); /*send data to client*/)
            try{
                inTextFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); /*read data from client*/
                outTextToClient = new PrintWriter(clientSocket.getOutputStream(), true); /*send data to client*/
                String clientCommand;
                while( (clientCommand = inTextFromClient.readLine()) != null ){
                    switch (clientCommand) {
                        case "/filelist":
                            //method filelist
                            fileList();
                            break;
                        case "/request":
                            //method download file
                            String fileName = inTextFromClient.readLine(); //recieve fileName
                            //recieve file
                            File file = new File(FILES_PATH + fileName); //create file
                            if (file.exists()) {
                                outTextToClient.println("Long");
                                outTextToClient.println(file.length()); //length() -> return long
                            }
                            else{
                                outTextToClient.println("String");
                                outTextToClient.println("File Not Found, Please try agian");
                            }
                            break;
                        case "download":
                            String filename = inTextFromClient.readLine();
                            long startByte = Long.parseLong(inTextFromClient.readLine());
                            long endByte = Long.parseLong(inTextFromClient.readLine());
                            //Method
                            sendFile(filename, startByte, endByte);
                            if(clientSocket.isConnected()){
                                System.err.println("Client (" + clientSocket + ") has disconnected");
                                clientsList.remove(clientSocket);
                                System.err.println("Amount of Client Connected Left : " + clientsList.size());
                            }
                            break;
                        case "/exit":
                            //shutdown client
                            inTextFromClient.close();
                            outTextToClient.close();
                            if(clientSocket.isConnected()){
                                System.err.println("Client (" + clientSocket + ") has disconnected");
                                clientsList.remove(clientSocket);
                                System.err.println("Amount of Client Connected Left : " + clientsList.size());
                            }
                            break;
                        default:
                            System.out.println("Wrong command , Please try again");
                            break;
                    }
                }
            }
            catch (IOException e) {
                if(clientSocket.isConnected()){
//                    System.err.println("Client #"+ clientsList.size() +"(" + clientSocket + ") has disconnected");
                    clientsList.remove(clientSocket);
//                    System.err.println("Amount of Client Connected Left : " + clientsList.size());
                }
//                e.printStackTrace();
            }
//            finally{
//                System.out.println("Final Amount of Client Connected : " + clientsList.size() );
//            }

        }
    
        public void fileList(){
            String allFilesInServer = "** Files **\n";
            File[] fileList = new File(FILES_PATH).listFiles();
            for(int i=0 ; i<fileList.length ; i++){
                //concat string
                allFilesInServer += String.format("* [%d] - %s\n",i+1,fileList[i].getName()); 
            }
            allFilesInServer += "** End Files **";
            outTextToClient.println(allFilesInServer);
            // send End signal because Bufferedreader.readline() need EOF if we don't have EOF readline() will not return null
            outTextToClient.println("EOF");
            
            System.out.println("Send fileList to Client successful");
        }
    
        public void sendFile(String filename, long startByte, long endByte){
            try(BufferedOutputStream outBytetoClient = new BufferedOutputStream(clientSocket.getOutputStream());
                RandomAccessFile inByteFromFile = new RandomAccessFile(new File(FILES_PATH + filename),"r"); ){
                
                inByteFromFile.seek(startByte); // read byte specific from start to end position 
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                long partSize = endByte - startByte;
                
                while( totalBytesRead < partSize  && ( bytesRead = inByteFromFile.read(buffer) ) != -1 ){
                    if (totalBytesRead + bytesRead > partSize) {
                        bytesRead = (int)(partSize - totalBytesRead);
                    }
                    totalBytesRead += bytesRead;
                    outBytetoClient.write(buffer, 0, bytesRead);
                }
                outBytetoClient.flush();
                System.out.println("download success");
            } catch (IOException e) {
                System.err.println("Error from ClientHandler() send file");
                e.printStackTrace();
            }
            
           
        }

        
    }
    
    public static void main(String[] args) {
        new Server();
    }
}
