package com.example.syncfiles;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends Activity {
    private static final int SERVER_PORT = 65432;
    private static final int PICK_FILE_REQUEST = 1;

    private EditText ipAddressEditText;
    private Button selectFileButton;
    private Button syncButton;
    private Uri fileUri;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ipAddressEditText = findViewById(R.id.ipAddressEditText);
        selectFileButton = findViewById(R.id.buttonSelectFile);
        syncButton = findViewById(R.id.buttonSync);

        // Load last used IP
        String lastIp = sharedPreferences.getString("last_ip", "");
        ipAddressEditText.setText(lastIp);

        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncFile();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                fileUri = data.getData();
                syncButton.setEnabled(true);
                Toast.makeText(this, "Archivo seleccionado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void syncFile() {
        if (fileUri == null) {
            Toast.makeText(this, "No se ha seleccionado ningún archivo", Toast.LENGTH_SHORT).show();
            return;
        }

        final String serverIp = ipAddressEditText.getText().toString().trim();
        if (serverIp.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese la IP del servidor", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the IP to SharedPreferences
        sharedPreferences.edit().putString("last_ip", serverIp).apply();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(serverIp);
                    Socket socket = new Socket(serverAddr, SERVER_PORT);

                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }

                    writer.flush();
                    socket.shutdownOutput();

                    BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((line = serverReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    reader.close();
                    serverReader.close();
                    socket.close();

                    OutputStream outputStream = getContentResolver().openOutputStream(fileUri, "wt");
                    BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    fileWriter.write(stringBuilder.toString());
                    fileWriter.flush();
                    fileWriter.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Archivo sincronizado", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error al sincronizar archivo", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
