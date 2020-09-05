package com.gifttome.gifttome;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.TimelineResult;
import com.twitter.sdk.android.tweetui.UserTimeline;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import twitter4j.Status;
import twitter4j.TwitterFactory;


public class AvailablePostsFragment extends Fragment implements ItemClickListener, View.OnClickListener{
    private View thisFragment;
    private UserTimeline userTimeline;
    private String username;

    private RecyclerView recyclerView;
    private MyAdapter nAdapter;
    ArrayList<AvailableObjectsData> avObData = new ArrayList<>();
    private MainActivity mainActivity;
    private EditText searchText;
    private Button searchButton;//todo searching


    public AvailablePostsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        thisFragment = inflater.inflate(R.layout.fragment_available_posts, container, false);
        mainActivity = (MainActivity) getActivity();
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("shared preferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);
        Log.i("usernamefromshared", "onCreateView: " + username);
        inizializzazione();
        TwitterConfig config = new TwitterConfig.Builder(requireContext())
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig("fud09hdnKuTT7PtYNuCZn2tRV", "gqzr3e1Rlz4noKtuhIytOBgfzjsJGSPNiMqmQO0quby2ycs1lp"))
                .debug(true)
                .build();
        Twitter.initialize(config);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        nAdapter = new MyAdapter(avObData,  this);
        recyclerView.setAdapter(nAdapter);

