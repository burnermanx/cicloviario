package br.org.ta.cicloviario;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks {

	/**
	 * vari�veis com a chave da api e o url dos servicos
	 */
	private static final String API_KEY = "AIzaSyCreptOWN3UAF4LdXLNt6XzMuPAbEciJH0";
	private static final String VERIFY_SYNC_URL = "https://www.googleapis.com/drive/v2/files/1PJXmib36JCeDRrWiemp9v6dsNuL2MU4cD3kz8QY?key=" + API_KEY;
	private static final String POI_SYNC_URL = "https://www.googleapis.com/fusiontables/v1/query?sql=select%20*%20from%201PJXmib36JCeDRrWiemp9v6dsNuL2MU4cD3kz8QY&key=" + API_KEY;
	private ProgressDialog pDialog;
	
	
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		
		//Chamando UpdatePoi, caso seja a primeira execucao do programa
		String PREFS_NAME = "CicloviarioSettings";
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String cacheModifiedDate = settings.getString("ModifiedDate", "NULL");
		if (cacheModifiedDate == "NULL")
			new UpdatePoi().execute();
		//MapFragment mapa = (MapFragment) findViewById(R.id.map);
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			///mTitle = getString(R.string.title_section1);
			break;
		case 2:
			//mTitle = getString(R.string.title_section2);
			break;
		case 3:
			//mTitle = getString(R.string.title_section3);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Classe Async para atualiza��o dos pontos de interesse via chamada HTTP
	 */
	 private class UpdatePoi extends AsyncTask<Void, Void, Void> {
		 
	     	@Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            // Mostrar di�logo de progresso
	            pDialog = new ProgressDialog(MainActivity.this);
	            pDialog.setMessage("Aguarde...");
	            pDialog.setCancelable(false);
	            pDialog.show();
	 
	        }
		 
		 	//Tarefas para o app resolver em background como verificar se os pontos do cache est�o atualizados e a atualiza��o dos mesmos. 
			@Override
			protected Void doInBackground(Void... arg0)  {
				String PREFS_NAME = "CicloviarioSettings";
					
				String modifiedDate = "";
				
				ServiceHandler sh = new ServiceHandler();
				String jsonUpdate = sh.makeServiceCall(VERIFY_SYNC_URL, 1);
				try {
					JSONObject dateObject = new JSONObject(jsonUpdate);
					//Log.d("Meu JSON",jsonUpdate);
					modifiedDate = dateObject.getString("modifiedDate");
					Log.d("ModifiedDate", modifiedDate);
				}
				catch (JSONException e) {
                    e.printStackTrace();
				}
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				String cacheModifiedDate = settings.getString("ModifiedDate", "");
				Log.d("Cache ModifiedDate", cacheModifiedDate);
				
						
				//Se a data de modificado for diferente do que est� no cache... ou seja, est� mais atualizado na internet
				if (!modifiedDate.equals(cacheModifiedDate)) {
					//Baixando o JSON atualizado com os pontos de interesse (POI)
					ServiceHandler shPoi = new ServiceHandler();
					try {
						String FILENAME = "poicache";
						String poiUpdate = shPoi.makeServiceCall(POI_SYNC_URL, 1);
						FileOutputStream outputStream = openFileOutput(FILENAME, Context.MODE_PRIVATE);
						outputStream.write(poiUpdate.getBytes());
						outputStream.close();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					
					//Atualizando o SharedPreferences com a nova data de modifica��o do JSON
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("ModifiedDate", modifiedDate);
					editor.commit();
				}
					return null;
			}
			
			@Override
	        protected void onPostExecute(Void result) {
	            super.onPostExecute(result);
	            // Fechar o di�logo de progresso
	            if (pDialog.isShowing())
	                pDialog.dismiss();
	            /**
	             * Se tudo der certo, tenho um sharedpreference e um arquivo com json no storage interno
	             * */
	           
	        }
	 }
	
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			/*TextView textView = (TextView) rootView
					.findViewById(R.id.section_label);
			textView.setText(Integer.toString(getArguments().getInt(
					ARG_SECTION_NUMBER))); */
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(
					ARG_SECTION_NUMBER));
		}
		
		
	}

}
