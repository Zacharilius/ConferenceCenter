package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.google.devrel.training.conference.service.OfyService.factory;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Announcement;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.domain.Session;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.form.SessionQueryForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = {
        Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @param profileForm
     *            A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException
     *             when the User object is null.
     */

    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that
    // conforms to the fields defined in ProfileForm
    // TODO 1 Pass the ProfileForm parameter
    // TODO 2 Pass the User parameter
    public Profile saveProfile(final User user, ProfileForm profileForm)
            throws UnauthorizedException {

        // TODO 2
        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO 2
        // Get the userId and mainEmail
        String mainEmail = user.getEmail();
        String userId = user.getUserId();

        // TODO 1
        // Get the displayName and teeShirtSize sent by the request.

        String displayName = profileForm.getDisplayName();
        TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();

        // Get the Profile from the datastore if it exists
        // otherwise create a new one
        Profile profile = ofy().load().key(Key.create(Profile.class, userId))
                .now();

        if (profile == null) {
            // Populate the displayName and teeShirtSize with default values
            // if not sent in the request
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(user
                        .getEmail());
            }
            if (teeShirtSize == null) {
                teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
            }
            // Now create a new Profile entity
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        } else {
            // The Profile entity already exists
            // Update the Profile entity
            profile.update(displayName, teeShirtSize);
        }

        // TODO 3
        // Save the entity in the datastore
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException
     *             when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // load the Profile Entity
        String userId = user.getUserId();
        Key key = Key.create(Profile.class, userId);

        Profile profile = (Profile) ofy().load().key(key).now();
        return profile;
    }
    /**
     * Gets the Profile entity for the current user
     * or creates it if it doesn't exist
     * @param user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(user.getUserId(),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }

    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        final String userId = user.getUserId();
        Key<Profile> profileKey = Key.create(Profile.class, userId);
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);
        final long conferenceId = conferenceKey.getId();
        final Queue queue = QueueFactory.getDefaultQueue();
        
        // Start transactions
        Conference conference = ofy().transact(new Work<Conference>(){
        	@Override
        	public Conference run(){
                Profile profile = getProfileFromUser(user);
        		Conference conference = new Conference(conferenceId, userId, conferenceForm);
                ofy().save().entities(profile, conference).now();
                queue.add(ofy().getTransaction(),
                		TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                		.param("email",  profile.getMainEmail())
                		.param("conferenceInfo", conference.toString()));
                return conference;
        	}
        }); 
        return conference;
    }
    
    
    /**
     * Queries the datastore for conferences.
     *
     * @return A list of conference objects
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm){
    	Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery().list();
    	List<Conference> result = new ArrayList<>(0);
    	List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
    	for(Conference conference : conferenceIterable){
    		organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerDisplayName()));
    		result.add(conference);
    	}
    	// To avoid separate datastore gets for each Conference, pre-fetch the Profiles
    	ofy().load().keys(organizersKeyList);
    	return result;
    
    }
    /**
     * Queries the datastore for conferences the user created
     *
     * @return A list of conference objects
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user)throws UnauthorizedException {
    	if (user == null) {
    		throw new UnauthorizedException("Authorization required");
    	}
    	String userId = user.getUserId();
        Key key = Key.create(Profile.class, userId);
        
        Query<Conference> query = ofy().load().type(Conference.class).ancestor(key);//.order("name");
    	return query.list();
    }
    
    
    
    public List<Conference> getConferencesPlayground()throws UnauthorizedException {
    	Query<Conference> query = ofy().load().type(Conference.class).order("maxAttendees");
    	query = query.filter("city = ", "London");
    	query = query.filter("topics =", "Medical Innovations");
    	query = query.filter("month =", 6);
    	query = query.filter("maxAttendees > ", 10).order("name");
    	return query.list();
    }
    /**
     * Returns a Conference object with the given conferenceId.
     *
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return a Conference object with the given conferenceId.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "getConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.GET
    )
    public Conference getConference(
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException("No Conference found with key: " + websafeConferenceKey);
        }
        return conference;
    }


 /**
     * Just a wrapper for Boolean.
     * We need this wrapped Boolean because endpoints functions must return
     * an object instance, they can't return a Type class such as
     * String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

  /**
     * Register to attend the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "registerForConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.POST
    )

    public WrappedBoolean registerForConference(final User user,
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException,
            ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = user.getUserId();

        // TODO
        // Start transaction
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {

                // TODO
                // Get the conference key -- you can get it from websafeConferenceKey
                // Will throw ForbiddenException if the key cannot be created
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                // TODO
                // Get the Conference entity from the datastore
                Conference conference = ofy().load().key(conferenceKey).now();

                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new WrappedBoolean (false,
                            "No Conference found with key: "
                                    + websafeConferenceKey);
                }

                // TODO
                // Get the user's Profile entity
                Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

                // Has the user already registered to attend this conference?
                if (profile.getConferenceKeysToAttend().contains(
                        websafeConferenceKey)) {
                    return new WrappedBoolean (false, "Already registered");
                } else if (conference.getSeatsAvailable() <= 0) {
                    return new WrappedBoolean (false, "No seats available");
                } else {
                    // All looks good, go ahead and book the seat
                    
                    // TODO
                    // Add the websafeConferenceKey to the profile's
                    // conferencesToAttend property
                    profile.addToConferenceKeysToAttend(websafeConferenceKey);
                    
                    // TODO 
                    // Decrease the conference's seatsAvailable
                    // You can use the bookSeats() method on Conference
                    conference.bookSeats(1);
 
                    // TODO
                    // Save the Conference and Profile entities
                    ofy().save().entities(conference, profile);
                    // We are booked!
                    return new WrappedBoolean(true, "Registration successful");
                }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else if (result.getReason() == "Already registered") {
                throw new ConflictException("You have already registered");
            }
            else if (result.getReason() == "No seats available") {
                throw new ConflictException("There are no seats available");
            }
            else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }


 /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getConferencesToAttend",
            path = "getConferencesToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // TODO
        // Get the Profile entity for the user
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now(); // Change this;
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }

        // TODO
        // Get the value of the profile's conferenceKeysToAttend property
        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend(); // change this

        // TODO
        // Iterate over keyStringsToAttend,
        // and return a Collection of the
        // Conference entities that the user has registered to attend
    	List<Key<Conference>> conferenceKeyList = new ArrayList<>(0);
    	for(String keyString : keyStringsToAttend){
    		conferenceKeyList.add(Key.<Conference>create(keyString));
    	}
        return ofy().load().keys(conferenceKeyList).values();
    }
    @ApiMethod(
    	name = "unregisterFromConference",
    	path = "conference/websafeConferenceKey/registration",
    	httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromConference(
    	final User user,
    	@Named("websafeConferenceKey")
    	final String websafeConferenceKey)
    	throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException
    {
    	if(user == null){
    		throw new UnauthorizedException("Authorization Required");
    	}

        // Get the userId
        final String userId = user.getUserId();

        // TODO
        // Start transaction
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {

                // TODO
                // Get the conference key -- you can get it from websafeConferenceKey
                // Will throw ForbiddenException if the key cannot be created
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                // TODO
                // Get the Conference entity from the datastore
                Conference conference = ofy().load().key(conferenceKey).now();

                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new WrappedBoolean (false,
                            "No Conference found with key: "
                                    + websafeConferenceKey);
                }

                // TODO
                // Get the user's Profile entity
                Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

                // Has the user already registered to attend this conference?
                if (!profile.getConferenceKeysToAttend().contains(
                        websafeConferenceKey)) {
                    return new WrappedBoolean (false, "You didn't register for the conference");
                } else {
                    // All looks good, go ahead and remove the seat
                    
                    // TODO
                    // Add the websafeConferenceKey to the profile's
                    // conferencesToAttend property
                    profile.unregisterFromConference(websafeConferenceKey);
                    
                    // TODO 
                    // Increase the conference's seatsAvailable
                    // You can use the bookSeats() method on Conference
                    conference.giveBackSeats(1);
 
                    // TODO
                    // Save the Conference and Profile entities
                    ofy().save().entities(conference, profile);
                    // We are booked!
                    return new WrappedBoolean(true, "Deregistration successful");
                }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else if (result.getReason() == "You didn't register for the conference") {
                throw new ConflictException("You already aren't registered for this  conference");
            }
            else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }
    @ApiMethod(
    		name = "getAnnouncement",
    		path = "announcement",
    		httpMethod = HttpMethod.GET
    )
    public Announcement getAnnouncement(){
    	MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
    	Object message = memcacheService.get(Constants.MEMCACHE_ANNOUNCEMENTS_KEY);
    	if(message != null){
    		return new Announcement(message.toString());
    	}
    	return null;
    }
    
    
    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
    		name = "createSession", 
    		path = "conference/{websafeConferenceKey}/session", 
    		httpMethod = HttpMethod.POST
    )
    public Session createSession(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey, final SessionForm sessionForm)
        throws UnauthorizedException {
    	// Check if the user is logged in. 
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        
        // Commented because unnecessary
        // final String conferenceId = user.getUserId();
        
        // Get the conferenceKey. 
        Key<Conference> conferenceKey = Key.create(Conference.class, websafeConferenceKey);
        
        // Create a sessionKey
        final Key<Session> sessionKey = factory().allocateId(conferenceKey, Session.class);
        
        // Get the id from the sessionKey
        final long sessionId = sessionKey.getId();
        
        // Create a queue object by fetching the default Push Queue
        //final Queue queue = QueueFactory.getDefaultQueue();
        
        // Start transactions
        Session session = ofy().transact(new Work<Session>(){
        	@Override
        	public Session run(){
        		// Profile profile = getProfileFromUser(user);
        		// Load the corresponding conference object from datastore
        		Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
                Conference conference = ofy().load().key(conferenceKey).now();
                String conferenceId = Objects.toString(conference.getId());
                
                System.out.println("conferenceId: " + conferenceId);
                System.out.println("conferenceId: " + conferenceKey.getId());
                System.out.println("conferenceKey: " + conferenceKey.toString());
                System.out.println("conference.getName(): " + conference.getName());

                Session session = new Session(sessionId, websafeConferenceKey, sessionForm);
                ofy().save().entities(conference, session).now();

                return session;
        	}
        }); 
        return session;
    }
    /**
     * Queries the datastore for sessions given a conference
     *
     * @return A list of session objects
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getSessionsCreated",
            path = "conference/{websafeConferenceKey}/getSessionsCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Session> getSessionsCreated(@Named("websafeConferenceKey") 
    		final String websafeConferenceKey) {

        Key<Conference> key = Key.create(websafeConferenceKey);
        System.out.println("key: " + key.getString());
        Conference c = ofy().load().key(key).now();
        System.out.println("c: " + c.getName());
        //Query<Conference> query = ofy().load().type(Conference.class).ancestor(key).order("name");

        Query<Session> query = ofy().load().type(Session.class).ancestor(key).order("name");
    	return query.list();
    }
    /**
     * Queries the datastore for sessions given a conference and given the sessionType param
     *
     * @return A list of session objects
     * @throws UnauthorizedException when the user is not signed in.
     */
    
    @ApiMethod(
            name = "getSessionsCreatedByType",
            path = "conference/{websafeConferenceKey}/getSessionsCreatedByType",
            httpMethod = HttpMethod.POST
    )
    public List<Session> getConferenceSessionsByType(@Named("websafeConferenceKey") 
    		final String websafeConferenceKey, @Named("sessionType") String sessionType) {
        Key<Conference> key = Key.create(websafeConferenceKey);
        Query<Session> query = ofy().load().type(Session.class).ancestor(key).order("name");
        query = query.filter("typeOfSession =", sessionType);
        
        return query.list();

    }
    /**
     * Queries the datastore for sessions given a conference and given the speaker param
     *
     * @return A list of session objects
     * @throws UnauthorizedException when the user is not signed in.
     */
    
    @ApiMethod(
            name = "getSessionsCreatedBySpeaker",
            path = "conference/getSessionsCreatedBySpeaker",
            httpMethod = HttpMethod.POST
    )
    public List<Session> getConferenceSessionsBySpeaker(@Named("speaker") String speaker) {
        Query<Session> query = ofy().load().type(Session.class);
        query = query.filter("speaker =", speaker);
        
        return query.list();

    }
    /**
     * Queries the datastore for sessions given a conference and given the sessionType param
     *
     * @return A list of session objects
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getSessionsCreatedQuery",
            path = "conference/{websafeConferenceKey}/getSessionsCreatedQuery",
            httpMethod = HttpMethod.POST
    )
    public List<Session> getSessionsCreatedQuery(@Named("websafeConferenceKey") 
    		final String websafeConferenceKey, SessionQueryForm sessionQueryForm) {

    	Iterable<Session> sessionIterable = sessionQueryForm.getQuery().list();
    	List<Session> result = new ArrayList<>(0);
    	List<Key<Conference>> conferencesKeyList = new ArrayList<>(0);
    	for(Session session : sessionIterable){
    		Key<Conference> key = Key.create(websafeConferenceKey);
    		conferencesKeyList.add(key);
    		result.add(session);
    	}
    	// To avoid separate datastore gets for each Conference, pre-fetch the Profiles
    	ofy().load().keys(conferencesKeyList);
    	return result;
    } 
    
/**
 * Add a session to a user's personal wishlist
 *
 * @param user An user who invokes this method, null when the user is not signed in.
 * @param websafeSessionKey The String representation of the Session Key.
 * @return Boolean true when success, otherwise false
 * @throws UnauthorizedException when the user is not signed in.
 * @throws NotFoundException when there is no Conference with the given conferenceId.
 */
