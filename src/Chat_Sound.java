import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;

import static java.lang.Thread.sleep;

public class Chat_Sound extends JFrame {
    private final JButton recordBtn;
    private final JButton stopBtn;
    private final JButton sendBtn;
    private final JButton playReceived;
    private final JLabel recordingStatus;
    private final JTextArea messageArea;
    private final File wavFile = new File("recording.wav");
    private SoundRecorder newRecording;
    private final Socket socket;
    private String newFilePath;

    // Konstruktor för klassen Chat_Sound - GUI
    public Chat_Sound() throws IOException {

        recordingStatus = new JLabel("Recording: Waiting..");
        recordingStatus.setBounds(10, 340, 200, 20);

        messageArea = new JTextArea();
        messageArea.setBounds(10, 30, 365, 200);
        messageArea.setEditable(false);

        recordBtn = new JButton("Record");
        recordBtn.setBounds(10, 250, 100, 20);
        recordBtn.addActionListener(this::recordListner);


        stopBtn = new JButton("Stop");
        stopBtn.setBounds(10, 280, 100, 20);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(this::stopListner);


        sendBtn = new JButton("Send");
        sendBtn.setBounds(10, 310, 100, 20);
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(this::sendListner);

        playReceived = new JButton("Play");
        playReceived.setBounds(210, 250, 100, 20);
        playReceived.setEnabled(false);
        playReceived.addActionListener(this::playReceivedListner);
        add(recordBtn);
        add(stopBtn);
        add(sendBtn);
        add(recordingStatus);
        add(messageArea);
        add(playReceived);
        this.setSize(400, 400);
        this.setLayout(null);
        this.setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);


        // Portnummer och adress
        int port = 4848;
        String address = "atlas.dsv.su.se";


        // Skapar en ny socket mot "atlas.dsv.su.se" och porten 4848
        socket = new Socket(address, port);


        setTitle("Connected.. ");
        new SoundReceiver(messageArea, socket, playReceived, this);
    }


    // Sträng som uppdaterar filepathen
    public synchronized void setFilePath(String filePath) {
        newFilePath = filePath;
    }


    /* Lyssnare för "Record"-knappen.
     *
     *  Skapar ett nytt objekt av klassen SoundRecorder.
     *
     *  Skapar sen för en ny tråd som startar SoundRecorder.
     * */
    public void recordListner(ActionEvent e) {
        stopBtn.setEnabled(true);
        recordBtn.setEnabled(false);
        newRecording = new SoundRecorder();
        Thread recordThread = new Thread(() -> {
            try {
                System.out.println("Started recording");

                newRecording.start();
            } catch (LineUnavailableException lue) {
                lue.printStackTrace();
            }
        });
        recordThread.start();

        recordingStatus.setText("Recording: Recording");
    }

    /* Lyssnare for "Stop"-knappen
    *  Stoppar inspelningen och sparar wavfilen*/
    public void stopListner(ActionEvent e) {

        try {
            newRecording.stop();
            newRecording.save(wavFile);
            System.out.println("Stopped");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        stopBtn.setEnabled(false);
        recordingStatus.setText("Recording: Stopped");
        sendBtn.setEnabled(true);
    }


    /* Lyssnare för "Send"-knappen
    *
    *  Skapar ett nytt objekt av klassen SoundSender
    *  */
    public void sendListner(ActionEvent e) {
        sendBtn.setEnabled(false);
        new SoundSender(wavFile, socket, this);

        recordingStatus.setText("Recording: Sent");
        reset();
    }


    /* Lyssnare för "Play Recording -knappen
    *
    *  Skapar ett nytt objekt av klassen SoundPlayer
    *  */
    public void playReceivedListner(ActionEvent e) {
        new SoundPlayer(newFilePath);
    }


    // Återställer alla knappar
    public void reset() {
        recordBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        recordingStatus.setText("Recording: Waiting..");
    }

    // Metod som meddelar användaren om skickningen har misslyckats eller inte
    public void addSentMsg(String msg) {
        messageArea.append(msg + "\n");
    }

    // Main
    public static void main(String[] args) throws IOException {
        new Chat_Sound();
    }



}

class SoundSender {
    private final File voiceMsg;
    private final Socket socket;
    private ObjectOutputStream out;
    private Chat_Sound chat_sound;


    // Konstruktor för klassen SoundSender
    public SoundSender(File voiceMsg, Socket socket, Chat_Sound chat_sound) {
        this.voiceMsg = voiceMsg;
        this.socket = socket;
        this.chat_sound = chat_sound;
        send();
    }


