package com.xiangyixie.picshouse.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.xiangyixie.picshouse.AppConfig;
import com.xiangyixie.picshouse.R;
import com.xiangyixie.picshouse.httpService.PHHttpClient;
import com.xiangyixie.picshouse.httpService.PHJsonRequest;
import com.xiangyixie.picshouse.util.UserWarning;
import com.xiangyixie.picshouse.view.pinnedHeaderListView.HeaderListViewAdapter;
import com.xiangyixie.picshouse.view.pinnedHeaderListView.PinnedHeaderListView;
import com.xiangyixie.picshouse.view.pinnedHeaderListView.SectionedBaseAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;


public class TabHouseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private final static String TAG = "TabHouseFragment";

    private Activity activity = null;

    private SectionedBaseAdapter adapter = null;
    private PinnedHeaderListView listView = null;
    private ArrayList<Bitmap> bitmap_array = null;
    //private ProgressDialog pDialog;

    private SwipeRefreshLayout refresh_layout = null;

    private int post_count = 15;

    private String url = null;

    public TabHouseFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.tab_house, container, false);

        bitmap_array = new ArrayList<>();
        //create PinnedHeaderListView adpater.
        adapter = new HeaderListViewAdapter(inflater, bitmap_array);
        listView = (PinnedHeaderListView) view.findViewById(R.id.tab_house_listview);
        listView.setPinHeaders(true);
        // TODO: API starts from 21. Need to change it. Consider to use NestedScrollView.
        //listView.setNestedScrollingEnabled(true);
        listView.setAdapter(adapter);

        //pull to refresh, set 'refresh' listener.
        refresh_layout = (SwipeRefreshLayout) view.findViewById(R.id.tab_house_refresh);
        refresh_layout.setOnRefreshListener(this);

        return view;
    }

    @Override
    public void onRefresh(){
        final PHHttpClient client = PHHttpClient.getInstance(activity);

        JSONObject jdata = new JSONObject();
        //Request a JSON response from getting user info url.
        PHJsonRequest req = new PHJsonRequest(Request.Method.GET,
                "/user/get/", jdata,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String user_id = "";
                        try {
                            JSONObject user = response.getJSONObject("user");
                            String username = user.getString("username");
                            //change user name to adapter.

                        }  catch (JSONException e) {
                            toastWarning("syntax_error");
                        }

                        refreshPostFeed(client, user_id);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        toastWarning("get user info error");
                        refresh_layout.setRefreshing(false);
                    }
                }
        );
        // Add the request to the RequestQueue.
        client.send(req);
    }


    private void refreshPostFeed (PHHttpClient client, final String user_id) {
        JSONObject jdata = new JSONObject();

        //Request a JSON response from getting post url.
        PHJsonRequest req = new PHJsonRequest(Request.Method.GET,
                "/post/get/", jdata,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray posts = response.getJSONArray("posts");
                            // size of user posts.
                            int len = posts.length();
                            String urls = "";
                            //traverse all post feed photos and load post from network asynchronously.
                            for (int i=0; i<len; ++i) {
                                JSONObject post = posts.getJSONObject(i);
                                JSONObject image = post.getJSONObject("image");
                                url = image.getString("src");
                                String base_url = "http://" + AppConfig.SERVER_IP + ":" + AppConfig.SERVER_PORT;
                                url = base_url + url;
                                //Async task LoadImage.
                                new LoadImage(i).execute(url);
                            }
                            toastWarning("get feed posts number: " + len + ":\n" + urls);

                        }  catch (JSONException e) {
                            toastWarning("parse json posts array error");
                        }
                        //set refresh pic visible.
                        refresh_layout.setRefreshing(false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        toastWarning("get feed posts url error");
                        refresh_layout.setRefreshing(false);
                    }
                }
        );
        client.send(req);
    }

    //Async task LoadImage(i).execute(url).
    private class LoadImage extends AsyncTask<String, String, Bitmap> {

        private int pos = 0;

        public LoadImage(int position){
            this.pos = position;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //pDialog = new ProgressDialog(activity);
            //pDialog.setMessage("Loading Image ....");
            //pDialog.show();
        }

        protected Bitmap doInBackground(String... args) {
            Bitmap bmap = null;
            try {
                bmap = BitmapFactory.decodeStream((InputStream) new URL(args[0]).getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bmap;
        }

        protected void onPostExecute(Bitmap image) {
            if(image != null){
                Log.d("MYDEBUG", "image bitmap != null");
                int sz = bitmap_array.size();
                Log.d("MYDEBUG", "sz == " + sz + ", pos == " + pos);
                if (sz <= pos) {
                    int idx = sz;
                    while(idx <= pos) {
                        bitmap_array.add(null);
                        idx++;
                    }
                }
                bitmap_array.set(pos, image);
                adapter.notifyDataSetChanged();
            }else{
                //pDialog.dismiss();
                Toast.makeText(activity, "Image does not exist or network error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = this.getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void toastWarning(String txt) {
        UserWarning.warn(this.getActivity(), txt);
    }

    public interface OnFragmentInteractionListener {

    }
}
