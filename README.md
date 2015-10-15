YoutubeUploadVideoV3sample
==========================

結合youtube上傳與oauth2認證的範例

網誌說明 http://publicwgengineer.blogspot.tw/2014/07/android-youtube.html

Step 1 

register google console Api 

https://console.developers.google.com/project

Step 2 

create google oauth web application

Step 3 

create google oauth app install application

### key setting ###
 * SubmitActivity.class
  
 
 ```java
 // youtube 上傳用
 private static String CLIENT_ID = "782371825438-m89u3lg77pmucgfv69prmkqi972fe0tn.apps.googleusercontent.com";
 
 // Use your own client id
 private static String CLIENT_SECRET = "0ayCIYhJXVNc563S2UZqvyV-";
 
 // Use your own client secret
 private static String REDIRECT_URI = "https://www.example.com/oauth2callback";
 ```
