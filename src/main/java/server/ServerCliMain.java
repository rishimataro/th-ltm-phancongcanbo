package server;

public class ServerCliMain {

    public static void main(String[] args) throws Exception {
        int port = 8888;
        if (args != null && args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        ServerApp app = new ServerApp(System.out::println);
        app.start(port);
        System.out.println("Server CLI dang chay. Nhan Ctrl+C de dung.");
        Thread.currentThread().join();
    }
}
