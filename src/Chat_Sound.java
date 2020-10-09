import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static java.lang.Thread.sleep;

public class Chat_Sound extends JFrame {
    private JButton recordBtn, stopBtn, sendBtn;
    private JLabel recordingStatus;
    private JTextArea messageArea;
    private File wavFile = new File("C:\\Users\\Marti\\Music\\Record.wav");
    private SoundRecorder newRecording;

    public Chat_Sound(){
        recordingStatus = new JLabel("Recording: Waiting..");
        recordingStatus.setBounds(10, 380, 200, 20);

        messageArea = new JTextArea();
        messageArea.setBounds(10, 30, 365, 200);
        messageArea.setEditable(false);

        recordBtn = new JButton("Record");
        recordBtn.setBounds(10, 420, 100, 20);
        recordBtn.addActionListener(this:: recordListner);


        stopBtn = new JButton("Stop");
        stopBtn.setBounds(150, 420, 100, 20);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(this:: stopListner);


        sendBtn = new JButton("Send");
        sendBtn.setBounds(270, 420, 100, 20);
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(this:: sendListner);



        add(recordBtn);add(stopBtn);add(sendBtn);add(recordingStatus);
        add(messageArea);
        this.setSize(400,500);
        this.setLayout(null);
        this.setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);


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
        //Start recording

        recordingStatus.setText("Recording: Sent");
        reset();
    }

    public void reset(){

        recordBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        recordingStatus.setText("Recording: Waiting..");
    }

    public static void main(String[] args) {
        Chat_Sound chatSound = new Chat_Sound();
    }

    public void addSentMsg(){
        messageArea.append("Message sent" + "\n");
    }

}

class SoundSender{

}

class SoundReceiver{

}

class SoundRecorder {
    private ByteArrayOutputStream recordedBytes;
    private TargetDataLine line;
    private AudioFormat audioFormat;
    private boolean recording;

    AudioFormat getAudioFormat(){
        float sampleRate = 16000;
        int sampleSizeInBits =  8;
        int channels = 2;

        return new AudioFormat(sampleRate,sampleSizeInBits,channels, true, true);
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
