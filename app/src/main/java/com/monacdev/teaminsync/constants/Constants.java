package com.monacdev.teaminsync.constants;

public final class Constants {
    private Constants() {}

    public static final String EDITING_MODE_STRING = "editing_mode";

    /* Numeric constants */
    public static final int INVALID_SELECTION = -1;
    public static final int DEFAULT_SELECTION_INDEX = 0;
    public static final int MSG_UPDATE_TIMER = 1;
    public static final int MSG_TRAINING_TARGET_REACHED = 2;
    public static final int DATE_PART_YEAR = 0;
    public static final int DATE_PART_MONTH = 1;
    public static final int DATE_PART_DAY = 2;
    public static final int NO_TEAM_SELECTED = -1;

    /* Textual constants */
    public static final String PLAYER_ROLE_STRING = "player";
    public static final String COACH_ROLE_STRING = "coach";
    public static final String CHRONO_TRAINING_STRING = "chrono";
    public static final String REPS_TRAINING_STRING = "reps";
    public static final String EFFECT_DISMISS = "dismiss";
    public static final String EFFECT_NEXT = "clear_fields";
    public static final String LOGOUT_CANCELED_MSG = "Logout dialog dismissed but user has not logged out";
    public static final String CLOUDINARY_PRESET_STRING = "TeamInSync";
    public static final String CLOUDINARY_UPLOAD_RESULT_STRING = "secure_url";
    public static final String CLOUDINARY_CLOUD_NAME_KEY = "cloud_name";
    public static final String CLOUDINARY_CLOUD_NAME_STRING = "dw7b93heu";

    /* Persistence related constants */
    public static final String SHARED_PREFERENCES_STRING = "teaminsync_sharedprefs";
}
