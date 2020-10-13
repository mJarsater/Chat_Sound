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
    private JButton recordBtn, stopBtn, sendBtn
            , playReceived;
    private JLabel recordingStatus;
    private JTextArea messageArea;
    private File wavFile = new File("recording.wav");
    private SoundRecorder newRecording;
    private Socket socket;
    private String newFilePath;

    public Chat_Sound() throws IOException {

        recordingStatus = new JLabel("Recording: Waiting..");
        recordingStatus.setBounds(10, 340, 200, 20);

        messageArea = new JTextArea();
        messageArea.setBounds(10, 30, 365, 200);
        messageArea.setEditable(false);

        recordBtn = new JButton("Record");
        recordBtn.setBounds(10, 250, 100, 20);
        recordBtn.addActionListener(this:: recordListner);


        stopBtn = new JButton("Stop");
        stopBtn.setBounds(10, 280, 100, 20);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(this:: stopListner);


        sendBtn = new JButton("Send");
        sendBtn.setBounds(10, 310, 100, 20);
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(this:: sendListner);

        playReceived = new JButton("Play");
        playReceived.setBounds(210, 250, 100, 20);
        playReceived.setEnabled(false);
        playReceived.addActionListener(this:: playReceivedListner);
        add(recordBtn);add(stopBtn);add(sendBtn);add(recordingStatus);
        add(messageArea);add(playReceived);
        this.setSize(400,400);
        this.setLayout(null);
        this.setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        int port = 4848;
        String address = "atlas.dsv.su.se";
        socket = new Socket(address, port);
        setTitle("Connected.. ");
        new SoundReceiver(messageArea, socket, playReceived, this);
    }

    public synchronized void setFilePath(String filePath){
        newFilePath = filePath;
    }

    public void recordListner(ActionEvent e){

        stopBtn.setEnabled(true);
        recordBtn.setEnabled(false);
        newRecording = new SoundRecorder();
        Thread recordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Started recording");

                    newRecording.start();
                }catch (LineUnavailableException lue){
                    lue.printStackTrace();
                }
                }
        });


        recordThread.start();

        recordingStatus.setText("Recording: Recording");

    }

    public void stopListner(ActionEvent e){

        try{
            newRecording.stop();
            newRecording.save(wavFile);
            System.out.println("Stopped");
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        stopBtn.setEnabled(false);


        recordingStatus.setText("Recording: Stopped");
        sendBtn.setEnabled(true);
    }

    public void sendListner(ActionEvent e){
        sendBtn.setEnabled(false);
        addSentMsg();
        SoundSender send = new SoundSender(wavFile,socket);

        recordingStatus.setText("Recording: Sent");
        reset();
    }

    public void playReceivedListner(ActionEvent e){
        new SoundPlayer(newFilePath);
    }

    public void reset(){
        recordBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        recordingStatus.setText("Recording: Waiting..");
    }

    public static void main(String[] args) throws IOException {
        Chat_Sound chatSound = new Chat_Sound();
    }

    public void addSentMsg(){
        messageArea.append("Message sent" + "\n");
    }

}

class SoundSender{
    private File voiceMsg;
    private Socket socket;
    private ObjectOutputStream out;

    public SoundSender(File voiceMsg, Socket socket){
        this.voiceMsg = voiceMsg;
        this.socket = socket;
        send();
    }
    public void send(){
        try {
            OutputStream outputStream = socket.getOutputStream();
            out = new ObjectOutputStream(outputStream);


            byte[] data = Files.readAllBytes(voiceMsg.toPath());
            String msg = "musicmessage";
            Storage message = new Storage(data, msg);
            out.writeObject(message);
            out.reset();
            System.out.println("Message SENT");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class SoundReceiver extends Thread{
    private Socket socket;
    private JTextArea messageArea;
    private ObjectInputStream in;
    private JButton playButton;
    private Chat_Sound chat;

    public SoundReceiver(JTextArea messageArea, Socket socket, JButton playButton, Chat_Sound chat) {
        this.messageArea = messageArea;
        this.socket = socket;
        this.playButton = playButton;
        this.chat = chat;
        start();
    }
    public void run(){
        try{
            while(true){
                in = new ObjectInputStream(socket.getInputStream());
                Storage storage = (Storage) in.readObject();
                System.out.println(storage.getData().length);
                byte[] data = storage.getData();
                String msg = storage.getId();
                File newFile = new File(msg+".wav");
                System.out.println(newFile.toPath());
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

    AudioFormat getAudioFormat(){
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0F, 16,1,2, 8000.0F, false);
    }

    public void start() throws LineUnavailableException {
        audioFormat = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

        if(!AudioSystem.isLineSupported(info)){
            System.out.println("Format not supported");
        }


        line = AudioSystem.getTargetDataLine(audioFormat);
        line.open();

        line.start();
        byte[]buffer = new byte[4096];
        int byteRead = 0;
        recordedBytes = new ByteArrayOutputStream();
        recording = true;

        while(recording){
            byteRead = line.read(buffer,0, buffer.length);
            recordedBytes.write(buffer, 0, byteRead);
        }
        line.drain();
        line.close();
    }

    public void stop() throws IOException{
        recording = false;



    }

    public void save(File wavFile) throws  IOException{
        System.out.println("Saving");
        byte[] audioData = recordedBytes.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = new AudioInputStream(inputStream, audioFormat, audioData.length/ audioFormat.getFrameSize());

        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
        audioInputStream.close();
        recordedBytes.close();
    }

}


class SoundPlayer{
    private String path;

    public SoundPlayer(String newFilePath){
        this.path = newFilePath;
        play();
    }

    public void play( ){

        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(path));
            AudioFormat audioFormat = audioIn.getFormat();
            SourceDataLine line = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);


            try{
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat);
                line.start();
            }  catch (LineUnavailableException le){
                le.printStackTrace();
            }

            int bytesRead = 0;
            byte[] abData = new byte[12800];
            while(bytesRead != -1){
                try{
                    bytesRead = audioIn.read(abData, 0, abData.length);
                } catch (IOException ioe){
                    ioe.printStackTrace();
                }
                if(bytesRead >= 0) {
                    int nBytesWritten = line.write(abData, 0, bytesRead);
                }
            }
            line.drain();
            line.close();




            System.out.println("Plaing...");
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}