@ApiMethod(
        name = "addSessionToWishlist",
        path = "conference/getSessionsCreated/{websafeSessionKey}/addSessionToWishlist",
        httpMethod = HttpMethod.POST

)

public WrappedBoolean addSessionToWishlist(final User user,
        @Named("websafeSessionKey") final String websafeSessionKey)
        throws UnauthorizedException, NotFoundException,
        ForbiddenException, ConflictException {
    // If not signed in, throw a 401 error.
    if (user == null) {
        throw new UnauthorizedException("Authorization required");
    }

    // Get the userId
    final String userId = user.getUserId();

    // TODO
    // Start transaction
    WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
        @Override
        public WrappedBoolean run() {
            try {

            // TODO
            // Get the conference key -- you can get it from websafeSessionKey
            // Will throw ForbiddenException if the key cannot be created
            Key<Session> sessionKey = Key.create(websafeSessionKey);

            // TODO
            // Get the Conference entity from the datastore
            Session session = ofy().load().key(sessionKey).now();

            // 404 when there is no Conference with the given conferenceId.
            if (session == null) {
                return new WrappedBoolean (false,
                        "No Conference found with key: "
                                + websafeSessionKey);
            }

            // TODO
            // Get the user's Profile entity
            Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

            // Has the user already registered to attend this conference?
            if (profile.getSessionWishlist().contains(
                    websafeSessionKey)) {
                return new WrappedBoolean (false, "Already added");
            } else {
                // All looks good, go ahead and book the seat
                
                // TODO
                // Add the websafeSessionKey to the profile's
                // conferencesToAttend property
                profile.addSessionWishLislist(websafeSessionKey);
                

                // TODO
                // Save the Conference and Profile entities
                ofy().save().entities(session, profile);
                // We are booked!
                return new WrappedBoolean(true, "Session added to wishlist");
            }

            }
            catch (Exception e) {
                return new WrappedBoolean(false, "Unknown exception");
            }
        }
    });
    // if result is false
    if (!result.getResult()) {
        if (result.getReason().contains("No Session found with key")) {
            throw new NotFoundException (result.getReason());
        }
        else if (result.getReason() == "Already added") {
            throw new ConflictException("You have already added the session");
        }
        else {
            throw new ForbiddenException("Unknown exception");
        }
    }
    return result;
}
/**
 * Returns a collection of Sessions Object of all sessions that the user is attending in a given conference
 *
 * @param user An user who invokes this method, null when the user is not signed in.
 * @return a Collection of Conferences that the user is going to attend.
 * @throws UnauthorizedException when the User object is null.
 */
