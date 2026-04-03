package com.monacdev.teaminsync.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.activities.MainActivity;
import com.monacdev.teaminsync.utils.CloudinaryManager;
import com.monacdev.teaminsync.utils.Constants;

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
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


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
    private final ArrayList<String> teamNames = new ArrayList<>();
    private final ArrayList<String> teamIDs = new ArrayList<>();

    private final ActivityResultLauncher<Intent> deviceGalleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == getActivity().RESULT_OK && result.getData() != null){
                        RegistrationWizardFragment.this.selectedImageUri = result.getData().getData();
                        RegistrationWizardFragment.this.cameraImageBytes = null;
                        RegistrationWizardFragment.this.profilePicIV.setImageURI(RegistrationWizardFragment.this.selectedImageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == getActivity().RESULT_OK && result.getData() != null && result.getData().getExtras() != null){
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
            }
    );

    public static RegistrationWizardFragment newInstance(String userEmail) {
        final RegistrationWizardFragment fragment = new RegistrationWizardFragment();
        final Bundle args = new Bundle();
        args.putString(Constants.EMAIL_KEY_STRING, userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.loggedUserEmail = getArguments().getString(Constants.EMAIL_KEY_STRING);
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
        /* Setting the listener for the Submit button */
        this.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                HashMap<String, Object> newUserData = userDataMapping(selectedTeam);
                //uploadUserOnDatabase(newUserData);
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
        this.profilePicIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RegistrationWizardFragment.this.selectImageSourceDialog();
            }
        });
        this.usernameET = view.findViewById(R.id.usernameET);
        this.nameET = view.findViewById(R.id.nameET);
        this.surnameET = view.findViewById(R.id.surnameET);
        this.birthDateET = view.findViewById(R.id.birthDateET);
        this.roleRadioGroup = view.findViewById(R.id.roleRadioGroup);
        this.teamSelectorSpinner = view.findViewById(R.id.teamSelectorSpinner);
        this.submitBtn = view.findViewById(R.id.submitBtn);
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
        newUserMap.put(Constants.EMAIL_KEY_STRING, this.loggedUserEmail);
        newUserMap.put(Constants.NAME_KEY_STRING, this.nameET.getText().toString().trim());
        newUserMap.put(Constants.SURNAME_KEY_STRING, this.surnameET.getText().toString().trim());
        newUserMap.put(Constants.BIRTHDATE_KEY_STRING, this.birthDateET.getText().toString().trim());
        newUserMap.put(Constants.ROLE_KEY_STRING, role);
        newUserMap.put(Constants.PROFILE_PIC_KEY_STRING, "");
        newUserMap.put(Constants.TEAM_KEY_STRING, selectedTeam);
        return newUserMap;
    }

    /**
     * Sets up the Spinner component that holds the team over which the user can select
     */
    private void setupTeamSelector(){
        this.teamNames.add(getResources().getString(R.string.team_selector_label));
        this.teamIDs.add("");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, this.teamNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.teamSelectorSpinner.setAdapter(adapter);
        /* Populating the Spinner component with the teams available on the Firebase DB */
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.TEAMS_REFERENCE_STRING);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot teamSnapshot : snapshot.getChildren()){
                    String teamID = teamSnapshot.getKey();
                    String teamName = teamSnapshot.child(Constants.NAME_KEY_STRING).getValue(String.class);
                    if(teamName != null){
                        teamNames.add(teamName);
                        teamIDs.add(teamID);
                    }
                }
                adapter.notifyDataSetChanged();
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
            if(birthDate != null && birthDate.after(new Date())){
                return false;
            }
            return true;
        } catch(ParseException e) {
            return false;
        }
    }

    /**
     * Checks whether all the fields within the form have been compiled
     * @return <strong>true</strong> if all the fields have been compiled, <strong>false</strong> otherwise
     */
    private boolean validateFieldCompilation(){
        if(this.nameET.getText().toString().isEmpty() || this.usernameET.getText().toString().isEmpty() || this.surnameET.getText().toString().isEmpty() || this.birthDateET.getText().toString().isEmpty()){
            return false;
        }
        return true;
    }

    /**
     * Performs the actual upload of the user data on the remote database
     * @param userData the data to be uploaded
     */
    private void uploadUserOnDatabase(HashMap<String, Object> userData){
        String username = this.usernameET.getText().toString();
        FirebaseDatabase.getInstance().getReference(Constants.USERS_REFERENCE_STRING).child(username).setValue(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    /* The newly registered user will NOW be automatically added to the pending requests */
                    String sectionString;
                    if(roleRadioGroup.getCheckedRadioButtonId() == R.id.athleteRadioBtn){
                        sectionString = Constants.PENDING_REQUESTS_KEY_STRING;
                    } else {
                        sectionString = Constants.MEMBERS_KEY_STRING;
                    }
                    FirebaseDatabase.getInstance().getReference(Constants.TEAMS_REFERENCE_STRING)
                            .child(userData.get(Constants.TEAM_KEY_STRING).toString()).child(sectionString)
                            .child(username).setValue(true);
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    intent.putExtra(Constants.LOGGED_USER_EXTRA_STRING, username);
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Gives the user the choice regarding the source from which selecting the image to be set as profile picture
     */
    private void selectImageSourceDialog(){
        String[] options = {getString(R.string.photo_source_camera), getString(R.string.photo_source_gallery)};
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.photo_source_selection_title))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int clickedOption) {
                        if(clickedOption == 0){
                            /* Launch camera */
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            RegistrationWizardFragment.this.cameraLauncher.launch(cameraIntent);
                        } else if(clickedOption == 1){
                            /* Launch gallery explorer */
                            Intent galleryExplorerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            RegistrationWizardFragment.this.deviceGalleryLauncher.launch(galleryExplorerIntent);
                        }
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
                userData.put(Constants.PROFILE_PIC_KEY_STRING, imageUrl);
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
}