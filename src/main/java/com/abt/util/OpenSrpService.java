package com.abt.util;


import akka.http.javadsl.model.DateTime;
import com.abt.domain.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class for OpenSRP operations.
 */
public class OpenSrpService {

    private static final int clientDatabaseVersion = 17;
    private static final int clientApplicationVersion = 2;
    private static final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    private static final SimpleDateFormat inputFormat2 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * Creates and returns an observation for the start event.
     *
     * @return Obs object for the start event.
     */
    private static Obs getStartOb() {
        return new Obs("concept", "start",
                "163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
                Arrays.asList(new Object[]{new Date()}), null, null, "start");
    }

    /**
     * Creates and returns an observation for the end event.
     *
     * @return Obs object for the end event.
     */
    private static Obs getEndOb() {
        return new Obs("concept", "end",
                "163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
                Arrays.asList(new Object[]{new Date()}), null, null, "end");
    }


    /**
     * Creates a Lab Results Event for a test request.
     *
     * @param labResult The Results object.
     * @return LabResultsEvent Event.
     */
    public static Event getLabResultsEvent(LabResult labResult) {
        Event labResultEvent = new Event();
        labResultEvent.setBaseEntityId(labResult.getBaseEntityId());

        List<LabResult.TBLResult> tblResults = labResult.getTBLResults();

        LabResult.TBLResult mTblResult = null;
        for (LabResult.TBLResult tblResult : tblResults) {
            if (tblResult.getLIMSObservationCode().equalsIgnoreCase("HIVVC") ||
                    tblResult.getLIMSObservationCode().equalsIgnoreCase("HIVVL") ||
                    tblResult.getLIMSObservationCode().equalsIgnoreCase("HIVVM")
            ) {
                mTblResult = tblResult;
                break;
            } else if (tblResult.getLIMSObservationCode().equalsIgnoreCase("HIVPC") || tblResult.getLIMSObservationCode().equalsIgnoreCase("EID")) {
                mTblResult = tblResult;
            }
        }

        if (labResult.getSampleType().equalsIgnoreCase("HVL")) {
            labResultEvent.setEventType("Lab HVL Results");
        } else {
            labResultEvent.setEventType("HEID Test Results");
        }

        labResultEvent.setEntityType("ec_lab_requests");

        labResultEvent.addObs(new Obs("concept", "text",
                "tested_by", "", Arrays.asList(new Object[]{
                labResult.getTestedBy()}), null, null, "tested_by"));


        labResultEvent.addObs(new Obs("concept", "text",
                "authorized_by", "",
                Arrays.asList(new Object[]{labResult.getAuthorisedBy()}), null, null,
                "authorized_by"));

        try {
            Date authorizedDateObj = inputFormat2.parse(labResult.getAuthorisedDateTime());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(authorizedDateObj);
            String authorizedDate = dateFormat.format(calendar.getTime());
            String authorizedTime = timeFormat.format(calendar.getTime());


            labResultEvent.addObs(new Obs("concept", "text",
                    "authorized_date", "",
                    Arrays.asList(new Object[]{authorizedDate}), null, null,
                    "authorized_date"));

            labResultEvent.addObs(new Obs("concept", "text",
                    "authorized_time", "",
                    Arrays.asList(new Object[]{authorizedTime}), null, null,
                    "authorized_time"));

            labResultEvent.addObs(new Obs("concept", "text",
                    "authorized_date_time", "",
                    Arrays.asList(new Object[]{labResult.getAuthorisedDateTime()}), null, null,
                    "authorized_date_time"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            Date testedDateTimeObj = inputFormat2.parse(labResult.getAnalysisDateTime());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(testedDateTimeObj);
            String testedDate = dateFormat.format(calendar.getTime());
            String testedTime = timeFormat.format(calendar.getTime());

            labResultEvent.addObs(new Obs("concept", "text",
                    "tested_date", "",
                    Arrays.asList(new Object[]{testedDate}), null, null,
                    "tested_date"));

            labResultEvent.addObs(new Obs("concept", "text",
                    "tested_time", "",
                    Arrays.asList(new Object[]{testedTime}), null, null,
                    "tested_time"));

            labResultEvent.addObs(new Obs("concept", "text",
                    "tested_date_time", "",
                    Arrays.asList(new Object[]{labResult.getAnalysisDateTime()}), null, null,
                    "tested_date_time"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        labResultEvent.addObs(new Obs("concept", "text",
                "type_of_results", "",
                Arrays.asList(new Object[]{"results"}), null, null,
                "type_of_results"));


        labResultEvent.addObs(new Obs("concept", "text",
                "sample_id", "",
                Arrays.asList(new Object[]{labResult.getSampleId()}), null, null,
                "sample_id"));

        String results = "";
        if (labResult.getSampleType().equalsIgnoreCase("HVL")) {
            labResultEvent.setEventType("Lab HVL Results");

            results = mTblResult.getLIMSRptResult();

            if (mTblResult.getLIMSCodedValue().toLowerCase().contains("tnd") || results.toLowerCase().contains("detected")) {
                results = "11";
            } else if (isInteger(results)) {
                results = mTblResult.getLIMSRptResult();
            } else {
                results = extractIntegers(results).toString();
            }
        } else {
            labResultEvent.setEventType("HEID Test Results");
            results = mTblResult.getLIMSRptResult();
        }

        labResultEvent.addObs(new Obs("concept", "text",
                "results", "",
                Arrays.asList(new Object[]{results}), null, null,
                "results"));


        labResultEvent.addObs(new Obs("concept", "text",
                "source", "",
                Arrays.asList(new Object[]{"DDR"}), null, null,
                "source"));

        setMetaData(labResultEvent, labResult);
        return labResultEvent;
    }


    /**
     * Creates a Lab Rejection Event for a test request.
     *
     * @param labRejection The Rejection object.
     * @return LabResultsEvent Event.
     */
    public static Event getLabRejectionEvent(LabRejection labRejection) {
        Event labRejectionEvent = new Event();
        labRejectionEvent.setBaseEntityId(labRejectionEvent.getBaseEntityId());

        List<LabRejection.LIMSRejection> tblRejections = labRejection.getLIMSRejectionArray();

        LabRejection.LIMSRejection mTblRejection = null;
        StringBuilder rejectionReasons = new StringBuilder();
        for (LabRejection.LIMSRejection rejection : tblRejections) {
            rejectionReasons.append(rejection.getDescription()).append(", ");
        }

        if (labRejection.getSampleType().equalsIgnoreCase("HVL")) {
            labRejectionEvent.setEventType("Lab HVL Results");
        } else {
            labRejectionEvent.setEventType("HEID Test Results");
        }


        labRejectionEvent.setEntityType("ec_lab_requests");

        labRejectionEvent.addObs(new Obs("concept", "text",
                "tested_by", "", Arrays.asList(new Object[]{
                labRejection.getTestedBy()}), null, null, "tested_by"));


        labRejectionEvent.addObs(new Obs("concept", "text",
                "authorized_by", "",
                Arrays.asList(new Object[]{labRejection.getAuthorisedBy()}), null, null,
                "authorized_by"));

        labRejectionEvent.addObs(new Obs("concept", "text",
                "authorized_by", "",
                Arrays.asList(new Object[]{labRejection.getAuthorisedBy()}), null, null,
                "authorized_by"));


        labRejectionEvent.addObs(new Obs("concept", "text",
                "type_of_results", "",
                Arrays.asList(new Object[]{"rejected"}), null, null,
                "type_of_results"));


        labRejectionEvent.addObs(new Obs("concept", "text",
                "reasons_for_rejection", "",
                Arrays.asList(new Object[]{rejectionReasons.toString()}), null, null,
                "reasons_for_rejection"));


        labRejectionEvent.addObs(new Obs("concept", "text",
                "sample_id", "",
                Arrays.asList(new Object[]{labRejection.getSampleId()}), null, null,
                "sample_id"));

        labRejectionEvent.addObs(new Obs("concept", "text",
                "source", "",
                Arrays.asList(new Object[]{"DDR"}), null, null,
                "source"));


        try {
            Date rejectionDateTimeObj = inputFormat.parse(labRejection.getReceivedDateTime());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(rejectionDateTimeObj);

            String rejectionDate = dateFormat.format(calendar.getTime());
            String rejectionTime = timeFormat.format(calendar.getTime());


            labRejectionEvent.addObs(new Obs("concept", "text",
                    "rejection_date", "",
                    Arrays.asList(new Object[]{rejectionDate}), null, null,
                    "rejection_date"));

            labRejectionEvent.addObs(new Obs("concept", "text",
                    "rejection_time", "",
                    Arrays.asList(new Object[]{rejectionTime}), null, null,
                    "rejection_time"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        labRejectionEvent.addObs(new Obs("concept", "text",
                "results", "",
                Arrays.asList(new Object[]{"rejection"}), null, null,
                "results"));

        setMetaData(labRejectionEvent, labRejection);
        return labRejectionEvent;
    }

    private static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static List<Integer> extractIntegers(String str) {
        List<Integer> integers = new ArrayList<>();
        Pattern pattern = Pattern.compile("-?\\d+");
        Matcher matcher = pattern.matcher(str);

        while (matcher.find()) {
            integers.add(Integer.parseInt(matcher.group()));
        }

        return integers;
    }

    /**
     * Set Event Metadata
     *
     * @param event              created Event
     * @param labRequestMetadata Object
     */
    private static void setMetaData(Event event, LabRequestMetadata labRequestMetadata) {
        event.setLocationId(labRequestMetadata.getLocationId());
        event.setProviderId(labRequestMetadata.getProviderId());
        event.setTeamId(labRequestMetadata.getTeamId());
        event.setTeam(labRequestMetadata.getTeam());
        event.setType("Event");
        event.setFormSubmissionId(UUID.randomUUID().toString());
        event.setEventDate(new Date());
        event.setDateCreated(new Date());
        event.addObs(OpenSrpService.getStartOb());
        event.addObs(OpenSrpService.getEndOb());
        event.setClientApplicationVersion(clientApplicationVersion);
        event.setClientDatabaseVersion(clientDatabaseVersion);
        event.setDuration(0);
        event.setIdentifiers(new HashMap<>());
    }


    public static String sendDataToDestination(EventRequest events, String mUrl, String username, String password) {
        String response = "";
        try {
            URL url = new URL(mUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            configureBasicAuthHeader(username, password, conn);

            try (OutputStream os = conn.getOutputStream()) {

                Gson gson
                        = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .registerTypeAdapter(DateTime.class, new DateTimeTypeConverter())
                        .create();

                byte[] input =
                        gson.toJson(events).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = conn.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) { // success
                System.out.println("POST was successful.");
                response = "sending successful";
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                System.out.println("POST request failed.");
                response = "Authentication Error: Incorrect Username or password";
            } else {
                System.out.println("POST request failed.");
                response = "sending failed";
            }

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            response = "Internal Error while processing the payload";

        }
        return response;
    }

    public static void configureBasicAuthHeader(String username,
                                                String password,
                                                HttpURLConnection conn) {
        if (
                username != null &&
                        !username.isEmpty() &&
                        password != null &&
                        !password.isEmpty()
        ) {
            String auth = username + ":" + password;
            byte[] encodedAuth =
                    Base64.getEncoder().encode(auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);

            conn.setRequestProperty("Authorization", authHeader);
        }
    }
}
