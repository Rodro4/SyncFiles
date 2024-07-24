package com.example.syncfiles;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends Activity {
    private static final int SERVER_PORT = 65432;
    private static final String SERVER_IP = "192.168.1.137"; // Cambia esto por la IP de tu PC
    private static final int PICK_FILE_REQUEST = 1;

    private Button selectFileButton;
    private Button syncButton;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectFileButton = findViewById(R.id.buttonSelectFile);
        syncButton = findViewById(R.id.buttonSync);

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                    Socket socket = new Socket(serverAddr, SERVER_PORT);

                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.println(line);
                    }

                    reader.close();
                    socket.close();

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
