package com.david.bit_biosignals;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.bitalino.comm.BITalinoDevice;
import com.bitalino.comm.BITalinoFrame;
import com.bitalino.util.SensorDataConverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";              // tag for activity

    private final String s_serverAddress = "192.168.0.99";         // server address
    private final int i_serverPort = 8085;                         // server port
    private static final UUID MY_UUID = UUID                       // default UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice bt_device = null;                      // remote bluetooth device
    private BluetoothSocket bt_socket = null;                      // bluetooth socket
    private BITalinoDevice bitalino;                               // bitalino object
    private BluetoothAdapter bt_adapter;                           // bluetooth adapter
    private BITalinoFrame[] frames;                                // array of bitalino frames

    private Socket ip_socket = null;                               // ip socket
    private OutputStream ip_os = null;                             // ip socket output stream
    private FileInputStream fis = null;                            // file input stream
    private BufferedInputStream bis = null;                        // buffered input stream

    // file objects to store data
    private File file_ECG = null;
    private File file_EMG = null;
    private File file_EDA = null;

    // strings to store user entered data
    private String s_id = null;
    private String s_forename = null;
    private String s_surname = null;
    private String s_dob = null;
    private String s_recordTime = null;
    private String s_deviceMAC = null;
    // string for patient info separated by _
    private String s_patient = null;
    // string for date and time
    private String s_dateTime = null;
    // string for file names when sending to server
    private String s_fileName = null;

    // text view objects
    private TextView textView_logs = null;
    // edit text objects
    private EditText editText_id = null;
    private EditText editText_forename = null;
    private EditText editText_surname = null;
    private EditText editText_dob = null;
    private EditText editText_recordTime = null;
    private EditText editText_deviceMAC = null;
    // checkbox objects
    private CheckBox checkBox_recordECG = null;
    private CheckBox checkBox_recordEMG = null;
    private CheckBox checkBox_recordEDA = null;
    private CheckBox checkBox_dataRecorded = null;
    // button objects
    private Button button_connect = null;
    private Button button_disconnect = null;
    private Button button_start = null;
    private Button button_clear = null;
    private Button button_save = null;

    // called when the app is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference text view objects
        textView_logs = (TextView) findViewById(R.id.textView_logs);
        // reference edit text objects
        editText_id         = (EditText) findViewById(R.id.editText_id);
        editText_forename   = (EditText) findViewById(R.id.editText_forename);
        editText_surname    = (EditText) findViewById(R.id.editText_surname);
        editText_dob        = (EditText) findViewById(R.id.editText_dob);
        editText_recordTime = (EditText) findViewById(R.id.editText_recordTime);
        editText_deviceMAC  = (EditText) findViewById(R.id.editText_deviceMAC);
        // reference checkbox objects
        checkBox_recordECG = (CheckBox) findViewById(R.id.checkBox_recordECG);
        checkBox_recordEMG = (CheckBox) findViewById(R.id.checkBox_reacordEMG);
        checkBox_recordEDA = (CheckBox) findViewById(R.id.checkBox_recordEDA);
        checkBox_dataRecorded = (CheckBox) findViewById(R.id.checkBox_dataRecorded);
        // reference to button objects
        button_connect = (Button) findViewById(R.id.button_connect);
        button_disconnect = (Button) findViewById(R.id.button_disconnect);
        button_start = (Button) findViewById(R.id.button_start);
        button_clear = (Button) findViewById(R.id.button_clear);
        button_save = (Button) findViewById(R.id.button_save);

        try{
            // instantiate bitalino object reading the three ports
            bitalino = new BITalinoDevice(1000, new int[]{0, 1, 2});
        } catch (Exception e) {
            //Log.e(TAG, "There was an error instantiating device.", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // function to handle connect button presses
    public void connect(View view) {
        // get MAC address form text field
        s_deviceMAC = editText_deviceMAC.getText().toString();

        textView_logs.setText("Connecting to Bitalino " + s_deviceMAC + "...\n");

        // check user has entered Bitalino MAC address
        if (s_deviceMAC == null || s_deviceMAC.isEmpty()) {
            textView_logs.append("Please enter Bitalino MAC address.\n");
        }
        else {
            // start connect bitalino async task
            ConnectBitalino connectBitalino = new ConnectBitalino();
            connectBitalino.execute();
        }
    }

    // as network operations are not able to be executed on the main UI thread, they must be contained in a async task
    class ConnectBitalino extends AsyncTask<Void, Void, Void> {
        // boolean for error
        private Boolean b_error = false;
        @Override
        protected Void doInBackground(Void... Params) {
            // set error false
            b_error = false;
            // try and connect to bitalino via bluetooth
            try{
                // get bluetooth adapter
                bt_adapter = BluetoothAdapter.getDefaultAdapter();
                // get remote bitalino (connected bia bluetooth)
                bt_device = bt_adapter.getRemoteDevice(s_deviceMAC);
                // stop discovery
                bt_adapter.cancelDiscovery();
                // connect to bitalino and create RFCOMM socket
                bt_socket = bt_device.createRfcommSocketToServiceRecord(MY_UUID);
                // if successful, connect
                bt_socket.connect();
            } catch (Exception e) {
                // if error set bool to true
                b_error = true;
            }

            return null;
        }

        //executed when the async task has completed
        @Override
        protected void onPostExecute(Void result) {
            // if no error
            if (b_error == false) {
                textView_logs.append("Successfully connected to Bitalino " + s_deviceMAC + ".\n");
            }
            else {
                textView_logs.append("There was an error connecting to Bitalino, make sure MAC address is correct, Bitalino is turned ON and paired via bluetooth.\nIf persistent please reset Bitalino.");
            }
        }
    }

    // function to handle disconnect button presses
    public void disconnect(View view) {
        textView_logs.setText("Attempting to disconnect Bitalino...\n");
        // try and disconnect in case of error or timeout
        try {
            bt_socket.close();
            bt_socket = null;
            bt_device = null;
            bt_adapter = null;
            textView_logs.append("Connection reset.\n");
        } catch (Exception e) {
            textView_logs.append("There was an error disconnecting Bitalino, probably already disconnected.\n");
        }

    }

    // function to handle start button presses
    public void start(View view) {
        textView_logs.setText("Starting data acquisition...\n");

        // get data from user entry and store in strings
        s_id = editText_id.getText().toString();
        s_forename = editText_forename.getText().toString();
        s_surname = editText_surname.getText().toString();
        s_dob = editText_dob.getText().toString();
        s_recordTime = editText_recordTime.getText().toString();
        // set patient string as id_forename_surname_dob
        s_patient = s_id + "_" + s_forename + "_" + s_surname + "_" + s_dob;

        // get date and time as string dd:mm:yyyy_hh:mm
        Calendar c = Calendar.getInstance();
        int i_date = c.get(Calendar.DATE);
        int i_month = c.get(Calendar.MONTH) + 1;
        int i_year = c.get(Calendar.YEAR);
        int i_hour = c.get(Calendar.HOUR_OF_DAY);
        int i_min = c.get(Calendar.MINUTE);
        s_dateTime = i_date + ":" + i_month + ":" + i_year + "_" + i_hour + ":" + i_min;

        // set string for the name of the data file of the patient
        s_fileName = s_patient + "_" + s_dateTime + "_" + s_recordTime;

        // check user has entered patient info
        if ((s_id == null || s_id.isEmpty()) || (s_forename == null || s_forename.isEmpty()) || (s_surname == null || s_surname.isEmpty()) || (s_dob == null || s_dob.isEmpty())) {
            textView_logs.append("Please complete entry for user information before gathering data.\n");
        }
        // check the user has entered record time
        else if (s_recordTime == null || s_recordTime.isEmpty()) {
            // if they have not tell them to enter a time
            textView_logs.append("Please enter a time to record data for in seconds.\n");
        }
        // check user has selected which sensor to record
        else if (!checkBox_recordECG.isChecked() && !checkBox_recordEMG.isChecked() && !checkBox_recordEDA.isChecked()){
            textView_logs.append("Please select active sensors.\n");
        }
        else {

            // lock user entered data
            editText_id.setEnabled(false);
            editText_forename.setEnabled(false);
            editText_surname.setEnabled(false);
            editText_dob.setEnabled(false);
            editText_recordTime.setEnabled(false);
            editText_deviceMAC.setEnabled(false);
            checkBox_recordECG.setEnabled(false);
            checkBox_recordEMG.setEnabled(false);
            checkBox_recordEDA.setEnabled(false);
            //lock buttons
            button_connect.setEnabled(false);
            button_disconnect.setEnabled(false);
            button_start.setEnabled(false);
            button_clear.setEnabled(false);
            button_save.setEnabled(false);

            // start start bitalino async task
            StartBitalino startBitalino = new StartBitalino();
            startBitalino.execute();
        }
    }

    // as gathering data from the bitalino could take a long time, they must be contained in a async task
    class StartBitalino extends AsyncTask<Void, Void, Void> {
        // boolean for error
        private Boolean b_error = false;
        @Override
        protected Void doInBackground(Void... Params) {
            // set error false
            b_error = false;
            // try and connect to bitalino via bluetooth
            try {
                // reference record time as int
                int i_recordTime = Integer.parseInt(s_recordTime);
                // open bitalino input and output streams
                bitalino.open(bt_socket.getInputStream(), bt_socket.getOutputStream());
                // start data acquisition and record for i_recordTime
                bitalino.start();
                frames = bitalino.read(1000 * i_recordTime);
                // stop acquisition and close bluetooth connection
                bitalino.stop();
                bt_socket.close();
            } catch (Exception e) {
                // if error set bool to true
                b_error = true;
            }
            return null;
        }

        //executed when the async task has completed
        @Override
        protected void onPostExecute(Void result) {
            // if no error
            if (b_error == false) {
                // check the data recorded checkbox
                checkBox_dataRecorded.setChecked(true);
                textView_logs.append("Bitalino has finished recording.\n");
                textView_logs.append("Gathered " + frames.length + " readings over " + s_recordTime + " seconds.\n");
                textView_logs.append("Bitalino has been disconnected.\n");
                // enable clear and save buttons
                button_clear.setEnabled(true);
                button_save.setEnabled(true);
            }
            else {
                textView_logs.append("There was an error getting data, make sure Bitalino is connected.\n");
                // free user entered data
                editText_id.setEnabled(true);
                editText_forename.setEnabled(true);
                editText_surname.setEnabled(true);
                editText_dob.setEnabled(true);
                editText_recordTime.setEnabled(true);
                editText_deviceMAC.setEnabled(true);
                checkBox_recordECG.setEnabled(true);
                checkBox_recordEMG.setEnabled(true);
                checkBox_recordEDA.setEnabled(true);
                // enable buttons
                button_connect.setEnabled(true);
                button_disconnect.setEnabled(true);
                button_start.setEnabled(true);
                button_clear.setEnabled(true);
                button_save.setEnabled(true);
            }
        }
    }

    // function to handle connect button presses
    public void clear(View view) {
        // update user
        textView_logs.setText("Clearing data...\n");
        // check there is recorded data
        if (!checkBox_dataRecorded.isChecked()) {
            textView_logs.append("No recorded data, please record data.\n");
        }
        else {
            // clear user entered data
            editText_id.setText(null);
            editText_forename.setText(null);
            editText_surname.setText(null);
            editText_dob.setText(null);
            editText_recordTime.setText(null);
            checkBox_recordECG.setChecked(false);
            checkBox_recordEMG.setChecked(false);
            checkBox_recordEDA.setChecked(false);
            // enable user entered data
            editText_id.setEnabled(true);
            editText_forename.setEnabled(true);
            editText_surname.setEnabled(true);
            editText_dob.setEnabled(true);
            editText_recordTime.setEnabled(true);
            editText_deviceMAC.setEnabled(true);
            checkBox_recordECG.setEnabled(true);
            checkBox_recordEMG.setEnabled(true);
            checkBox_recordEDA.setEnabled(true);
            // enable buttons
            button_connect.setEnabled(true);
            button_disconnect.setEnabled(true);
            button_start.setEnabled(true);
            // clear frames data array
            frames = null;
            // clear the data checkbox
            checkBox_dataRecorded.setChecked(false);
            textView_logs.append("Data cleared.\n");
        }
    }

    // function to handle save button presses
    public void save(View view) {
        textView_logs.setText("Preparing to save data and send to server...\n");
        // check there is recorded data
        if (!checkBox_dataRecorded.isChecked()) {
            textView_logs.append("Please gather data from patient before saving to server.\n");
        }
        // save data to file and send to server
        else {
            textView_logs.append("Saving patient: " + s_forename + " " + s_surname + "...\n");

            //lock buttons
            button_clear.setEnabled(false);
            button_save.setEnabled(false);

            // start save data async task
            SaveData saveData = new SaveData();
            saveData.execute(checkBox_recordECG.isChecked(), checkBox_recordEMG.isChecked(), checkBox_recordEDA.isChecked());
        }
    }

    // as network operations are not able to be executed on the main UI thread, they must be contained in a async task
    class SaveData extends AsyncTask<Boolean, Void, Void> {
        // loop counter to print time to csv;
        private int i_index = 0;
        // bytes read counter for sending to server
        private int i_bytesRead = 0;
        // boolean for error
        private Boolean b_error = false;

        @Override
        protected Void doInBackground(Boolean... Params) {
            // set error false
            b_error = false;
            // get parameters
            Boolean b_recordECG = Params[0];
            Boolean b_recordEMG = Params[1];
            Boolean b_recordEDA = Params[2];
            // if the ECG checkbox was checked save ECG data
            if (b_recordECG == true) {
                // try and save the data to external storage
                try {
                    file_ECG = new File(getExternalFilesDir(null), s_fileName + "_ECG.csv");
                    PrintWriter pw_ECG = new PrintWriter((file_ECG));
                    pw_ECG.print(s_fileName + "_ECG.csv\n");
                    pw_ECG.print("Time(s),mV\n");
                    // loop through frames and save ECG data
                    i_index = 0;
                    for (BITalinoFrame frame : frames) {
                        pw_ECG.print((float) i_index / 1000 + "," + SensorDataConverter.scaleECG(0, frame.getAnalog(0)) + "\n");
                        i_index++;
                    }
                    pw_ECG.close();
                } catch (Exception e) {
                    //Log.e(TAG, "ERROR saving ECG data file locally " + e);
                    b_error = true;
                }
                // try and send the file to the server
                try {
                    // create socket
                    ip_socket = new Socket(s_serverAddress, i_serverPort);
                    // make byte array the length of the file to send
                    byte[] byteArray = new byte[(int) file_ECG.length()];
                    // set file input stream and buffered input stream
                    fis = new FileInputStream(file_ECG);
                    bis = new BufferedInputStream(fis);

                    // get socket output stream
                    ip_os = ip_socket.getOutputStream();
                    // write to output stream
                    i_bytesRead = 0;
                    while ((i_bytesRead = bis.read(byteArray)) > 0) {
                        ip_os.write(byteArray, 0, i_bytesRead);
                    }

                    // flush and close streams and socket
                    ip_os.flush();
                    ip_os.close();
                    bis.close();
                    fis.close();
                    ip_socket.close();
                } catch (Exception e) {
                    //Log.e(TAG, "ERROR saving ECG data file to server " + e);
                    b_error = true;
                }
            }
            // if the EMG checkbox was checked save EMG data
            if (b_recordEMG == true) {
                // try and save the data to external storage
                try {
                    file_EMG = new File(getExternalFilesDir(null), s_fileName + "_EMG.csv");
                    PrintWriter pw_EMG = new PrintWriter((file_EMG));
                    pw_EMG.print(s_fileName + "_EMG.csv\n");
                    pw_EMG.print("Time(s),mV\n");
                    // loop through frames and save ECG data
                    i_index = 0;
                    for (BITalinoFrame frame : frames) {
                        pw_EMG.print((float) i_index / 1000 + "," + SensorDataConverter.scaleEMG(1, frame.getAnalog(1)) + "\n");
                        i_index++;
                    }
                    pw_EMG.close();
                } catch (Exception e) {
                    //Log.e(TAG, "ERROR saving EMG data file locally " + e);
                    b_error = true;
                }
                // try and send the file to the server
                try {
                    // create socket
                    ip_socket = new Socket(s_serverAddress, i_serverPort);
                    // make byte array the length of the file to send
                    byte[] byteArray = new byte[(int) file_EMG.length()];
                    // set file input stream and buffered input stream
                    fis = new FileInputStream(file_EMG);
                    bis = new BufferedInputStream(fis);

                    // get socket output stream
                    ip_os = ip_socket.getOutputStream();
                    // write to output stream
                    i_bytesRead = 0;
                    while ((i_bytesRead = bis.read(byteArray)) > 0) {
                        ip_os.write(byteArray, 0, i_bytesRead);
                    }

                    // flush and close streams and socket
                    ip_os.flush();
                    ip_os.close();
                    bis.close();
                    fis.close();
                    ip_socket.close();
                } catch (Exception e) {
                    //Log.e(TAG, "ERROR saving EMG data file to server " + e);
                    b_error = true;
                }
            }
            // if the EDA checkbox was checked save EDA data
            if (b_recordEDA == true) {
                // try and save the data to external storage
                try {
                    file_EDA = new File(getExternalFilesDir(null), s_fileName + "_EDA.csv");
                    PrintWriter pw_EDA = new PrintWriter((file_EDA));
                    pw_EDA.print(s_fileName + "_EDA.csv\n");
                    pw_EDA.print("Time(s),uS\n");
                    // loop through frames and save ECG data
                    i_index = 0;
                    for (BITalinoFrame frame : frames) {
                        pw_EDA.print((float) i_index / 1000 + "," + SensorDataConverter.scaleEDA(2, frame.getAnalog(2)) + "\n");
                        i_index++;
                    }
                    pw_EDA.close();
                } catch (Exception e) {
                    //Log.e(TAG, "ERROR saving EDA data file locally " + e);
                    b_error = true;
                }
                // try and send the file to the server
                try {
                    // create socket
                    ip_socket = new Socket(s_serverAddress, i_serverPort);
                    // make byte array the length of the file to send
                    byte[] byteArray = new byte[(int) file_EDA.length()];
                    // set file input stream and buffered input stream
                    fis = new FileInputStream(file_EDA);
                    bis = new BufferedInputStream(fis);

                    // get socket output stream
                    ip_os = ip_socket.getOutputStream();
                    // write to output stream
                    i_bytesRead = 0;
                    while ((i_bytesRead = bis.read(byteArray)) > 0) {
                        ip_os.write(byteArray, 0, i_bytesRead);
                    }

                    // flush and close streams and socket
                    ip_os.flush();
                    ip_os.close();
                    bis.close();
                    fis.close();
                    ip_socket.close();
                } catch (Exception e) {
                    //Log.e(TAG, "ERROR saving EDA data file to server " + e);
                    b_error = true;
                }
            }
            return null;
        }

        //executed when the async task has completed
        @Override
        protected void onPostExecute(Void result) {
            // if no error
            if (b_error == false) {
                // clear user entered data
                editText_id.setText(null);
                editText_forename.setText(null);
                editText_surname.setText(null);
                editText_dob.setText(null);
                editText_recordTime.setText(null);
                checkBox_recordECG.setChecked(false);
                checkBox_recordEMG.setChecked(false);
                checkBox_recordEDA.setChecked(false);
                // enable user entered data
                editText_id.setEnabled(true);
                editText_forename.setEnabled(true);
                editText_surname.setEnabled(true);
                editText_dob.setEnabled(true);
                editText_recordTime.setEnabled(true);
                editText_deviceMAC.setEnabled(true);
                checkBox_recordECG.setEnabled(true);
                checkBox_recordEMG.setEnabled(true);
                checkBox_recordEDA.setEnabled(true);
                // enable buttons
                button_connect.setEnabled(true);
                button_disconnect.setEnabled(true);
                button_start.setEnabled(true);
                button_clear.setEnabled(true);
                button_save.setEnabled(true);
                // clear frames data array
                frames = null;
                // clear the data checkbox
                checkBox_dataRecorded.setChecked(false);
                textView_logs.append("Save successful.\n");
            }
            else {
                textView_logs.append("There was an error saving data, please make sure external storage is connected and there is a valid internet connection.\n");
                // enable clear and save buttons
                button_clear.setEnabled(true);
                button_save.setEnabled(true);
            }
        }
    }
}




