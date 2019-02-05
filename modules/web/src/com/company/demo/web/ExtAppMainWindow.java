package com.company.demo.web;

import com.company.demo.entity.Place;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.web.app.mainwindow.AppMainWindow;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExtAppMainWindow extends AppMainWindow {

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    @Inject
    private Logger log;

    @Inject
    private SuggestionPickerField<Place> placesSuggestionPicker;
    @Inject
    private GoogleApiConfig googleApiConfig;

    @Inject
    private Metadata metadata;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        placesSuggestionPicker.setSearchExecutor(this::loadPlaces);

        if (StringUtils.isBlank(googleApiConfig.getApiKey())) {
            throw new IllegalStateException("Google API Key is not set");
        }
    }

    private List loadPlaces(String input, @SuppressWarnings("unused") Map<String, Object> searchParams) {
        log.info("Searching for {}", input);

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            String sb = PLACES_API_BASE + TYPE_AUTOCOMPLETE +
                    OUT_JSON +
                    "?sensor=false" +
                    "&key=" + googleApiConfig.getApiKey() +
                    "&input=" +
                    URLEncoder.encode(input, "utf8");

            URL url = new URL(sb);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (Exception e) {
            log.error("Error processing Places API URL", e);
            return Collections.emptyList();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            if (jsonObj.has("error_message")) {
                String errorMessage = jsonObj.getString("error_message");
                if (errorMessage != null) {
                    log.error("Google API error {}", errorMessage);
                    return Collections.emptyList();
                }
            }

            JSONArray predictionsJsonArray = jsonObj.getJSONArray("predictions");
            ArrayList<Place> resultList = new ArrayList<>(predictionsJsonArray.length());

            // fill result list
            for (int i = 0; i < predictionsJsonArray.length(); i++) {
                Place place = metadata.create(Place.class);

                place.setTitle(predictionsJsonArray.getJSONObject(i).getString("description"));

                resultList.add(place);
            }

            return resultList;
        } catch (JSONException e) {
            log.error("Error processing JSON results", e);
            return Collections.emptyList();
        }
    }
}