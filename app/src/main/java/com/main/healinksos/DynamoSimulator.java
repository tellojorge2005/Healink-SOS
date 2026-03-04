package com.main.healinksos;

import android.util.Log;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DynamoSimulator {

    private static final String ACCESS_KEY = "ACCESS_KEY";
    private static final String SECRET_KEY = "SECRET_KEY";
    private static final String TABLE_NAME = "HeaLink_Sensores";

    public interface OnDataReceivedListener {
        void onDataReceived(String bpm, String lat, String lng);
    }

    public void enviarDatosSimulados(int idUsuarioReal, boolean esAlertaSOS) {
        new Thread(() -> {
            try {
                BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
                AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentials);
                client.setRegion(Region.getRegion(Regions.US_EAST_2));

                Map<String, AttributeValue> item = new HashMap<>();
                String idDispositivo = "WATCH_USER_" + String.format(Locale.US, "%03d", idUsuarioReal);

                item.put("id_dispositivo", new AttributeValue().withS(idDispositivo));
                item.put("fecha_hora", new AttributeValue().withS(getIsoDate()));

                Map<String, AttributeValue> biometriaMap = new HashMap<>();
                int randomBpm = 75 + (int)(Math.random() * 15);
                biometriaMap.put("bpm", new AttributeValue().withN(String.valueOf(randomBpm)));
                biometriaMap.put("oxigeno", new AttributeValue().withN("96"));
                biometriaMap.put("temperatura", new AttributeValue().withN("36.5"));
                item.put("biometria", new AttributeValue().withM(biometriaMap));

                Map<String, AttributeValue> ubicacionMap = new HashMap<>();
                ubicacionMap.put("latitud", new AttributeValue().withN("19.4326"));
                ubicacionMap.put("longitud", new AttributeValue().withN("-99.1332"));
                item.put("ubicacion", new AttributeValue().withM(ubicacionMap));

                item.put("alerta_sos", new AttributeValue().withBOOL(esAlertaSOS));

                client.putItem(new PutItemRequest(TABLE_NAME, item));
            } catch (Exception e) { Log.e("HeaLink", "Error enviando: " + e.getMessage()); }
        }).start();
    }

    public void obtenerUltimoDato(int idUsuarioReal, OnDataReceivedListener listener) {
        new Thread(() -> {
            try {
                BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
                AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentials);
                client.setRegion(Region.getRegion(Regions.US_EAST_2));

                String idDispositivo = "WATCH_USER_" + String.format(Locale.US, "%03d", idUsuarioReal);

                QueryRequest queryRequest = new QueryRequest()
                        .withTableName(TABLE_NAME)
                        .withKeyConditions(new HashMap<String, Condition>() {{
                            put("id_dispositivo", new Condition()
                                    .withComparisonOperator(ComparisonOperator.EQ)
                                    .withAttributeValueList(new AttributeValue().withS(idDispositivo)));
                        }})
                        .withScanIndexForward(false)
                        .withLimit(1);

                QueryResult result = client.query(queryRequest);

                if (!result.getItems().isEmpty()) {
                    Map<String, AttributeValue> lastItem = result.getItems().get(0);
                    String bpm = lastItem.get("biometria").getM().get("bpm").getN();
                    String lat = lastItem.get("ubicacion").getM().get("latitud").getN();
                    String lng = lastItem.get("ubicacion").getM().get("longitud").getN();
                    listener.onDataReceived(bpm, lat, lng);
                }
            } catch (Exception e) { Log.e("HeaLink", "Error leyendo: " + e.getMessage()); }
        }).start();
    }

    private String getIsoDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}