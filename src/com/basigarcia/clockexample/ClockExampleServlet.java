package com.basigarcia.clockexample;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

//kill -15 $(lsof -i TCP:8888 | grep java | grep LISTEN | awk '{ print $2 }') kills a project server
@SuppressWarnings("serial")
public class ClockExampleServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		SimpleDateFormat fmt = new SimpleDateFormat("yyy-MM-dd hh:mm:ss.SSSSSS");
		fmt.setTimeZone(new SimpleTimeZone(0, ""));
		
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		String loginUrl = userService.createLoginURL("/");
		String logoutUrl = userService.createLogoutURL("/");
		
		//Retrieve the user preference entity by reconstructing the key UserPrefs and calling get, then gets the tzOffset from the entity
		Entity userPrefs = null;
		if(user != null){
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

			//Use memcache to speed the app
			MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
			String cacheKey = "UserPrefs:" + user.getUserId();
			userPrefs = (Entity) memcache.get(cacheKey);

			if(userPrefs == null){
				Key userKey = KeyFactory.createKey("UserPrefs", user.getUserId());
				try{
					userPrefs = ds.get(userKey);
					memcache.put(cacheKey, userPrefs);
				}
				catch(EntityNotFoundException e){
					//No user preferences stored
				}
			}
		}
		
		if(userPrefs != null){
			int tzOffset = ((Long) userPrefs.getProperty("tz_offset")).intValue();
			fmt.setTimeZone(new SimpleTimeZone(tzOffset*60*60*1000, ""));
			req.setAttribute("tzOffset",  tzOffset);
		}
		else{
			req.setAttribute("tzOffset", 0);
		}

		req.setAttribute("user",  user);;
		req.setAttribute("loginUrl", loginUrl);
		req.setAttribute("logoutUrl", logoutUrl);
		req.setAttribute("currentTime", fmt.format(new Date()));

		//Handle the server response, in this case it is an html string
		resp.setContentType("text/html");
		
		//ClockServlet invokes home.jsp and expects attributes from user, loginUrl...
		RequestDispatcher jsp = req.getRequestDispatcher("/WEB-INF/home.jsp");
		jsp.forward(req, resp);
		
	}
}
