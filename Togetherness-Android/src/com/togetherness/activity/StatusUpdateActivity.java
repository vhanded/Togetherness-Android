package com.togetherness.activity;

import java.io.ByteArrayOutputStream;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Date;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.togetherness.R;
import com.togetherness.communication.TogethernessServerCommUtil;
import com.togetherness.dm.TogethernessORMLiteHelper;
import com.togetherness.entity.Friends;
import com.togetherness.entity.UserTogetherMap;
import com.togetherness.lazylist.ImageLoader;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: Prashanth
 * Date: 3/31/12
 * Time: 4:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusUpdateActivity extends Activity implements ViewSwitcher.ViewFactory, View.OnClickListener {

    private ImageSwitcher mSwitcher;

    private List<Friends> selectedFriends;
    
    private Bitmap[] friendPhotoBitmap = new Bitmap[0];

    private boolean isTogether = false;

     public void onCreate(Bundle savedInstanceState){
         super.onCreate(savedInstanceState);

         setContentView(R.layout.status_update);

         if(getIntent().getExtras() != null){
             String friendFBID =
                     getIntent().getExtras().getString("facebook_id");

             if(friendFBID != null){
                 populateFriendPhoto(friendFBID);

                 this.selectedFriends = getFriends(friendFBID);
             }
         } else{
             storeSampleBitmap();
            // populateFriendPhotoFromORMLite();
         }

         isTogether = isAlreadyTogether(selectedFriends);

         final Button button = (Button) findViewById(R.id.updateBtn);

         //If already together, then display Apart button
         if(isTogether){
            button.setText("Apart");
         }
         //button.setOnClickListener(this);

        /* Gallery g = (Gallery) findViewById(R.id.gallery);
         g.setAdapter(new ImageAdapter(this));*/

     }


    /**
     * On Click of the Together button, store the Selected User List with Timestamp to ORMLite (Local DB on device)
     * and send the request to Server.
     * @param v
     */
    public void onClick(View v){


        if(!isTogether){
             updateTogetherStatus();
        }else{
             updateApartStatus();
        }

    }

    /**
     * Updates the Together Status
     */
    private void updateTogetherStatus(){

        List<UserTogetherMap> userTogetherMapList = new ArrayList<UserTogetherMap>();

        //Get Message entered by the user
        String msg =  ((EditText)findViewById(R.id.StatusUpdate)).getText().toString();

        //Get Current Time
        Date date = GregorianCalendar.getInstance().getTime();

        for(Friends friend: selectedFriends){
            UserTogetherMap userTogetherMap = new UserTogetherMap();
            userTogetherMap.setFbUserId(friend.getLoggedInUserId());
            userTogetherMap.setTogetherUserId(friend.getFriendsFBID());
            userTogetherMap.setTogetherMessage((msg != null) ? msg : "");
            userTogetherMap.setTogetherStatus("I");
            userTogetherMap.setTogetherTimestamp(date);

            userTogetherMapList.add(userTogetherMap);
        }

        TogethernessORMLiteHelper togethernessORMLiteHelper =
                OpenHelperManager.getHelper(this, TogethernessORMLiteHelper.class);

        togethernessORMLiteHelper.storeUserTogetherMap(userTogetherMapList);

        SharedPreferences setting = getSharedPreferences("TogethernessSetting", 0);
        long fbUserToken = setting.getLong("fbAccessToken", 0);

        TogethernessServerCommUtil  serverComUtil = new TogethernessServerCommUtil(fbUserToken, userTogetherMapList);
        serverComUtil.execute();

        OpenHelperManager.releaseHelper();

    }

    /**
     * Post Apart msg to Server & also store it in local DB.
     */
    private void updateApartStatus(){
        TogethernessORMLiteHelper togethernessORMLiteHelper =
                OpenHelperManager.getHelper(this, TogethernessORMLiteHelper.class);

        List<UserTogetherMap> updatedUserTogethernessList = togethernessORMLiteHelper.updateAndComputeApartStatus(selectedFriends);

        SharedPreferences setting = getSharedPreferences("TogethernessSetting", 0);
        long fbUserToken = setting.getLong("fbAccessToken", 0);

        TogethernessServerCommUtil  serverComUtil = new TogethernessServerCommUtil(fbUserToken, updatedUserTogethernessList);
        serverComUtil.execute();

        OpenHelperManager.releaseHelper();

    }

    public View makeView() {
        ImageView i = new ImageView(this);
       // i.setBackgroundColor(0xFF000000);
        i.setBackgroundColor(android.R.color.white);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(Gallery.LayoutParams.MATCH_PARENT,
                Gallery.LayoutParams.MATCH_PARENT));
        return i;
    }


    private void populateFriendPhoto(String fbUserID){
        /*TogethernessORMLiteHelper togethernessORMLiteHelper =
                OpenHelperManager.getHelper(this, TogethernessORMLiteHelper.class);

        friendPhotoBitmap = new Bitmap[friendsArray.length];

        for(int i = 0; i < friendsArray.length; i++){
            Friends friend = (Friends)friendsArray[i];
            List<Friends> friendPhoto = togethernessORMLiteHelper.fetchFriendByID(friend.getFriendsFBID());

            if(friendPhoto != null){
                friendPhotoBitmap[i] = parseByteArrayToBitmap(((Friends)friendPhoto.get(0)).getFriendPhoto());
            }
        }*/
        ImageLoader imgLoader = new ImageLoader(this.getApplicationContext(), 200, true);
        ImageView imgView = (ImageView)findViewById(R.id.imgView);
        imgLoader.DisplayImage("https://graph.facebook.com/"+fbUserID+"/picture?type=large", imgView);

        OpenHelperManager.releaseHelper();
    }

    /**
     * This method checks if the Logged In user is already Together with a Friend.
     * If they are already together, then the button will be displayed as "Apart", else it will display as "Together"
     * @param friendsList
     * @return
     */
    public boolean isAlreadyTogether(List<Friends> friendsList){

        boolean isTogether = false;

//        TogethernessORMLiteHelper togethernessORMLiteHelper =
//                OpenHelperManager.getHelper(this, TogethernessORMLiteHelper.class);
//
//        for(Friends friend: friendsList){
//            List<UserTogetherMap> userTogetherMap = togethernessORMLiteHelper.fetchUserTogetherMapByID(friend.getFriendsFBID());
//            if(userTogetherMap != null && userTogetherMap.size() > 0){
//                String togetherStatus = ((UserTogetherMap)userTogetherMap.get(0)).getTogetherStatus();
//                
//                if(togetherStatus != null && togetherStatus.trim().equalsIgnoreCase("I")){
//                    isTogether = true;
//                    break;
//                }
//            }
//        }

        return isTogether;
    }

    private void storeSampleBitmap(){
        Bitmap bbicon=BitmapFactory.decodeResource(getResources(),R.drawable.sample_thumb_0);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bbicon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        Friends friends = new Friends();
        friends.setFriendsFBID("test");
        friends.setFriendPhoto(byteArray);

        List<Friends> list = new ArrayList<Friends>();
        list.add(friends);

        selectedFriends = list;

        TogethernessORMLiteHelper togethernessORMLiteHelper =
                OpenHelperManager.getHelper(this, TogethernessORMLiteHelper.class);

        togethernessORMLiteHelper.storeFriendPhoto(list);
        OpenHelperManager.releaseHelper();
    }

    private Bitmap parseByteArrayToBitmap(byte[] friendPhotoArray){
        return BitmapFactory.decodeByteArray(friendPhotoArray, 0, friendPhotoArray.length);
    }
    
    private List<Friends> getFriends(String fbUserID){
        List<Friends> friendsList = new ArrayList<Friends>();

        Friends friend = new Friends();
        friend.setLoggedInUserId(fbUserID);

        friendsList.add(friend);

        return friendsList;
    }
}
