package com.example.nfcrw;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Formatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.util.Arrays;

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
                        writeTag(myTag,data);
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
    public String printByte(byte[] input){
        try {
            return new String(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
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

    private byte[] convertHexToByte(String str) {
        byte[] ans = new byte[str.length() / 2];

        System.out.println("Hex String : " + str);

        for (int i = 0; i < ans.length; i++) {
            int index = i * 2;

            // Using parseInt() method of Integer class
            int val = Integer.parseInt(str.substring(index, index + 2), 16);
            ans[i] = (byte) val;
        }

        // Printing the required Byte Array
        System.out.print("Byte Array : ");
        for (int i = 0; i < ans.length; i++) {
            System.out.print(ans[i] + " ");
        }
        return ans;
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

    public String join(String[] input, String delim) {
        String output = "";
        if (input.length > 0)
            output += input[0];
        if (input.length > 1)
            for (int i = 1; i < input.length; i++)
                output += delim + input[i];
        return output;
    }
    public void writeTag(Tag tag, String data) {
        NfcV myTag = NfcV.get(tag);
        int startByte = 0;
            try {
                myTag.connect();
                if (myTag.isConnected()) {
                    byte[] info = data.getBytes();
                    System.out.println("INFO" + Arrays.toString(info));
                    int dataLength = info.length;
                    if (data.length()/4 <= 64){
                        byte[] args = new byte[15];
                        args[0] = 0x20;
                        args[1] = 0x21;
                        byte[] id = tag.getId();
                        for (int o=0; o<8; o++)
                            args[o+2] = id[o];
                        for (int i = 0; i<64; i++) {
                            args[10] = (byte) i;
                            args[11] = 0x00;
                            args[12] = 0x00;
                            args[13] = 0x00;
                            args[14] = 0x00;
                            byte[] out = myTag.transceive(args);
                            String out2 = bytesToHex(out);
                            System.out.println("1:.. " + printHex(out2));
                        }
                        for (int i = 0; i<=dataLength/4; i++) {
                            args[10] = (byte) i;
                            args[11] = getByte(info, (i*4)+0);
                            args[12] = getByte(info, (i*4)+1);
                            args[13] = getByte(info, (i*4)+2);
                            args[14] = getByte(info, (i*4)+3);
                            byte[] out = myTag.transceive(args);
                            String out2 = bytesToHex(out);
                            System.out.println("2:.. " + printHex(out2));
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

    private void write(String text, Tag tag) throws IOException,FormatException {

        NfcV nfcV = NfcV.get(tag);
//        nfcV.connect();
//
//        String dataString = "12";
//        int offset = 0;  // offset of first block to read
//        int blocks = 8;  // number of blocks to read
//        byte[] data = convertHexToByte((dataString));
//        byte[] cmd = new byte[] {
//                (byte)0x60, // FLAGS
//                (byte)0x21, // WRITE SINGLE COMMAND
//                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // UID
//                (byte)0x00, // OFFSET
//                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00  //DATA
//        };
//        System.arraycopy(tag.getId(), 0, cmd, 2, 8);
//        for (int i = 0; i < blocks; ++i) {
//            cmd[10] = (byte)((offset + i) & 0x0ff);
//            System.arraycopy(data, 4 * i, cmd, 11, 4);
//
//            byte[] response = nfcV.transceive(cmd);
//        }
        byte[] id = tag.getId();

        if (nfcV != null) {

            byte[] infoCmd = new byte[2 + id.length];
            // set "addressed" flag
            infoCmd[0] = 0x20;
            // ISO 15693 Get System Information command byte
            infoCmd[1] = 0x2B;
            //adding the tag id
            System.arraycopy(id, 0, infoCmd, 2, id.length);

            int memoryBlocks = 0;
            try {
                nfcV.connect();
                byte[] data = nfcV.transceive(infoCmd);

                memoryBlocks = Integer.parseInt(String.format("%02X", data[data.length - 3]), 16);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    nfcV.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

//        byte[] cmdInfo = new byte[]{
//                (byte)0x20,
//                (byte)0x21,
//                (byte)0x00,
//                (byte)0xFE,
//                (byte)0xFE,
//                (byte)0xFE,
//                (byte)0xFA,
//                (byte)0xFE,
//                (byte)0xFA,
//                (byte)0xFE,
//        };

//
//        System.arraycopy(tag.getId(), 0, cmdInfo, 2, 8);
//            byte[] answer = new byte[0];
//            try {
//                answer = nfcV.transceive(cmd);
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.d("HA OCURRIDO UN ERROR EN EL ANSWER", "hola");
//            }
//            System.out.println("INFORMACION DEL CHIP" + Arrays.toString(answer));
//        NdefRecord[] records = {createRecord(text)};
//        NdefMessage message = new NdefMessage(records);
//        Ndef ndef = Ndef.get(tag);
//        ndef.connect();
//        ndef.writeNdefMessage(message);
//        ndef.close();
        }
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = text.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes,0,payload,1, langLength);
        System.arraycopy(textBytes,0,payload,1+langLength,textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readfromintent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
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


    private void getInfo() {

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
