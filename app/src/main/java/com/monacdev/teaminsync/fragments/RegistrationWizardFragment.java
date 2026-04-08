package com.monacdev.teaminsync.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.activities.MainActivity;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.utils.CloudinaryManager;
import com.monacdev.teaminsync.constants.Constants;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class RegistrationWizardFragment extends BottomSheetDialogFragment {
    private ImageView profilePicIV;
    private EditText usernameET;
    private EditText nameET;
    private EditText surnameET;
    private EditText birthDateET;
    private RadioGroup roleRadioGroup;
    private Spinner teamSelectorSpinner;
    private Button submitBtn;
    private String loggedUserEmail;
    private Uri selectedImageUri = null; // In case the user selects an existing photo
    private byte[] cameraImageBytes = null; // In case the user takes a new photo from camera
    private boolean isEditMode;
    private final HashMap<String, String> editingData = new HashMap<>();
    private final ArrayList<String> teamNames = new ArrayList<>();
    private final ArrayList<String> teamIDs = new ArrayList<>();

    private final ActivityResultLauncher<Intent> deviceGalleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                getActivity();
                if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                    RegistrationWizardFragment.this.selectedImageUri = result.getData().getData();
                    RegistrationWizardFragment.this.cameraImageBytes = null;
                    RegistrationWizardFragment.this.profilePicIV.setImageURI(RegistrationWizardFragment.this.selectedImageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                getActivity();
                if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getExtras() != null){
                    Bitmap capturedPhoto = (Bitmap) result.getData().getExtras().get("data");
                    RegistrationWizardFragment.this.profilePicIV.setImageBitmap(capturedPhoto);
                    /* Conversion of the image for the upload on Cloudinary */
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    if(capturedPhoto != null) {
                        capturedPhoto.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
                        RegistrationWizardFragment.this.cameraImageBytes = byteStream.toByteArray();
                        RegistrationWizardFragment.this.selectedImageUri = null;
                    }
                }
            }
    );

    public static RegistrationWizardFragment newInstance(String userEmail) {
        final RegistrationWizardFragment fragment = new RegistrationWizardFragment();
        final Bundle args = new Bundle();
        args.putString(KeyStrings.EMAIL, userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    public static RegistrationWizardFragment newEditingInstance(HashMap<String, String> userData){
        final RegistrationWizardFragment fragment = new RegistrationWizardFragment();
        final Bundle args = new Bundle();
        for(String key : userData.keySet()){
            args.putString(key, userData.get(key));
        }
        args.putBoolean(Constants.EDITING_MODE_STRING, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.isEditMode = getArguments().getBoolean(Constants.EDITING_MODE_STRING, false);
            if(this.isEditMode){
                this.editingData.put(KeyStrings.USERNAME, getArguments().getString(KeyStrings.USERNAME));
                this.editingData.put(KeyStrings.NAME, getArguments().getString(KeyStrings.NAME));
                this.editingData.put(KeyStrings.SURNAME, getArguments().getString(KeyStrings.SURNAME));
                this.editingData.put(KeyStrings.BIRTHDATE, getArguments().getString(KeyStrings.BIRTHDATE));
                this.editingData.put(KeyStrings.PROFILE_PIC, getArguments().getString(KeyStrings.PROFILE_PIC));
                this.editingData.put(KeyStrings.TEAM, getArguments().getString(KeyStrings.TEAM));
            } else {
                this.loggedUserEmail = getArguments().getString(KeyStrings.EMAIL);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration_wizard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /* Binding the views to their respective objects */
        this.bindViewsToObjects(view);
        /* Setting up the dropdown menu for the teams */
        this.setupTeamSelector();
        if(this.isEditMode) {
            /* In case we are in edit-mode, pre-compile the fields */
            this.fieldPreCompilation(view);
        }
        /* Setting the listener for the Submit button */
        this.submitBtn.setOnClickListener(view1 -> {
            if(!validateFieldCompilation()){
                Toast.makeText(getContext(), R.string.empty_form_fields,Toast.LENGTH_SHORT).show();
                return;
            }
            if(!validateBirthDateInput(birthDateET.getText().toString().trim())){
                Toast.makeText(getContext(), R.string.invalid_date,Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedTeam;
            int selectedTeamID = validateTeamSelection();
            if(selectedTeamID == Constants.INVALID_SELECTION){
                Toast.makeText(getContext(), R.string.no_team_selected, Toast.LENGTH_SHORT).show();
                return;
            }
            /* if all the form validations checks passed, then we proceed to store the data */
            selectedTeam = teamIDs.get(selectedTeamID);
            if(RegistrationWizardFragment.this.isEditMode){
                HashMap<String, Object> updateMap = new HashMap<>();
                updateMap.put(KeyStrings.NAME, RegistrationWizardFragment.this.nameET.getText().toString().trim());
                updateMap.put(KeyStrings.SURNAME, RegistrationWizardFragment.this.surnameET.getText().toString().trim());
                updateMap.put(KeyStrings.BIRTHDATE, RegistrationWizardFragment.this.birthDateET.getText().toString().trim());
                updateMap.put(KeyStrings.TEAM, selectedTeam);
                RegistrationWizardFragment.this.uploadUserOnDatabase(updateMap);
            } else {
                HashMap<String, Object> newUserData = userDataMapping(selectedTeam);
                RegistrationWizardFragment.this.uploadUserWithProfilePicture(newUserData);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     * @param view The view to be taken as reference for the binding
     */
    private void bindViewsToObjects(@NonNull View view){
        this.profilePicIV = view.findViewById(R.id.profilePicIV);
        this.profilePicIV.setOnClickListener(view1 -> RegistrationWizardFragment.this.selectImageSourceDialog());
        this.usernameET = view.findViewById(R.id.usernameET);
        this.nameET = view.findViewById(R.id.nameET);
        this.surnameET = view.findViewById(R.id.surnameET);
        this.birthDateET = view.findViewById(R.id.birthDateET);
        this.roleRadioGroup = view.findViewById(R.id.roleRadioGroup);
        this.teamSelectorSpinner = view.findViewById(R.id.teamSelectorSpinner);
        this.submitBtn = view.findViewById(R.id.submitBtn);
        TextView regWizardTitle = view.findViewById(R.id.regWizardTitle);
        if(this.isEditMode){
            regWizardTitle.setText(R.string.edit_wizard_title);
        } else {
            regWizardTitle.setText(R.string.registration_wizard_title);
        }
    }

    /**
     * Automatically maps the data that has been input in the form to an HashMap representing the user's data structure in the Firebase DB.
     * <br>This method assumes that data such as the birthdate has already been adequately validated
     * @return The HashMap containing the mapped data
     */
    private HashMap<String, Object> userDataMapping(String selectedTeam){
        String role;
        if(this.roleRadioGroup.getCheckedRadioButtonId() == R.id.athleteRadioBtn){
            role = Constants.PLAYER_ROLE_STRING;
        } else {
            role = Constants.COACH_ROLE_STRING;
        }
        HashMap<String, Object> newUserMap = new HashMap<>();
        newUserMap.put(KeyStrings.EMAIL, this.loggedUserEmail);
        newUserMap.put(KeyStrings.NAME, this.nameET.getText().toString().trim());
        newUserMap.put(KeyStrings.SURNAME, this.surnameET.getText().toString().trim());
        newUserMap.put(KeyStrings.BIRTHDATE, this.birthDateET.getText().toString().trim());
        newUserMap.put(KeyStrings.ROLE, role);
        newUserMap.put(KeyStrings.PROFILE_PIC, "");
        newUserMap.put(KeyStrings.TEAM, selectedTeam);
        return newUserMap;
    }

    /**
     * Sets up the Spinner component that holds the team over which the user can select
     */
    private void setupTeamSelector(){
        this.teamNames.add(getResources().getString(R.string.team_selector_label));
        this.teamIDs.add("");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, this.teamNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.teamSelectorSpinner.setAdapter(adapter);
        /* Populating the Spinner component with the teams available on the Firebase DB */
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(ReferenceStrings.TEAMS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot teamSnapshot : snapshot.getChildren()){
                    String teamID = teamSnapshot.getKey();
                    String teamName = teamSnapshot.child(KeyStrings.NAME).getValue(String.class);
                    if(teamName != null){
                        teamNames.add(teamName);
                        teamIDs.add(teamID);
                    }
                }
                adapter.notifyDataSetChanged();
                if(RegistrationWizardFragment.this.isEditMode){
                    String currentTeamID = RegistrationWizardFragment.this.editingData.get(KeyStrings.TEAM);
                    if(currentTeamID != null){
                        int selectedSpinnerPosition = RegistrationWizardFragment.this.teamIDs.indexOf(currentTeamID);
                        if(selectedSpinnerPosition >= 0){
                            RegistrationWizardFragment.this.teamSelectorSpinner.setSelection(selectedSpinnerPosition);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.teams_load_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Validates the selection within the Spinner component and eventually returns the index of the selected team
     * @return The selected team index within the Spinner if valid, -1 otherwise
     */
    private int validateTeamSelection(){
        int selectedTeamID = this.teamSelectorSpinner.getSelectedItemPosition();
        if(selectedTeamID == Constants.DEFAULT_SELECTION_INDEX){
            return Constants.INVALID_SELECTION;
        }
        return selectedTeamID;
    }

    /**
     * Validates the date inserted by the user as the birthdate (validates any valid date format)
     * @param birthDateString the string to be validated
     * @return <strong>true</strong> if the input string represents a valid date string, <strong>false</strong> otherwise
     */
    private boolean validateBirthDateInput(String birthDateString){
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            simpleDateFormat.setLenient(false);
            Date birthDate = simpleDateFormat.parse(birthDateString);
            /* Check if the inserted date is in the future (which will result in an invalid date) */
            return !(birthDate != null && birthDate.after(new Date()));
        } catch(ParseException e) {
            return false;
        }
    }

    /**
     * Checks whether all the fields within the form have been compiled
     * @return <strong>true</strong> if all the fields have been compiled, <strong>false</strong> otherwise
     */
    private boolean validateFieldCompilation(){
        if(this.nameET.getText().toString().isEmpty() || this.surnameET.getText().toString().isEmpty() || this.birthDateET.getText().toString().isEmpty()){
            return false;
        }
        return !(!this.isEditMode && this.usernameET.getText().toString().isEmpty());
    }

    /**
     * Performs the actual upload of the user data on the remote database
     * @param userData the data to be uploaded
     */
    private void uploadUserOnDatabase(HashMap<String, Object> userData){
        if(this.isEditMode){
            String username = this.editingData.get(KeyStrings.USERNAME);
            if(username == null){
                Toast.makeText(getContext(), getString(R.string.error_changes_save), Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseDatabase.getInstance().getReference(ReferenceStrings.USERS).child(username).updateChildren(userData).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    String previousTeam = RegistrationWizardFragment.this.editingData.get(KeyStrings.TEAM);
                    String newTeam = (String) userData.get(KeyStrings.TEAM);
                    if(previousTeam != null && newTeam != null && !previousTeam.equals(newTeam)){
                        DatabaseReference teamsRef = FirebaseDatabase.getInstance().getReference(ReferenceStrings.TEAMS);
                        teamsRef.child(previousTeam).child(KeyStrings.MEMBERS).child(username).removeValue();
                        teamsRef.child(newTeam).child(KeyStrings.MEMBERS).child(username).setValue(true);
                    }
                    Toast.makeText(getContext(), getString(R.string.changes_saved_label), Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().setFragmentResult(NavigationTags.EDIT_FRAGMENT_RESULT, new Bundle());
                    dismiss();
                }
            });
        } else {
            String username = this.usernameET.getText().toString();
            FirebaseDatabase.getInstance().getReference(ReferenceStrings.USERS).child(username).setValue(userData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    /* The newly registered user will be automatically added to the team */
                    FirebaseDatabase.getInstance().getReference(ReferenceStrings.TEAMS)
                            .child(Objects.requireNonNull(userData.get(KeyStrings.TEAM)).toString()).child(KeyStrings.MEMBERS)
                            .child(username).setValue(true);
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    intent.putExtra(IntentExtrasTags.LOGGED_USER, username);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * Gives the user the choice regarding the source from which selecting the image to be set as profile picture
     */
    private void selectImageSourceDialog(){
        String[] options = {getString(R.string.photo_source_camera), getString(R.string.photo_source_gallery)};
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.photo_source_selection_title))
                .setItems(options, (dialogInterface, clickedOption) -> {
                    if(clickedOption == 0){
                        /* Launch camera */
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        RegistrationWizardFragment.this.cameraLauncher.launch(cameraIntent);
                    } else if(clickedOption == 1){
                        /* Launch gallery explorer */
                        Intent galleryExplorerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        RegistrationWizardFragment.this.deviceGalleryLauncher.launch(galleryExplorerIntent);
                    }
                })
                .show();
    }

    /**
     * Manages the user upload on the remote database as well as dealing with the profile picture uploading
     * over the remote object storage
     * @param userData the data to be uploaded
     */
    private void uploadUserWithProfilePicture(HashMap<String, Object> userData){
        if(this.cameraImageBytes == null && this.selectedImageUri == null){
            this.uploadUserOnDatabase(userData);
            return;
        }
        /* If we get to this point, there is a photo that we need to upload to Cloudinary */
        CloudinaryManager cloudinaryManager = new CloudinaryManager();
        UploadCallback cloudinaryCallback = new UploadCallback() {
            @Override public void onStart(String requestId) {}

            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get(Constants.CLOUDINARY_UPLOAD_RESULT_STRING);
                userData.put(KeyStrings.PROFILE_PIC, imageUrl);
                RegistrationWizardFragment.this.uploadUserOnDatabase(userData);
            }


            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(getContext(), getString(R.string.photo_upload_error), Toast.LENGTH_SHORT).show();
            }

            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        };
        if(this.cameraImageBytes != null){
            cloudinaryManager.uploadFromBytes(this.cameraImageBytes, cloudinaryCallback);
        } else {
            cloudinaryManager.uploadFromURI(this.selectedImageUri, cloudinaryCallback);
        }
    }

    /**
     * If we are in editing mode, we have to pre-compile some fields accordingly and hide some other fields
     * @param view the view over which we are working
     */
    private void fieldPreCompilation(View view){
        this.submitBtn.setText(R.string.edit_profile_submit);
        this.nameET.setText(this.editingData.get(KeyStrings.NAME));
        this.surnameET.setText(this.editingData.get(KeyStrings.SURNAME));
        this.birthDateET.setText(this.editingData.get(KeyStrings.BIRTHDATE));
        String profilePicUrl = this.editingData.get(KeyStrings.PROFILE_PIC);
        if(profilePicUrl != null && !profilePicUrl.isEmpty()){
            Glide.with(this).load(profilePicUrl).circleCrop().into(this.profilePicIV);
        }
        this.usernameET.setVisibility(View.GONE);
        this.roleRadioGroup.setVisibility(View.GONE);
        view.findViewById(R.id.selectRoleLabel).setVisibility(View.GONE);
    }
}