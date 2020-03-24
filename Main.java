

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Main extends Application
{
    String ip_string = "192.168.1.188";
    ReadMsg rm;
    Socket socket;
    ObjectOutputStream oos;
    ObjectInputStream ois;

    public Color[] colors = {Color.BLUE, Color.GREEN, Color.CRIMSON};
    public int color_code = 0;
    public boolean connected = false;
    ArrayList<Point> points = new ArrayList<>();

    public int counter = 0;
    VBox root = new VBox();
    Canvas canvas = new Canvas(800, 800);
    Button clear = new Button("CLEAR");
    Button color = new Button("CHANGE COLOR");
    Button connect = new Button("CONNECT");
    TextField ip = new TextField();

    GraphicsContext gc;
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        gc = canvas.getGraphicsContext2D();
        primaryStage.setTitle("ONLINE DRAW");
        primaryStage.setScene(new Scene(root, 800, 900));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e ->
        {
            try
            {
                if(socket != null)
                {
                    if(socket.isConnected())
                    {
                        oos.close();
                        ois.close();
                        socket.close();
                    }
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });
        init1();
        primaryStage.show();
    }

    public void init1() throws IOException
    {
        root.getChildren().addAll(canvas, clear, color, ip, connect);
        clear.setOnAction(e -> clear());
        clear.setMinWidth(800);
        color.setOnAction(e -> color());
        color.setMinWidth(800);
        connect.setOnAction(e ->
        {
            try
            {
                connect();
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        });
        color.setBackground(new Background(new BackgroundFill(colors[color_code], CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void clear()
    {
        draw(null, true);
    }

    public void connect() throws IOException
    {
        if(!connected)
        {
            connect.setText("DISCONNECT");
            connected = true;
            ip_string = ip.getText();
            ip.setDisable(true);
            socket = new Socket(ip_string, 7778);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,
                    event ->
                    {
                        if(connected)
                        {
                            if (event.isAltDown())
                            {
                                if(points.size() > 0)
                                {
                                    if(Math.abs(points.get(points.size() - 1).getX() - event.getX()) > 0.1 || Math.abs(points.get(points.size() - 1).getY() - event.getY()) > 0.1)
                                    {
                                        points.add(new Point(event.getX(), event.getY()));
                                    }
                                }
                                else
                                {
                                    points.add(new Point(event.getX(), event.getY()));
                                }

                                if(points.size() > 10)
                                {
                                    GRAPHICS msg = new GRAPHICS(color_code, (ArrayList<Point>) points.clone());
                                    try {
                                        oos.writeObject(msg);
                                        oos.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    points.clear();
                                }
                            }
                            else
                            {
                                points.clear();
                            }
                        }

                    });
            rm = new ReadMsg();
            rm.start();

        }
        else
        {

            rm = null;
            ois.close();
            oos.close();
            socket.close();
            connect.setText("CONNECT");
            connected = false;
            ip.setDisable(false);

        }
    }

    public void color()
    {
        if(color_code < colors.length - 1)
        {
            color_code++;
            color.setBackground(new Background(new BackgroundFill(colors[color_code], CornerRadii.EMPTY, Insets.EMPTY)));
        }
        else
        {
            color_code = 0;
            color.setBackground(new Background(new BackgroundFill(colors[color_code], CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }

    public synchronized void draw(GRAPHICS g, boolean clear )
    {
        if(!clear)
        {
            gc.setStroke(colors[g.color]);

            for(int i = 0; i < g.points.size() - 1; i++)
            {
                gc.strokeLine(g.points.get(i).getX(), g.points.get(i).getY(), g.points.get(i + 1).getX(), g.points.get(i + 1).getY());

            }

        }
        else
        {
            gc.clearRect(0, 0, 800, 800);
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    private class ReadMsg extends Thread
    {
        @Override
        public void run()
        {

            GRAPHICS str;
            try
            {
                while (true)
                {
                    str = (GRAPHICS) ois.readObject();
                    GRAPHICS g = str;
                    Platform.runLater(() ->
                    {
                        draw(g, false);
                    });

                }
            } catch (IOException | ClassNotFoundException e)
            {

            }
        }
    }



}
