package com.example.todolist.update;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.example.todolist.BuildConfig;
import com.example.todolist.R;
import com.example.todolist.core.ReleaseInfo;
import com.example.todolist.core.ReleaseNotes;

/**
 * The "Update" dialog: shows the current step of {@link UpdateController} (checking, up to date,
 * available, downloading with progress, ready to install, error) and the matching buttons.
 */
public class UpdateDialogFragment extends DialogFragment {

    private static final String TAG = "UpdateDialog";
    private static final int NOTES_MAX_CHARS = 800;

    private UpdateController controller;
    private TextView titleView;
    private TextView messageView;
    private TextView notesView;
    private ProgressBar progressBar;
    private Button negativeButton;
    private Button positiveButton;

    /** Opens the dialog unless it is already open. */
    public static void show(@NonNull FragmentManager fragmentManager) {
        if (fragmentManager.isStateSaved() || fragmentManager.findFragmentByTag(TAG) != null) return;
        new UpdateDialogFragment().show(fragmentManager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update, null);
        titleView = view.findViewById(R.id.updateTitle);
        messageView = view.findViewById(R.id.updateMessage);
        notesView = view.findViewById(R.id.updateNotes);
        progressBar = view.findViewById(R.id.updateProgress);
        negativeButton = view.findViewById(R.id.updateNegative);
        positiveButton = view.findViewById(R.id.updatePositive);

        controller = UpdateController.get(requireContext());
        // A fresh look for the latest release, unless a download/verified APK is already in progress.
        UpdateController.Phase phase = currentPhase();
        if (phase == UpdateController.Phase.IDLE
                || phase == UpdateController.Phase.UP_TO_DATE
                || phase == UpdateController.Phase.ERROR) {
            controller.check(false);
        }
        controller.getState().observe(this, this::render);

        return new AlertDialog.Builder(requireContext()).setView(view).create();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Coming back from the "install unknown apps" settings screen.
        if (controller != null) controller.refreshPermission(requireContext());
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (controller != null) controller.resetIfFinished();
    }

    private UpdateController.Phase currentPhase() {
        UpdateController.State s = controller.getState().getValue();
        return s == null ? UpdateController.Phase.IDLE : s.phase;
    }

    private void render(@NonNull UpdateController.State state) {
        ReleaseInfo release = state.release;
        progressBar.setVisibility(View.GONE);
        notesView.setVisibility(View.GONE);
        positiveButton.setVisibility(View.GONE);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setText(R.string.update_close);
        negativeButton.setOnClickListener(v -> dismiss());

        switch (state.phase) {
            case IDLE:
            case CHECKING:
                titleView.setText(R.string.update_checking_title);
                messageView.setText(R.string.update_checking_message);
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
                break;

            case UP_TO_DATE:
                titleView.setText(R.string.update_up_to_date_title);
                messageView.setText(getString(R.string.update_up_to_date_message, BuildConfig.VERSION_NAME));
                break;

            case AVAILABLE:
                if (release == null) break;
                titleView.setText(getString(R.string.update_available_title, release.versionName()));
                messageView.setText(availableMessage(release));
                showNotes(release);
                negativeButton.setText(R.string.update_later);
                positiveButton.setText(R.string.update_action);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setOnClickListener(v -> controller.download());
                break;

            case DOWNLOADING:
                titleView.setText(R.string.update_downloading_title);
                messageView.setText(getString(R.string.update_downloading_message, state.percent));
                progressBar.setIndeterminate(state.percent <= 0);
                progressBar.setMax(100);
                progressBar.setProgress(state.percent);
                progressBar.setVisibility(View.VISIBLE);
                negativeButton.setText(R.string.action_cancel);
                negativeButton.setOnClickListener(v -> controller.cancelDownload());
                break;

            case READY:
                titleView.setText(R.string.update_ready_title);
                messageView.setText(R.string.update_ready_message);
                negativeButton.setText(R.string.update_later);
                positiveButton.setText(R.string.update_install);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setOnClickListener(v -> controller.install(requireContext()));
                break;

            case NEEDS_PERMISSION:
                titleView.setText(R.string.update_permission_title);
                messageView.setText(R.string.update_permission_message);
                negativeButton.setText(R.string.update_later);
                positiveButton.setText(R.string.update_open_settings);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setOnClickListener(v -> openInstallSettings());
                break;

            case ERROR:
                titleView.setText(R.string.update_error_title);
                messageView.setText(errorMessage(state.errorKind));
                if (release != null && canRetryDownload(state.errorKind)) {
                    positiveButton.setText(R.string.update_retry);
                    positiveButton.setVisibility(View.VISIBLE);
                    positiveButton.setOnClickListener(v -> controller.download());
                } else if (release == null) {
                    positiveButton.setText(R.string.update_retry);
                    positiveButton.setVisibility(View.VISIBLE);
                    positiveButton.setOnClickListener(v -> controller.check(false));
                }
                break;
        }
    }

    private String availableMessage(ReleaseInfo release) {
        String size = release.apkSize > 0
                ? Formatter.formatShortFileSize(requireContext(), release.apkSize)
                : getString(R.string.update_size_unknown);
        return getString(R.string.update_available_message, BuildConfig.VERSION_NAME, release.versionName(), size);
    }

    private void showNotes(ReleaseInfo release) {
        String notes = ReleaseNotes.toPlainText(release.body, NOTES_MAX_CHARS);
        if (notes.isEmpty()) return;
        notesView.setText(getString(R.string.whats_new) + "\n" + notes);
        notesView.setVisibility(View.VISIBLE);
    }

    private static boolean canRetryDownload(@Nullable UpdateException.Kind kind) {
        // A different signing key or an older build will not fix itself on a second download.
        return kind != UpdateException.Kind.SIGNATURE && kind != UpdateException.Kind.NOT_NEWER
                && kind != UpdateException.Kind.NO_APK;
    }

    private int errorMessage(@Nullable UpdateException.Kind kind) {
        if (kind == null) return R.string.update_error_generic;
        switch (kind) {
            case NETWORK:
                return R.string.update_error_network;
            case RATE_LIMIT:
                return R.string.update_error_rate_limit;
            case NO_APK:
                return R.string.update_error_no_apk;
            case NO_SPACE:
                return R.string.update_error_no_space;
            case CHECKSUM:
                return R.string.update_error_checksum;
            case INVALID_APK:
                return R.string.update_error_invalid_apk;
            case NOT_NEWER:
                return R.string.update_error_not_newer;
            case SIGNATURE:
                return R.string.update_error_signature;
            case BAD_RESPONSE:
            default:
                return R.string.update_error_generic;
        }
    }

    private void openInstallSettings() {
        try {
            startActivity(controller.unknownSourcesSettingsIntent(requireContext()));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.update_error_generic, Toast.LENGTH_LONG).show();
        }
    }
}
