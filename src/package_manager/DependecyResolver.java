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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.StringBufferInputStream;
import java.net.URL;
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

	public void setDb(Firestore db) {
		this.db = db;
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
		InputStream serviceAccount = DependecyResolver.class.getResourceAsStream("/res/hackscheme.json");

		//InputStream serviceAccount = new FileInputStream(config);
		GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
		FirebaseOptions options = new FirebaseOptions.Builder()
						.setCredentials(credentials)
						.build();
		FirebaseApp.initializeApp(options);
		db = FirestoreClient.getFirestore();
	}

	private Object[] find(List<QueryDocumentSnapshot> docs, String package_name, String package_version) {
		QueryDocumentSnapshot ret = null;
		int i;
		for (i = 0; i < docs.size(); i++) {
			QueryDocumentSnapshot doc = docs.get(i);
			String name = doc.getString("package_name").toLowerCase();
			String version = doc.getString("package_version").toLowerCase();
			String url = doc.getString("package_url");
			if (name.equals(package_name.toLowerCase()) && version.equals(package_version.toLowerCase())) {
				ret = doc;
				break;
			}

			if (name.equals(package_name.toLowerCase())) {
				break;
			}
		}

		Object[] arr = new Object[2]; 
		arr[0] = ret; 
		arr[1] = i; 
		return arr;
	}

	public void resolve() {
		try {
			Toml project_file = JToml.parse(new File(config.getPath()));
			TomlTable table = project_file.getTomlTable("dependencies");
			Set<String> keys = table.keySet();

			if (keys.size() == 0) {
				return;
			}

			if (db == null) {
				connectCloud();
			}

			ApiFuture<QuerySnapshot> packages_query = db.collection("packages").get();
			QuerySnapshot querySnapshot = packages_query.get();
			List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
			for (String pkg_name : keys) {
				String version = table.getString(pkg_name);
				Object[] arr = find(documents, pkg_name, version);
				QueryDocumentSnapshot found = (QueryDocumentSnapshot) arr[0];

				if (found == null) {
					System.err.println("Error: Invalid package named `".concat(pkg_name).concat("`, check the package name or version"));
					Integer index = (Integer) arr[1];
					int i = index.intValue();
					QueryDocumentSnapshot doc = documents.get(i);
					String name = doc.getString("package_name");
					String version_ = doc.getString("package_version");
					System.err.println("Hint: Cannot find package " + (pkg_name.toLowerCase() + " - " + version) + " but found " + (name + " - " + version_) + ".");
					System.exit(101);
				}
				String url = found.getString("package_url");
				System.out.println("Downloading: ".concat(pkg_name).concat(" version ").concat(version));
				String packed_tar = "".concat(pkg_name).concat("-").concat(version).concat(".tar.gz");
				String dest = ".pkg/".concat(packed_tar);
				FileDownloader.downloadGZ(pkg_name, url, dest);

				System.out.println("Extracting: ".concat(pkg_name).concat(" to the packages folder"));
				FileDownloader.extractGZ(dest, packed_tar);

				String extracted_dir = ".pkg/".concat(pkg_name).toLowerCase();
				String inner_project = extracted_dir.concat("/project.toml");

				File inner_file = new File(inner_project);
				if (inner_file.exists()) {
					System.out.println("Resolving dependencies for ".concat(pkg_name));
					FileAttributes current_config = new FileAttributes(inner_project);
					DependecyResolver deps = new DependecyResolver(true, current_config);
					deps.setDb(db);
					deps.resolve();
				} else {
					System.err.println("Error: dependency `".concat(pkg_name).concat("` is missing a project file"));
					System.exit(101);
				}
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
