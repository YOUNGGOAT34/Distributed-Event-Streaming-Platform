import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Main {
  public static void main(String[] args) throws IOException{
  
    System.err.println("Logs from your program will appear here!");

  
  
    ServerSocket serverSocket = null;

    int port = 9092;


    serverSocket = new ServerSocket(port);
    serverSocket.setReuseAddress(true);


      while(true){
          Socket clientSocket = serverSocket.accept();
          new Thread(()->{
              handleClient(clientSocket);
          }).start();

      }

  }



  private static boolean readAllDataFromSocket(InputStream in,byte[] buffer) throws  IOException{
    int totalRead=0;
    while(totalRead<buffer.length){
       int bytesRead=in.read(buffer,totalRead,buffer.length-totalRead);
       if(bytesRead==-1){
         return false;
       }

       totalRead+=bytesRead;
    }
    return true;
  }


  public static void handleClient(Socket clientSocket){


      try {

          InputStream in=clientSocket.getInputStream();
          OutputStream out=clientSocket.getOutputStream();

          while(true){

              //message size is 4 bytes
              byte[] messageSizeInBytes=new byte[4];
              if(!readAllDataFromSocket(in,messageSizeInBytes)){
                  break;
              }

              int messageSize= ByteBuffer.wrap(messageSizeInBytes).getInt();

              //read the rest of the message ,we already have the size to read
              byte[] messageBytes=new byte[messageSize];
              if(!readAllDataFromSocket(in,messageBytes)){
                  break;
              }


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


              //      00 00 00 13  // message_size:      19 bytes
              //      ab cd ef 12  // correlation_id:    (matches request)
              //start of body.......
              //      00 00        // error_code:        0 (no error)
              //      02           // api_keys array length:    1 element
              //      00 12        // api_key:           18 (ApiVersions)
              //      00 00        // min_version:       0
              //      00 04        // max_version:       4
              //      00           // TAG_BUFFER:        empty
              //      00 00 00 00  // throttle_time_ms:  0
              //      00           // TAG_BUFFER:        empty
              //end of body


              ByteBuffer  body=ByteBuffer.allocate(15);

              body.putShort((short)0);//error code
              body.put((byte)2);// compact array length
              body.putShort((short)18);//api key
              body.putShort((short)0);//min version
              body.putShort((short)4);//max version
              body.put((byte)0);//tag buffer
              body.putInt(0);//throttle time in ms
              body.put((byte)0);//tag buffer


              //sending response

              int responseMessageSize=4+body.capacity(); //correlation id + body

              ByteBuffer response=ByteBuffer.allocate(4+responseMessageSize);
              response.putInt(responseMessageSize);
              response.putInt(correlationId);
              response.put(body.array());

              out.write(response.array());
              out.flush();


          }


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
