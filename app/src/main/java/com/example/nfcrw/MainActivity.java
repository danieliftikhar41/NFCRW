package com.example.nfcrw;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {


    public static final String error_detected = "No se detectó ninguna NFC Tag";
    public static final String write_success = "Se completó correctamente la escritura.";
    public static final String write_error = "Error al escribir los datos";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;
    TextView edit_message;
    TextView nfc_contents;
    Button ActivateButton;
    Button ReadButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit_message = (TextView) findViewById(R.id.edit_message);
        nfc_contents = (TextView) findViewById(R.id.nfc_contents);
        ActivateButton = (Button) findViewById(R.id.ActivateButton);
        context=this;
        ActivateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myTag== null) {
                    Toast.makeText(context, error_detected, Toast.LENGTH_LONG).show();
                }
                else {
                    String data = edit_message.getText().toString();
                    writeTagNFCv(myTag,data);
                    Toast.makeText(context,write_success, Toast.LENGTH_LONG).show();
                }
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            Toast.makeText(this,"Este dispositivo no soporta la tecnología NFC",Toast.LENGTH_SHORT).show();
            finish();
        }
        readfromintent(getIntent());

        pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[] { tagDetected };

    }


    public static byte getByte(byte[] input, int key){
        try {
            return input[key];
        } catch (Exception e){
            return (byte)0x00;
        }
    }

    public String printHex(String input){
        return input;
    }
    private void readfromintent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)||NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)||NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if ( rawMsgs != null ) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }
    @SuppressLint("SetTextI18n")
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;
        String text = "";



        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLenght = payload[0] & 0077;


        try {
            text = new String(payload,languageCodeLenght + 1 , payload.length - languageCodeLenght - 1,textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }
        nfc_contents.setText("NFC Content:" + text);
    }


    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02X", b);
        }
        formatter.close();
        return sb.toString();
    }

    public static void readTagNFCv (Tag currentTag, TextView nfc_contents){

        System.out.println("currentTag" + currentTag);
        NfcV nfcvTag = NfcV.get(currentTag);
        System.out.println("MYTAG ES :" + nfcvTag);
        int numberOfBlocks = 1;
        StringBuilder fullData = new StringBuilder();
        StringBuilder utf8String = new StringBuilder();
        StringBuilder blocksData = new StringBuilder();
        while(numberOfBlocks < 24)
        {
            try {
                nfcvTag.connect();
            } catch (IOException e) {
                System.out.println("Error reading");
                return;
            }
            try {
//
                byte[] tagUid = currentTag.getId();
//
                byte[] cmd = new byte[] {
                        (byte)0x20,
                        (byte)0x20,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        (byte)(numberOfBlocks & 0x0ff)
                };
                System.arraycopy(tagUid, 0, cmd, 2, 8);
                byte[] response = nfcvTag.transceive(cmd);
                String data =  bytesToHex(response).substring(2);
                String utf8 = new String(response , StandardCharsets.UTF_8);
                blocksData.append(data.replaceAll(" " , ""));
                fullData.append(data.replaceAll(" " , ""));
                utf8String.append(utf8);
                nfcvTag.close();

                System.out.println(blocksData);
                System.out.println(fullData);
                System.out.println(utf8String);
                numberOfBlocks = numberOfBlocks + 1;

            } catch (IOException e) {
                System.out.println("Error reading");
                return;
            }
        }
        nfc_contents.setText(utf8String);
    }
    public void writeTagNFCv(Tag tag, String data) {

        NfcV myTag = NfcV.get(tag);
        try {
            myTag.connect();
            if (myTag.isConnected()) {
                int c = 1;
                byte[] info = data.getBytes();
                int dataLength = info.length;
                if (data.length()/4 <= 8192){
                    byte[] args = new byte[15];
                    args[0] = 0x20;
                    args[1] = 0x21;
                    byte[] id = tag.getId();
                    for (int o=0; o<8; o++)
                        args[o+2] = id[o];
                    for (int i = 0; i<64; i++) {
                        System.out.println(c);
                        args[10] = (byte) c;
                        args[11] = 0x00;
                        args[12] = 0x00;
                        args[13] = 0x00;
                        args[14] = 0x00;
                        c++;
                        byte[] out = myTag.transceive(args);
                        String out2 = bytesToHex(out);
                        System.out.println(printHex(out2));
                    }
                    c = 1;
                    for (int i = 0; i<=dataLength/4; i++) {
                        System.out.println(c);
                        args[10] = (byte)c;
                        args[11] = getByte(info, (i*4)+0);
                        args[12] = getByte(info, (i*4)+1);
                        args[13] = getByte(info, (i*4)+2);
                        args[14] = getByte(info, (i*4)+3);
                        c++;
                        byte[] out = myTag.transceive(args);
                        String out2 = bytesToHex(out);
                        System.out.println(printHex(out2));
                    }
                }

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (myTag != null) {
                try {
                    myTag.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        readfromintent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            readTagNFCv(myTag,nfc_contents);
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,writingTagFilters,null);
    }

    private void WriteModeOff() {
        writeMode= false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}