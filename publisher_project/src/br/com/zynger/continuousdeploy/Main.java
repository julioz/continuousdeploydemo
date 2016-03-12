package br.com.zynger.continuousdeploy;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Apklistings;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Apks;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Listings;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Tracks;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.Apk;
import com.google.api.services.androidpublisher.model.ApkListing;
import com.google.api.services.androidpublisher.model.ApksListResponse;
import com.google.api.services.androidpublisher.model.AppEdit;
import com.google.api.services.androidpublisher.model.Listing;
import com.google.api.services.androidpublisher.model.ListingsListResponse;
import com.google.api.services.androidpublisher.model.Track;

public class Main {

    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String APP_APK_PATH = "app-release.apk";
    private static final String PACKAGE = Constants.PACKAGE;

    public static void main(String[] args) throws GeneralSecurityException,
            IOException {
        NetHttpTransport http = GoogleNetHttpTransport.newTrustedTransport();
        JacksonFactory json = JacksonFactory.getDefaultInstance();

        Set<String> scopes = Collections
                .singleton(AndroidPublisherScopes.ANDROIDPUBLISHER);

        File secretFile = new File(Constants.SECRET_FILE_PATH);
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(http).setJsonFactory(json)
                .setServiceAccountPrivateKeyId(Constants.KEY_ID)
                .setServiceAccountId(Constants.SERVICE_ACCOUNT_EMAIL)
                .setServiceAccountScopes(scopes)
                .setServiceAccountPrivateKeyFromPemFile(secretFile).build();

        AndroidPublisher publisher = new AndroidPublisher.Builder(http, json,
                credential).setApplicationName(PACKAGE).build();

        final Edits edits = publisher.edits();
        // Create a new edit to make changes.
        AppEdit appEdit = edits.insert(PACKAGE, null).execute();
        String transactionId = appEdit.getId();
        String language = "en-US";
        
        printListings(edits, transactionId);
        listApks(edits, transactionId);

        String description = "This app is a simple demo application for developers who are interested in continuous deploy "
                + "in the android platform.\n\n"
                + "Check out my presentation for further details or check my personal website.";
        setListingDescription(edits, transactionId, language, description);

        File apkFile = new File(APP_APK_PATH);
        String track = "production"; // could be "alpha", "beta", "rollout" or "production"
        String whatsNew = "Updated app through Google Play Developer API!";
        updateApplication(edits, transactionId, language, apkFile, track, whatsNew);
        
        edits.validate(PACKAGE, transactionId).execute();
        edits.commit(PACKAGE, transactionId).execute();
    }

    private static void printListings(final Edits edits,
            final String transactionId) throws IOException {
        Listings listings = edits.listings();
        ListingsListResponse listingsListResponse = listings.list(PACKAGE,
                transactionId).execute();

        for (Listing listing : listingsListResponse.getListings()) {
            System.out.println(listing.toPrettyString());
        }
        System.out.println();
    }

    private static void listApks(Edits edits, String transactionId)
            throws IOException {
        // Get a list of apks.
        ApksListResponse apksResponse = edits.apks()
                .list(PACKAGE, transactionId).execute();

        // Print the apk info.
        for (Apk apk : apksResponse.getApks()) {
            System.out.println(String.format("Version: %d - Binary sha1: %s",
                    apk.getVersionCode(), apk.getBinary().getSha1()));
        }
        System.out.println();
    }

    private static void setListingDescription(final Edits edits,
            final String transactionId, String language, String description)
            throws IOException {
        Listings listings = edits.listings();
        ListingsListResponse listingsListResponse = listings.list(PACKAGE,
                transactionId).execute();
        Listing listing = getListingByLanguage(
                listingsListResponse.getListings(), language);

        if (listing == null) {
            throw new IOException("Could not find listing for language "
                    + language);
        }

        listing.setFullDescription(description);

        listings.update(PACKAGE, transactionId, language, listing).execute();
    }

    private static Listing getListingByLanguage(List<Listing> listings,
            String language) {
        for (Listing listing : listings) {
            if (listing.getLanguage().equalsIgnoreCase(language)) {
                return listing;
            }
        }

        return null;
    }
    
    private static void updateApplication(final Edits edits, final String transactionId,
            final String language, final File apkFile, String trackId, String whatsNewDescription) throws IOException {
        // APK upload
        Apks apks = edits.apks();
        FileContent apkContent = new FileContent(APK_MIME_TYPE, apkFile);
        Apk apk = apks.upload(PACKAGE, transactionId, apkContent).execute();
        int version = apk.getVersionCode();
        System.out.println("Uploaded APK with version code " + version);

        // Assign APK to Track
        Tracks tracks = edits.tracks();
        List<Integer> versions = Collections.singletonList(version);
        Track track = new Track().setVersionCodes(versions);
        tracks.update(PACKAGE, transactionId, trackId, track).execute();
        System.out.println("App " + version + " has been set to " + trackId + " track.");

        // Update APK listing
        Apklistings apklistings = edits.apklistings();
        ApkListing whatsnew = new ApkListing().setRecentChanges(whatsNewDescription);
        apklistings.update(PACKAGE, transactionId, version, language, whatsnew).execute();
        System.out.println("Updated listings with the following 'whats new' content:" +
                System.lineSeparator() + whatsNewDescription);
    }
}
