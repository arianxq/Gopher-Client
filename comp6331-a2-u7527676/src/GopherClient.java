import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.net.UnknownHostException;

public class GopherClient {

    private static final String GOPHER_HOST = "comp3310.ddns.net";
    private static final int GOPHER_PORT = 70;
    //
    private static int countD ; // the number of Gopher directories on the server
    private static int countIR ;  // the number of invalid references
    private static int countER ; //the number of all external references
    //
    private static List<String> tFiles = new ArrayList<>(); ////store text files

    private static List<String> bFiles = new ArrayList<>();  ////store binary files

    private static List<String> tPath = new ArrayList<>(); //store text files full path
    //store binary Files full Path
    private static List<String> bPath = new ArrayList<>();

    //
    private static String sTextFile ; // smallest text file
    private static String lTextFile ; // largest Text file
    private static String sBinaryFile; // smallest binary file
    private static String lBinaryFile; //  largest binary file
    //
    private static int sTSize = Integer.MAX_VALUE;  //// size of the smallest text file size
    private static int lTSize ;  //largest text file size
    private static int sBSize = Integer.MAX_VALUE; //// size of the smallest binary file size
    private static int lBSize ; //largest binary file size


    public static void main(String[] args) {

        //Try to connect to the Gopher server and confirm
        System.out.println("");
        System.out.println("Confirm connection");
        System.out.println("---------------------");
        tryConnect(GOPHER_HOST,GOPHER_PORT);

        //Show top-level directory
        System.out.println("");
        System.out.println("Show top-level directory");
        System.out.println("---------------------");
        try {
            topLevelDirectory("");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }


        //Crawl
        System.out.println("");
        indexGopher(GOPHER_HOST,"");
        printReport();
    }

    static class GItems {
        char Type;
        String context;
        String selector;
        String server;
        int port;

        GItems(char Type, String context, String selector, String server, int port) {
            this.Type = Type;
            this.context = context;
            this.selector = selector;
            this.server = server;
            this.port = port;
        }


    }

    private static void tryConnect(String hostname, int port) {
        try {
            // Connect
            Socket socket = new Socket(hostname, port);
            System.out.println("Connected to Gopher server at " + hostname + ""+":" + port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdln = new BufferedReader(new InputStreamReader(System.in));
            // immediately sent over the network
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);//
            String msg = "Hello, Gopher!";
            out.println(msg);//sent
            System.out.println("Sent message: " + msg);
            String response = in.readLine();
//            System.out.println("Received : " + response);
            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            System.err.println("Incorrect host: " + GOPHER_HOST);
//            return "";
        } catch (IOException e) {
            System.err.println("Error request: " + e.getMessage());
//            return "";
        }
    }

    private static Socket connectServer() throws IOException {
        return new Socket(GOPHER_HOST, GOPHER_PORT);
    }

    private static void topLevelDirectory(String selector) throws IOException {
        try (Socket socket = connectServer()) {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.write(selector + "\r\n");
            out.flush(); //buffer

            String l;
//            List<String> directories = new ArrayList<>();
            while ((l = in.readLine()) != null) {
                System.out.println(l);
            }

//            for (String directory : directories) {
//                scanDirectory(directory);
//            }
        }catch (UnknownHostException e) {
            System.err.println("Incorrect host: " + GOPHER_HOST);
        } catch (IOException e) {
            System.err.println("Error request: " + e.getMessage());
        }
    }


