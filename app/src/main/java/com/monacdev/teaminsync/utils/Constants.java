package com.monacdev.teaminsync.utils;

public final class Constants {
    private Constants() {}

    /* Firebase and data model related constants */
    public static final String EMAIL_KEY_STRING = "email";
    public static final String NAME_KEY_STRING = "name";
    public static final String SURNAME_KEY_STRING = "surname";
    public static final String BIRTHDATE_KEY_STRING = "birth_date";
    public static final String ROLE_KEY_STRING = "role";
    public static final String PROFILE_PIC_KEY_STRING = "profile_pic";
    public static final String TEAM_KEY_STRING = "team";
    public static final String MEMBERS_KEY_STRING = "membri";
    public static final String USERS_REFERENCE_STRING = "users";
    public static final String TEAMS_REFERENCE_STRING = "teams";
    public static final String PENDING_REQUESTS_KEY_STRING = "pending_requests";
    public static final String LEAGUE_KEY_STRING = "league_name";
    public static final String ADDRESS_KEY_STRING = "address";
    public static final String STADIUM_KEY_STRING = "stadium";
    public static final String LOGO_KEY_STRING = "logo";
    public static final String USERNAME_KEY_STRING = "username";
    public static final String TRAINING_TITLE_KEY_STRING = "title";
    public static final String TRAINING_TYPE_KEY_STRING = "type";
    public static final String TRAINING_TARGET_KEY_STRING = "target";
    public static final String TRAINING_DUE_TO_KEY_STRING = "due_to";
    public static final String TRAINING_COMPLETED_KEY_STRING = "completed";
    public static final String TRAINING_UUID_KEY_STRING = "training_uuid";
    public static final String TRAININGS_REFERENCE_STRING = "trainings";
    public static final String NOTIFICATIONS_REFERENCE_STRING = "notifications";

    /* Navigation related constants */
    public static final String LOGGED_USER_EXTRA_STRING = "logged_user_id";
    public static final String DISPLAYED_USER_EXTRA_STRING = "DISPLAYED_USER";
    public static final String DISPLAYED_USER_SURNAME_EXTRA_STRING = "DISPLAYED_USER_SURNAME";
    public static final String REG_WIZARD_TAG = "REGISTRATION_WIZARD";
    public static final String TEAM_ID_TAG = "TEAM_ID";
    public static final String TRAINING_CREATION_WIZARD_TAG = "TRAINING_CREATION_WIZARD";
    public static final String TRAINING_TRACKER_TAG = "TRAINING_TRACKER";

    /* Numeric constants */
    public static final int INVALID_SELECTION = -1;
    public static final int DEFAULT_SELECTION_INDEX = 0;

    /* Textual constants */
    public static final String PLAYER_ROLE_STRING = "player";
    public static final String COACH_ROLE_STRING = "coach";
    public static final String CHRONO_TRAINING_STRING = "chrono";
    public static final String REPS_TRAINING_STRING = "reps";
    public static final String EFFECT_DISMISS = "dismiss";
    public static final String EFFECT_NEXT = "clear_fields";
    public static final String LOGOUT_CANCELED_TAG = "logout_canceled";
    public static final String LOGOUT_CANCELED_MSG = "Logout dialog dismissed but user has not logged out";

    /* Constants for Notifications management */
    public static final String NOTIFICATIONS_CHANNEL_ID = "teaminsync_notifications";
    /* Persistence related constants */
    public static final String SHARED_PREFERENCES_STRING = "teaminsync_sharedprefs";
}
