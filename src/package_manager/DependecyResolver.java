/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package package_manager;

import com.google.api.core.ApiFuture;
import io.ous.jtoml.JToml;
import io.ous.jtoml.Toml;
import io.ous.jtoml.TomlTable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jscheme.FileAttributes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author hexaredecimal
 */
public class DependecyResolver {

	private boolean is_dirty;
	private FileAttributes config;
	private Firestore db;

	public DependecyResolver(boolean is_dirty) {
		this.is_dirty = is_dirty;
		this.config = new FileAttributes("project.toml");
	}

	public DependecyResolver(boolean is_dirty, FileAttributes path) {
		this.is_dirty = is_dirty;
		this.config = (path);
	}

	private void connectCloud() {
		try {
			_connectCloud();
		} catch (IOException e) {
			System.err.println("Error: failed to establish connection with server");
			System.exit(101);
		}
	}

	private void _connectCloud() throws IOException {
		// InputStream serviceAccount = new StringBufferInputStream(s) 
		InputStream serviceAccount = new FileInputStream("hackscheme.json");
		GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
		FirebaseOptions options = new FirebaseOptions.Builder()
						.setCredentials(credentials)
						.build();
		FirebaseApp.initializeApp(options);
		db = FirestoreClient.getFirestore();
	}

	private QueryDocumentSnapshot find(List<QueryDocumentSnapshot> docs, String package_name, String package_version) {
		QueryDocumentSnapshot ret = null;
		for (QueryDocumentSnapshot doc : docs) {
			String name = doc.getString("package_name").toLowerCase();
			String version = doc.getString("package_version").toLowerCase();
			String url = doc.getString("package_url");
			if (name.equals(package_name.toLowerCase()) && version.equals(package_version.toLowerCase())) {
				ret = doc;
			}
		}
		return ret;
	}

	public void resolve() {
		try {
			Toml project_file = JToml.parse(new File(config.getPath()));
			TomlTable table = project_file.getTomlTable("dependencies");
			Set<String> keys = table.keySet();

			if (keys.size() == 0) {
				return;
			}

			connectCloud();
			ApiFuture<QuerySnapshot> packages_query = db.collection("packages").get();
			QuerySnapshot querySnapshot = packages_query.get();
			List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
			for (String pkg_name : keys) {
				String version = table.getString(pkg_name);
				QueryDocumentSnapshot found = find(documents, pkg_name, version);

				if (found == null) {
					System.out.println("Error: Invalid package named `".concat(pkg_name).concat("`, check the package name or version"));
					System.exit(101);
				}
				String url = found.getString("package_url");
				System.out.println("Downloading: ".concat(pkg_name).concat(" version ").concat(version));
				String packed_tar = "".concat(pkg_name).concat("-").concat(version).concat(".tar.gz");
				String dest = ".pkg/".concat(packed_tar);
				FileDownloader.downloadGZ(pkg_name, url, dest);

				System.out.println("Extracting: ".concat(pkg_name).concat(" to the packages folder"));
				FileDownloader.extractGZ(dest, packed_tar);
			}

			this.config.saveAttributes();

		} catch (IOException ex) {
			System.err.println("Error: failed to open `project.toml` for parsing");
			System.exit(101);
		} catch (InterruptedException ex) {
			Logger.getLogger(DependecyResolver.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ExecutionException ex) {
			Logger.getLogger(DependecyResolver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void saveInfo() {
		this.config.saveAttributes();
	}

	public boolean isDirty() {
		return this.is_dirty;
	}
}
