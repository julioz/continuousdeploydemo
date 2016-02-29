package br.com.zynger.continuousdeploy;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits;
import com.google.api.services.androidpublisher.AndroidPublisher.Edits.Listings;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.Apk;
import com.google.api.services.androidpublisher.model.ApksListResponse;
import com.google.api.services.androidpublisher.model.AppEdit;
import com.google.api.services.androidpublisher.model.Listing;
import com.google.api.services.androidpublisher.model.ListingsListResponse;

public class Main {

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

		printListings(edits, transactionId);
		listApks(edits, transactionId);

		String description = "This app is a simple demo application for developers who are interested in continuous deploy "
				+ "in the android platform.\n\n"
				+ "Check out my presentation for further details or check my personal website.";
		setListingDescription(edits, transactionId, "en-US", description);

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
}
