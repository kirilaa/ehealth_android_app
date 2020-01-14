/*
Codificamos el Json en base64 para mandarlo en la url
Después lo que recibimos (el pid del temporizador en el servidor)
lo almacenamos en la variable texto, si es un 0 el temporizador no está activo.
* */

package com.example.anton.eHealthApp;

import android.util.Log;

import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Request extends Thread{

    private String texto;
    private boolean conexion;
    private String base64json;


    Request(JSONObject json){
        String jsonString = json.toString();
        Base64.Encoder encoder = Base64.getEncoder();
        base64json = encoder.encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
    }

    public void run() {

        try {
            URL url = new URL("http://192.168.3.141/comprobar.php?json="+base64json);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            int responseCode = urlConnection.getResponseCode();
            String responseMessage = urlConnection.getResponseMessage();
            String TAG = "Reply";
            Log.d(TAG, responseMessage + "         " + responseCode);

            InputStream in = urlConnection.getInputStream();
            texto = getStringFromInputStream(in);
            Log.d(TAG, texto);
            conexion = true;
        }catch (Exception e) {
            conexion = false;
            System.out.println(e.getMessage());
        }
    }

    String getTexto(){
        return this.texto;
    }

    boolean getConexion(){
        return conexion;
    }

    //Convierte el stream de datos a un String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

}

