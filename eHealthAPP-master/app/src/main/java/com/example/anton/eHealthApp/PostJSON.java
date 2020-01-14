/*
 * En esta clase creamos el Json que vamos a mandar al servidor e iniciamos la petición
 * con el mismo.
 * Además también tenemos la función que nos permitirá actualizar la ubicación del
 * dispositivo móvil
 * */


package com.example.anton.eHealthApp;


import android.util.Log;

import org.json.simple.JSONObject;

class PostJSON {

    private int pid = 0;


    @SuppressWarnings("unchecked")
    boolean startRequestEmergency(int valorHR, long date, int latitude, int longitude) throws InterruptedException {
        JSONObject json = new JSONObject();
        json.put("valor", valorHR); //VALOR QUE MANDAMOS AL SERVIDOR.
        json.put("latitude", latitude);
        json.put("longitude", longitude);
        json.put("date", date);
        Request request = new Request(json);
        Log.d("heart", Double.toString(valorHR));
        Log.d("latitud", Double.toString(latitude));
        Log.d("longitud", Double.toString(longitude));
        Log.d("date", String.valueOf(date));
        request.start();
        request.join();
        request.interrupt();

        if (request.getConexion()) {
            Log.d("conexion", "conexion correcta");
            pid = Integer.parseInt(request.getTexto());
            return true;
        } else {
            Log.d("conexion", "conexion fallida");
            pid = 0;
            return false;
        }

    }

    boolean isTimerActive() {
        Log.d("pid", String.valueOf(pid));
        return pid != 0;
    }

    int getPid() {
        return this.pid;
    }

    void reset() {
        pid = 0;
    }

}

