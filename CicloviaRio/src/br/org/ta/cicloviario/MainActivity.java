package br.org.ta.cicloviario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks {

	/**
	 * vari�veis com a chave da api, url dos servicos, e valores padrao
	 */
	private static final String API_KEY = "&key=AIzaSyBD4CmETkOmfBxadLcX1tac0SK6oFbij1E";
	private static final String VERIFY_SYNC_URL = "https://www.googleapis.com/drive/v2/files/1PJXmib36JCeDRrWiemp9v6dsNuL2MU4cD3kz8QY" + API_KEY;
	private static final String POI_SYNC_URL = "https://www.googleapis.com/fusiontables/v1/query?sql=select%20*%20from%201PJXmib36JCeDRrWiemp9v6dsNuL2MU4cD3kz8QY";
	private static final double LAT_INICIAL = -22.93949546286523;
	private static final double LON_INICIAL = -43.34304013427737;
	private static final int ZOOM_INICIAL = 10;
	private static final LatLng POS_INICIAL = new LatLng(LAT_INICIAL, LON_INICIAL);
	private static final String tipoPontos[] = {"Ciclovia", "Ciclofaixa", "Faixa+Compartilhada", "Via+Compartilhada", "Via+Proibida", "Bicicleta+Publica", "Bicicletario", "Oficina+de+Bicicleta"};
	private ProgressDialog pDialog;
	private GoogleMap map;
	private boolean locationEnabled = false;
	
	
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
		
		
		updatePoi(); //Chamando UpdatePoi
		
	}
	
	//Manipulando o GoogleMaps no onResume, 2o metodo do ciclo de vida da Activity
	protected void onResume() {
		super.onResume();
		SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);

				map = mapFrag.getMap();
		if (map != null) {
			map.moveCamera((CameraUpdateFactory.newLatLngZoom(POS_INICIAL, ZOOM_INICIAL)));
		}
	}
	
	//Resolvendo o problema do app crashear ao virar o dispositivo
	@Override
	public void onDestroy() {
	    FragmentManager fm = getSupportFragmentManager();

	    Fragment xmlFragment = fm.findFragmentById(R.id.map);
	    if (xmlFragment != null) {
	        fm.beginTransaction().remove(xmlFragment).commit();
	    }

	    super.onDestroy();
	}
	
	//Metodo para testes
	public void testeDraw() {
		drawOnMap("Ciclovia");
	}
	
	//Metodo para mostrar a localizacao atual
	public void myLocation() {
		if (!getLocationEnabled()) {
			map.setMyLocationEnabled(true);
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			String provider = locationManager.getBestProvider(criteria, true);
			Location myLocation = locationManager.getLastKnownLocation(provider);
			double latitude = myLocation.getLatitude();
			double longitude = myLocation.getLongitude();
			LatLng latLng = new LatLng(latitude, longitude);    
			map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
			map.animateCamera(CameraUpdateFactory.zoomTo(15));
			locationEnabled = true;
		}
		else {
			map.setMyLocationEnabled(false);
			locationEnabled = false;
		}
	}
	
	//Metodo para verificar se a localizacao por GPS esta ativada ou nao
	public boolean getLocationEnabled() {
		return locationEnabled;
	}
	
	//Metodo para chamar o UpdatePoi - Para uso em outros fragments
	public void updatePoi() {
		new UpdatePoi().execute();
	}
	
	//Metodo para chamar Toasts
	public void toastThis(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show(); //Mostra toast
	}
	
	//Metodo para a leitura dos arquivos .json
	public String readJsonFile(String file) {
		StringBuffer output = new StringBuffer();
		try {
			FileInputStream fIn = openFileInput(file);
			InputStreamReader iSr = new InputStreamReader (fIn);
			BufferedReader buffReader = new BufferedReader (iSr);
			
			String data = buffReader.readLine();
			while (data != null) {
				output.append(data);
				data = buffReader.readLine();
			}
			iSr.close();
			
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return output.toString();
	} 
	
	//Metodo para desenhar os pontos no mapa
	public void drawOnMap(String ponto) {
		JSONObject jsonFile = null; 
		JSONArray array = null;
		String description = null;
		String name = null;
		String cor = null;
		String tipo = null;
		String type = null;
		String newColor = null;

		double poLat = 0;
		double poLng = 0;
		
		double[] plLat = {};
		double[] plLng = {};

		List<LatLng> llPoints = new ArrayList<>();
		LatLng ll = null;
		
		try {
			jsonFile = new JSONObject(readJsonFile(ponto+".json"));
			array = jsonFile.getJSONArray("rows");
		}
		catch (JSONException e) {
			e.printStackTrace();
			Log.d ("Falhei aqui", "primeiro exception");
		}
		//Em rows
		for (int i=0; i < array.length(); i++) {
			try {				
				//Em rows
				JSONArray row = (JSONArray) array.get(i);
				String hDescription = row.getString(0);
				description = hDescription.replaceAll("<br>","\n");
				Log.d ("Acertei aqui descricao", description);
				name = row.getString(1);
				Log.d ("Acertei aqui nome", name);
				cor = row.getString(3);
				Log.d ("Acertei cor", cor);
				tipo = row.getString(4);
				Log.d ("Acertei tipo", tipo);
				//We need to go deeper! 
				JSONObject container = (JSONObject) row.get(2);
				JSONObject geometry = (JSONObject) container.get("geometry");
				type = geometry.getString("type");
				Log.d ("Acertei type", type);
				JSONArray arrayCoordinates = geometry.getJSONArray("coordinates");
				//Reaching the limbo!
				//Se eh um LineString, entao ele ainda tem um array dentro do array
				if (type.equals("LineString")) {
					llPoints.clear();
					for (int l=0; l < arrayCoordinates.length(); l++) {
						JSONArray rowCoordinates = (JSONArray) arrayCoordinates.get(l);
						poLat =  rowCoordinates.getDouble(1);
						Log.d ("Acertei lat", String.valueOf(poLat));
						poLng = rowCoordinates.getDouble(0);
						Log.d ("Acertei lgn", String.valueOf(poLng));
						llPoints.add(new LatLng(poLat,poLng));
					}
				}
				//Senao o proprio array ja tem os valores
				if (type.equals("Point")){
					poLat =  arrayCoordinates.getDouble(1);
					Log.d ("Acertei lat", String.valueOf(poLat));
					poLng = arrayCoordinates.getDouble(0);
					Log.d ("Acertei lgn", String.valueOf(poLng));
					ll = new LatLng(poLat,poLng);
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
				Log.d ("Falhei aqui", "segundo exception");
			}
			
			
			//Vamos por os pontos ou trajetos no mapa
			if (type.equals("LineString")) {
				
				/*for (int p=0; p < plLat.length; p++) {
					ll = new LatLng(plLat[p],plLng[p]);
					llPoints.add(ll);
				}*/
				
				map.addPolyline(new PolylineOptions()
					.addAll(llPoints)
						);
			}
			if (type.equals("Point")) {
				map.addMarker(new MarkerOptions()
					.position(ll)
					.title(name)
					.snippet(description)
					.icon(BitmapDescriptorFactory.defaultMarker(colorizeMarks(cor)))
						);
			}
			
		}
	}
	
	//Metodo para pegar as cores do JSON e transforma-la em cor para a classe BitmapDescriptorFactory
	public float colorizeMarks(String color) {
		float hue=255;
		switch (color) {
			case "green":
				hue = (float) 120.0;
				break;
			case "33ff3373":
				hue = (float) 120.0;
				break;
			case "blue":
				hue = (float) 240.0;
				break;
			case "black":
				hue = (float) 0.0;
				break;
			case "small_blue":
				hue = (float) 210.0;
				break;
			case "small_red":
				hue = (float) 300.0;
				break;
			case "small_yellow":
				hue = (float) 30.0;
				break;
			case "cyan":
				hue = (float) 180.0;
				break;
		}
		return hue;
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
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
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
		 	private int updateStatus = 0; //0 = erro na atualiza��o; 1 = atualizado com sucesso; 2 = j� atualizado
	     	@Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            // Mostrar di�logo de progresso
	            pDialog = new ProgressDialog(MainActivity.this);
	            pDialog.setMessage(getString(R.string.dialog_updating));
	            pDialog.setCancelable(false);
	            pDialog.show();
	 
	        }
		 
		 	//Tarefas para o app resolver em background como verificar se os pontos do cache est�o atualizados e a atualiza��o dos mesmos. 
			@Override
			protected Void doInBackground(Void... arg0)  {
				final String PREFS_NAME = "CicloviarioSettings";	
				String modifiedDate = "";
				
				ServiceHandler sh = new ServiceHandler();
				String jsonUpdate = sh.makeServiceCall(VERIFY_SYNC_URL, 1);
				//Comentando essa parte, para poder testar o resto
				/*
				try {
					JSONObject dateObject = new JSONObject(jsonUpdate);
					Log.d("Meu JSON",jsonUpdate);
					modifiedDate = dateObject.getString("modifiedDate");
					Log.d("ModifiedDate", modifiedDate);
				}
				catch (JSONException e) {
                    e.printStackTrace();
                    updateStatus=0;
                    return null;
				} */
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				String cacheModifiedDate = settings.getString("ModifiedDate", "");
				Log.d("Cache ModifiedDate", cacheModifiedDate);
				modifiedDate = "999";
				
				//Se a data de modificado for diferente do que est� no cache... ou seja, est� mais atualizado na internet
				if (!modifiedDate.equals(cacheModifiedDate)) {
					//Baixando o JSON atualizado com os pontos de interesse (POI)
					ServiceHandler shPoi = new ServiceHandler();
					try {
						Log.d("Entrando no for", modifiedDate);
						for (int i=0; i < tipoPontos.length; i++ ) {
						Log.d("Gerando arquivo", tipoPontos[i]);
						String filename = tipoPontos[i] + ".json";
						String poiUpdate = shPoi.makeServiceCall(POI_SYNC_URL+"%20where%20col4%20in%20(%27"+tipoPontos[i]+"%27)"+API_KEY, 1);
						FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
						outputStream.write(poiUpdate.getBytes());
						outputStream.close();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					
					//Atualizando o SharedPreferences com a nova data de modifica��o do JSON
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("ModifiedDate", modifiedDate);
					editor.commit();
					updateStatus = 1; //1 = sucesso
				}
				else {
					updateStatus = 2; // 2 = ja atualizado
				}
					return null;
			}
			
			@Override
	        protected void onPostExecute(Void result) {
	            super.onPostExecute(result);
	            // Fechar o di�logo de progresso
	            if (pDialog.isShowing())
	                pDialog.dismiss();
	            //Mostrar toast de acordo com a atualiza��o feita
	            switch (updateStatus) {
	            case 0:
	            	toastThis(getString(R.string.toast_update_error));
	            break;
	            case 1: 
	            	toastThis(getString(R.string.toast_update_success));
	            	break;
	            case 2:
	            	toastThis(getString(R.string.toast_updated));
	            	break;
	            default:
	            	break;
	            }
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