@ApiMethod(
        name = "getSessionsInWishlist",
        path = "conference/{websafeConferenceKey}/getSessionsInWishlist",
        httpMethod = HttpMethod.GET
)
public Collection<Session> getSessionsInWishlist(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
        throws UnauthorizedException, NotFoundException {
    // If not signed in, throw a 401 error.
    if (user == null) {
        throw new UnauthorizedException("Authorization required");
    }
    // TODO
    // Get the Profile entity for the user
    Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now(); // Change this;
    if (profile == null) {
        throw new NotFoundException("Profile doesn't exist.");
    }

    // TODO
    // Get the value of the profile's sessionWishlist property
    List<String> keyStringsWishlist = profile.getSessionWishlist();

    // TODO
    // Iterate over keyStringsToAttend,
    // and return a Collection of the
    // Conference entities that the user has registered to attend
	List<Key<Session>> sessionKeyList = new ArrayList<>(0);
	for(String keyString : keyStringsWishlist){
		Key<Session> sessionKey = Key.<Session>create(keyString);
		Session session = ofy().load().key(sessionKey).now();
		String sessionConferenceKey = session.getConferenceKey().getString();
		if(sessionConferenceKey.equals(websafeConferenceKey)){
			sessionKeyList.add(sessionKey);
		}
	}
    return ofy().load().keys(sessionKeyList).values();
}
/**
 * Queries the datastore for the featured speaker
 *
 * @return A list of session objects
 * @throws UnauthorizedException when the user is not signed in.
 */

@ApiMethod(
        name = "getFeaturedSpeaker",
        path = "conference/getFeaturedSpeaker",
        httpMethod = HttpMethod.GET
)
public List<Session> getFeaturedSpeaker() {
    Query<Session> query = ofy().load().type(Session.class);
    query = query.filter("speaker =", "Featured Speaker");
    
    return query.list();

}

}