    /* Metod som skapar en outputstream, läsen ljudfilen
    * som bytes och skapar ett Storage-objekt som skrivs
    * till outputstreamen */
    public void send() {
        try {
            OutputStream outputStream = socket.getOutputStream();
            out = new ObjectOutputStream(outputStream);


            byte[] data = Files.readAllBytes(voiceMsg.toPath());
            String msg = "musicmessage";
            Storage message = new Storage(data, msg);
            out.writeObject(message);
            out.reset();
            chat_sound.addSentMsg("Message sent.");

        } catch (IOException e) {
            e.printStackTrace();
            chat_sound.addSentMsg("Failed to send message.");
        }
    }
}

class SoundReceiver extends Thread {
    private final Socket socket;
    private final JTextArea messageArea;
    private ObjectInputStream in;
    private final JButton playButton;
    private final Chat_Sound chat;

    //Konstruktor för klassen SoundReceiver
    public SoundReceiver(JTextArea messageArea, Socket socket, JButton playButton, Chat_Sound chat) {
        this.messageArea = messageArea;
        this.socket = socket;
        this.playButton = playButton;
        this.chat = chat;
        start();
    }

    /* Trådens run-metod
    *
    * Skapar en objektinputstream mot socketen,
    * läsen sedan det objektet som skickats och
    * skapar ett Storage-objekt av det.
    *
    * Hämtar Storage-objektets array av data
    * och eventuellt meddelande.
    *
    * Skapar en ny fl av objektet.
    *
    * Skriver sen objektetsdata till den nya filen*/
    public void run() {
        try {
            while (true) {
                in = new ObjectInputStream(socket.getInputStream());
                Storage storage = (Storage) in.readObject();



                byte[] data = storage.getData();
                String msg = storage.getId();


                File newFile = new File(msg + ".wav");
                chat.setFilePath(newFile.getPath());

                FileOutputStream fos = new FileOutputStream(newFile);

                fos.write(data);
                playButton.setEnabled(true);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}

class SoundRecorder {
    private ByteArrayOutputStream recordedBytes;
    private TargetDataLine line;
    private AudioFormat audioFormat;
    private boolean recording;


    /* Metod som skapar och returnerar ett ljudformat */
    AudioFormat getAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0F, 16, 1, 2, 8000.0F, false);
    }

    /* Metod som hämtar ett audioformat. Skapar ett DataLine-objekt,
    * med audioformatet. Öppnar sen DataLine och startar (Spelar nu in)
    *
    * Skapar en ByteArrayOutputStream för de inspelade bytesen.
    *
    * Skriver bytesen till ByteArrayOutputStreamen sålänge
    * inspelningen inte har stoppats. */
    public void start() throws LineUnavailableException {
        audioFormat = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Format not supported");
        }


        line = AudioSystem.getTargetDataLine(audioFormat);
        line.open();

        line.start();
        byte[] buffer = new byte[4096];
        int byteRead = 0;
        recordedBytes = new ByteArrayOutputStream();
        recording = true;

        while (recording) {
            byteRead = line.read(buffer, 0, buffer.length);
            recordedBytes.write(buffer, 0, byteRead);
        }
        line.drain();
        line.close();
    }

    public void stop() throws IOException {
        recording = false;


    }
    /* Metod som sparar filen som har spelats in som en ny fil*/
    public void save(File wavFile) throws IOException {
        byte[] audioData = recordedBytes.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = new AudioInputStream(inputStream, audioFormat, audioData.length / audioFormat.getFrameSize());

        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
        audioInputStream.close();
        recordedBytes.close();
    }

}


class SoundPlayer {
    private final String path;


    //Konstruktor för klassen SoundPlayer
    public SoundPlayer(String newFilePath) {
        this.path = newFilePath;
        play();
    }


    public void play() {
        try {
            // Skapar ett AudioInputStream med den mottagna filen.
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(path));
            // Hämtar vårt audioformat
            AudioFormat audioFormat = audioIn.getFormat();


            SourceDataLine line = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);


            try {
                // Skapar ett SourceDataLine objekt med vår dataLine
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat);
                line.start();
            } catch (LineUnavailableException le) {
                le.printStackTrace();
            }


            /* Läser data från vårt audioSystem
            *  och skriver dessa till våran DataLine
            *  till att det itne finns något mer att läsa
            *  (-1)*/
            int bytesRead = 0;
            byte[] abData = new byte[12800];
            while (bytesRead != -1) {
                try {
                    bytesRead = audioIn.read(abData, 0, abData.length);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                if (bytesRead >= 0) {
                    int nBytesWritten = line.write(abData, 0, bytesRead);
                }
            }

            line.drain();

            // Stänger dataline
            line.close();

        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}