    private static void indexGopher(String server, String selector) {
        try {
            Set<String> visited = new HashSet<>();

            List<GItems> items = getGItems(server, selector);
            storeFiles(server, items, visited);
        }catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static List<GItems> getGItems(String server, String selector) {
        List<GItems> items = new ArrayList<>();

        String response = sendRequest(server, selector);//
        for (String line : response.split("\r\n")) {

            if (!line.isEmpty() && !line.equals(".")) {// the Ending line

                GItems item = parseL(line);
                if (item != null) {
                    items.add(item);
                }
//                items.add(item); //add items

//count invalid references and external references
                char itemType = line.charAt(0);
                if (itemType == '3') {//Determining invalid references
                    countIR++;
                } else if (!item.server.equalsIgnoreCase(server)) {//Determine external references, including external references under the submenu of type "1" in addition to type "8" and "T"
                    if (itemType == '1' || itemType == '8' ||itemType == 'T') {
                        countER++;
                    }
                }
            }
        }
        return items;
    }

    private static String sendRequest(String server, String selector) { //text files
        try (Socket socket = new Socket(server, GOPHER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            //Production Request
            String request = selector + "\r\n";
            out.print(request);
//            String request = selector + "\r\n";
            out.flush();

            //response
            StringBuilder response = new StringBuilder();
            String line;
            // Print the timestamp and the client-request
            System.out.println("Timestamp: " + System.currentTimeMillis());
            System.out.println("Client-request: " + request);
            while ((line = in.readLine()) != null) {
                response.append(line).append("\r\n");
            }
            return response.toString();
        } catch (UnknownHostException e) {
            System.err.println("Incorrect host: " + GOPHER_HOST);
            return "";
        } catch (IOException e) {
            System.err.println("Error sending Gopher request: " + e.getMessage());
            return "";
        }
    }

    private static GItems parseL(String line) {
        String[] parts = line.split("\t");
        char itemType = parts[0].charAt(0);
        String context = parts[0].substring(1);
        String selector = parts[1];
        String server = parts[2];
        int port = Integer.parseInt(parts[3]);
        return new GItems(itemType, context, selector, server, port);
    }


    // with full path
    private static void storeFiles(String server, List<GItems> items, Set<String> visited) {
        for (GItems item : items) {
            if (!visited.contains(item.selector)) { //Prevention of loop
                visited.add(item.selector);

                String fullPath = "gopher://" + server + item.selector;

                if (item.Type == '0') { // Text file
                    tPath.add(fullPath);
                    String content = sendRequest(server, item.selector);
                    int contentLength = content.getBytes().length;
                    if (contentLength < sTSize) {
                        sTSize = contentLength; //smallest text files
                        sTextFile = item.context;
                    }
                    if (contentLength > lTSize) {
                        lTSize = contentLength;  //largest lest text files
                        lTextFile = item.context;
                    }
                    tFiles.add(item.context);
                } else if (item.Type == '9') { // Binary file

                    bPath.add(fullPath);
//                    byte[] binaryContent = sendRequestB(server, item.selector);
//                    byte[] binaryContent = sendRequest(server, item.selector).getBytes(StandardCharsets.UTF_8);
                    byte[] binaryContent = (byte[]) sendRequestB(server,item.selector,true);

                    int binaryContentLength = binaryContent.length;
                    if (binaryContentLength < sBSize) {
                        sBSize = binaryContentLength;  //smallest
                        sBinaryFile = item.context;
                    }
                    if (binaryContentLength > lBSize) {
                        lBSize = binaryContentLength;  //largest
                        lBinaryFile = item.context;
                    }
                    bFiles.add(item.context);
                } else if (item.Type == '1') { // Directory

                    countD++;
                    List<GItems> subItems = getGItems(server, item.selector);
                    storeFiles(server, subItems, visited); // a recursive function
                }
            }
        }
    }



    private static Object sendRequestB(String server, String selector, boolean returnBytes) {
        try (Socket socket = new Socket(server, GOPHER_PORT);// deal with binary files
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             ByteArrayOutputStream bArrayOStream = new ByteArrayOutputStream()) {
            //byte array output stream

            String request = selector + "\r\n";
            out.writeBytes(request);
            out.flush();

            System.out.println("Timestamp: " + System.currentTimeMillis());
            System.out.println("Client-request: " + request);

            byte[] buffer = new byte[4096];
            int bytes;
            InputStream inStream = socket.getInputStream();
            while ((bytes = inStream.read(buffer)) != -1) {
                bArrayOStream.write(buffer, 0, bytes);
            }

            if (returnBytes) {
                return bArrayOStream.toByteArray();
            } else {
                return bArrayOStream.toString();
            }
        } catch (UnknownHostException e) {
            System.err.println("Incorrect host: " + GOPHER_HOST);
            if (returnBytes) {
                return new byte[0];
            } else {
                return "";
            }
        } catch (IOException e) {
            System.err.println("Error sending Gopher request: " + e.getMessage());
            if (returnBytes) {
                return new byte[0];
            } else {
                return "";
            }
        }
    }



    private static void printReport() {
        System.out.println("Report of Gopher Crawl:");
        System.out.println("---------------------");
        System.out.println("a. Number of Gopher directories: " + countD);
        System.out.println("b. Number of text files: " + tFiles.size());
        System.out.println("   List of full paths to text files:");
        tPath.forEach(System.out::println);
        System.out.println("c. Number of binary files: " + bFiles.size());
        System.out.println("   List of full paths to binary files:");
        bPath.forEach(System.out::println);
        System.out.println("d. Smallest text file: " + sTextFile + " (" + sTSize + " bytes)");
//        System.out.println("   Largest text file: " + largestTextFile + " (" + largestTextFileSize +
        System.out.println("   Largest text file: " + lTextFile + " (" + lTSize + " bytes)");
        System.out.println("e. Smallest binary file: " + sBinaryFile + " (" + sBSize + " bytes)");
        System.out.println("   Largest binary file: " + "(" + lBSize + " bytes) " + lBinaryFile  );
        System.out.println("f. Number of invalid references: " + countIR);
        System.out.println("g. Number of external references: " + countER);
    }





}


