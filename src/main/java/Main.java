import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Main {
  public static void main(String[] args){
  
    System.err.println("Logs from your program will appear here!");

  
  
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    int port = 9092;

    try {
      serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept();

      InputStream in=clientSocket.getInputStream();
      OutputStream out=clientSocket.getOutputStream();


      //message size is 4 bytes
      byte[] messageSizeInBytes=new byte[4];
      in.read(messageSizeInBytes);

      int messageSize= ByteBuffer.wrap(messageSizeInBytes).getInt();

      //read the rest of the message ,we already have the size to read
      byte[] messageBytes=new byte[messageSize];
      in.read(messageBytes);


      ByteBuffer buffer=ByteBuffer.wrap(messageBytes);

      //skip the request api key(2 bytes) and the request api version(2 bytes) , 4 bytes in total
      /*
          request api key 2 bytes
          request api version 2 bytes
          correlation id 4 bytes


       */
      buffer.getShort();
      short apiVersion=buffer.getShort();

      short errorCode;

      if(apiVersion<0 || apiVersion>4){
         errorCode=35;
      }else{
         errorCode=0;
      }


      int correlationId=buffer.getInt();

      System.out.println("Correlation id "+correlationId);

      //sending response
      ByteBuffer response=ByteBuffer.allocate(8);
      response.putInt(0);
      response.putInt(correlationId);
      response.putShort(errorCode);

      out.write(response.array());
      out.flush();

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (clientSocket != null) {
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
      }
    }
  }

}