        searchText = thisFragment.findViewById(R.id.search_text);
        searchButton = thisFragment.findViewById(R.id.search_button1);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFilteredPosts();

            }
        });


        //bearTokenTwitter();
        makeTwitterRequest("true");
        //getAvailablePostsTweets();
        nAdapter.notifyDataSetChanged();

        return thisFragment;
    }


    private void inizializzazione() {

        recyclerView = thisFragment.findViewById(R.id.my_recycler_view);
    }

    @Override
    public void onClick(View view, int position) {
        Toast.makeText(getActivity(), "before gototchat"+ position, Toast.LENGTH_SHORT).show();
        //in teoria se clicchi ti manda al chat fragment todo
        Toast.makeText(getActivity(), String.valueOf(view.getRootView().getId()), Toast.LENGTH_SHORT).show();
        goToChatFragment();
    }

    public void goToChatFragment(){
        RepliesToMeFragment newChatFragment = new RepliesToMeFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, newChatFragment)
                .addToBackStack(null)
                .commit();
    }

    public ArrayList<AvailableObjectsData> getAvailablePostsTweets() {
        userTimeline = new UserTimeline.Builder()
                .screenName("GiftToME5")
                .includeRetweets(false)
                .maxItemsPerRequest(200)
                .build();
        userTimeline.next(null, callback);
        return avObData;
    }

    Callback<TimelineResult<Tweet>> callback = new Callback<TimelineResult<Tweet>>()
    {
        @Override
        public void success(Result<TimelineResult<Tweet>> searchResult)
        {
            List<Tweet> tweets = searchResult.data.items;
            long maxId = 0;
            avObData.clear();
            MainActivity mainActivity = (MainActivity) getActivity();

            for (Tweet tweet : tweets){
                String jsonString = tweet.text; //Here is the body
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);

                    //sceglie i post che non hanno lo stesso username dell'utente
                    String username1 = jsonObject.get("issuer").toString();
                    Log.v("credUsernameAPF", username1);
                    Log.v("credUsernameAPFSP", username);

                    if(!username1.equals(username)) {
                        String name1 = jsonObject.get("name").toString();
                        Log.v("cred", name1);

                        String userId = jsonObject.get("id").toString();
                        String category1 = jsonObject.get("category").toString();
                        double lat1 = Double.parseDouble(jsonObject.get("lat").toString());
                        double lon1 = Double.parseDouble(jsonObject.get("lon").toString());
                        String description1 = jsonObject.get("description").toString();
                        Log.i("idnotvalid", userId);
                        AvailableObjectsData newPost = new AvailableObjectsData(name1, username1, UUID.fromString(userId), category1, lat1, lon1, description1);
                        newPost.setTwitterId(tweet.getId());
                        if (!name1.equals("") && !username1.equals("")) {
                            avObData.add(newPost);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                maxId = tweet.id;
                Log.v("TagSuccc","str");

            }
            if (mainActivity != null)
                mainActivity.addNewAvailablePost(avObData);
            nAdapter.notifyDataSetChanged();
            Log.v("credd", String.valueOf(avObData.size()));

            //da ricontrollare
            if (searchResult.data.items.size() == 100) {
                userTimeline.previous(maxId, callback);
            }
        }
        @Override
        public void failure(TwitterException error)
        {
            Log.e("TAG","Error");
        }
    };

    interface RepositoryCallback {
        void onComplete(ArrayList<AvailableObjectsData> result);
    }

    public void makeTwitterRequest(String text) {
        GetTwitterInBackground gtib = new GetTwitterInBackground(mainActivity.executorService);
        gtib.makeRequest(text, new RepositoryCallback() {
            @Override
            public void onComplete(ArrayList<AvailableObjectsData> result) {
                Log.i("oncompletearray", "onComplete: " + result.size());
/*
                avObData.clear();
                avObData.addAll(result);
                MainActivity mainActivity = (MainActivity) getActivity();
                assert mainActivity != null;
                mainActivity.addNewAvailablePost(avObData);
 */
            }
        });
    }

    @Override
    public void onClick(View v) {
        int position = v.getId();
        AvailableObjectsData objectInterestedIn = avObData.get(position);
        dialogCreateReply(objectInterestedIn);

    }

    private void dialogCreateReply(AvailableObjectsData objectInterestedIn) {
        //costruisco un dialog per la risposta
        final EditText edittext = new EditText(getContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Scrivi il testo della risposta");
        builder.setMessage("Enter Your Message");
        builder.setView(edittext);
        builder.setPositiveButton("INVIA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Reply reply =  new Reply(UUID.randomUUID(), username, objectInterestedIn.getId(),objectInterestedIn.getIssuer(), edittext.getText().toString());

                try {
                    Status tweetStat = postOnTwitter(reply.formatToString());
                    if(tweetStat != null) {
                        reply.setTwitterId(tweetStat.getId());
                        Toast.makeText(getActivity(), "Risposta avvenuta con successo", Toast.LENGTH_SHORT).show();
                    }
                } catch (twitter4j.TwitterException | JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("annulla", null);
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public class GetTwitterInBackground {

        private final Executor executor;

        public GetTwitterInBackground(Executor executor) {
            this.executor = executor;
        }

        public void makeRequest(final String text, final RepositoryCallback callback){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ArrayList<AvailableObjectsData> result = getAvailablePostsTweets();
                        callback.onComplete(result);

                    } catch (Exception ignored) {

                    }
                }
            });
        }

    }

    private ArrayList<AvailableObjectsData> getFilteredPosts() {
            userTimeline = new UserTimeline.Builder()
                    .screenName("GiftToME5")
                    .includeRetweets(false)
                    .maxItemsPerRequest(200)
                    .build();
            userTimeline.next(null, filteredCallback);
            return avObData;
    }

    Callback<TimelineResult<Tweet>> filteredCallback = new Callback<TimelineResult<Tweet>>()
    {
        @Override
        public void success(Result<TimelineResult<Tweet>> searchResult)
        {
            List<Tweet> tweets = searchResult.data.items;
            long maxId = 0;
            avObData.clear();

            for (Tweet tweet : tweets){
                String jsonString = tweet.text;
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    String filterText = searchText.getText() == null ? "" : searchText.getText().toString();
                    if(!jsonString.contains(filterText))
                        continue;
                    //sceglie i post che non hanno lo stesso username dell'utente
                    String username1 = jsonObject.get("issuer").toString();

                    if(!username1.equals(username)) {
                        String name1 = jsonObject.get("name").toString();
                        Log.v("cred", name1);

                        String userId = jsonObject.get("id").toString();
                        String category1 = jsonObject.get("category").toString();
                        double lat1 = Double.parseDouble(jsonObject.get("lat").toString());
                        double lon1 = Double.parseDouble(jsonObject.get("lon").toString());
                        String description1 = jsonObject.get("description").toString();
                        Log.i("idnotvalid", userId);
                        AvailableObjectsData newPost = new AvailableObjectsData(name1, username1, UUID.fromString(userId), category1, lat1, lon1, description1);
                        newPost.setTwitterId(tweet.getId());
                        if (!name1.equals("") && !username1.equals("")) {
                            avObData.add(newPost);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                maxId = tweet.id;
            }
            if (mainActivity != null)
            nAdapter.notifyDataSetChanged();

            //da ricontrollare
            if (searchResult.data.items.size() == 100) {
                userTimeline.previous(maxId, callback);
            }
        }
        @Override
        public void failure(TwitterException error)
        {
            Log.e("TAG","Error");
        }
    };


    public Status postOnTwitter(String text) throws twitter4j.TwitterException {

        twitter4j.Twitter twitter = TwitterFactory.getSingleton();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Status status = twitter.updateStatus(text);
        System.out.println("Successfully updated the status to [" + status.getText() + "].");

        return status;
    }
}