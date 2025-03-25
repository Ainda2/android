package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL;
import static mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS;
import static mega.privacy.android.app.utils.Constants.STRING_SEPARATOR;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Spanned;

import androidx.core.text.HtmlCompat;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.domain.usecase.IsEmailValidUseCase;
import timber.log.Timber;

public class TextUtil {

    public static boolean isTextEmpty(String string) {
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    public static boolean isTextEmpty(StringBuilder string) {
        if (string == null)
            return true;

        return isTextEmpty(string.toString());
    }

    /**
     * Method to remove the format placeholders.
     *
     * @param text The string to be processed.
     * @return The processed string.
     */
    public static String removeFormatPlaceholder(String text) {
        try {
            text = text.replace("[A]", "");
            text = text.replace("[/A]", "");
            text = text.replace("[B]", "");
            text = text.replace("[/B]", "");
            text = text.replace("[C]", "");
            text = text.replace("[/C]", "");
        } catch (Exception e) {
            Timber.w(e, "Error replacing text. ");
        }
        return text;
    }

    /**
     * Add the appropriate format in the chat messages.
     *
     * @param context      Current Context object, to get a resource(for example, color) should not use application context, need to pass it from the caller.
     * @param textToShow   The message text
     * @param isOwnMessage If it is a sent or received message
     * @return The formatted text
     */
    public static Spanned replaceFormatChatMessages(Context context, String textToShow, boolean isOwnMessage) {
        String colorStart = ColorUtils.getColorHexString(context, R.color.grey_900_grey_100);
        String colorEnd = isOwnMessage ?
                ColorUtils.getColorHexString(context, R.color.grey_500_grey_400) :
                ColorUtils.getThemeColorHexString(context, com.google.android.material.R.attr.colorSecondary);

        return replaceFormatText(textToShow, colorStart, colorEnd);
    }

    /**
     * Add the appropriate format in the call ended chat messages.
     *
     * @param textToShow The message text
     * @return The formatted text
     */
    public static Spanned replaceFormatCallEndedMessage(String textToShow) {
        try {
            textToShow = textToShow.replace("[A]", "");
            textToShow = textToShow.replace("[/A]", "");
            textToShow = textToShow.replace("[B]", "<font face=\'sans-serif-medium\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
            textToShow = textToShow.replace("[C]", "");
            textToShow = textToShow.replace("[/C]", "");
        } catch (Exception e) {
            Timber.e(e.getStackTrace().toString());
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    /**
     * Add appropriate formatting to text on empty screens with chosen colours.
     *
     * @param textToShow The message text
     * @param colorStart Color
     * @param colorEnd   Color
     * @return The formatted text
     */
    public static Spanned replaceFormatText(String textToShow, String colorStart, String colorEnd) {
        try {
            textToShow = textToShow.replace("[A]", "<font color=" + colorStart + ">");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=" + colorEnd + ">");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            Timber.e(e.getStackTrace().toString());
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    /**
     * Check email validity
     *
     * @param str Email
     * @return Boolean
     * @deprecated <p> Use {@link IsEmailValidUseCase} instead.
     */
    @Deprecated
    public static boolean isEmail(String str) {
        return !isTextEmpty(str) && EMAIL_ADDRESS.matcher(str).matches();
    }

    /**
     * Gets the latest position of a file name before the .extension in order to set the cursor
     * or select the entire file name.
     *
     * @param isFile True if is file, false otherwise.
     * @param text   Current text of the input view.
     * @return The latest position of a file name before the .extension.
     */
    public static int getCursorPositionOfName(boolean isFile, String text) {
        if (isTextEmpty(text)) {
            return 0;
        }

        if (isFile) {
            String[] s = text.split("\\.");
            if (s != null) {
                int numParts = s.length;
                int lastSelectedPos = 0;

                if (numParts > 1) {
                    for (int i = 0; i < (numParts - 1); i++) {
                        lastSelectedPos += s[i].length();
                        lastSelectedPos++;
                    }

                    //The last point should not be selected)
                    lastSelectedPos--;
                    return lastSelectedPos;
                }
            }
        }

        return text.length();
    }

    /**
     * Formats a String of an empty screen.
     *
     * @param context    Current Context object, to get a resource(for example, color)
     *                   should not use application context, need to pass it from the caller.
     * @param textToShow The text to format.
     * @return The string formatted.
     */
    public static Spanned formatEmptyScreenText(Context context, String textToShow) {
        String colorStart = ColorUtils.getColorHexString(context, R.color.grey_900_grey_100);
        String colorEnd = ColorUtils.getColorHexString(context, R.color.grey_900_grey_100);
        return replaceFormatText(textToShow, colorStart, colorEnd);
    }

    /**
     * Gets the string to show as content of a folder.
     *
     * @param numFolders The number of folders the folder contains.
     * @param numFiles   The number of files the folder contains.
     * @return The string so show as content of the folder.
     */
    public static String getFolderInfo(int numFolders, int numFiles, Context context) {
        if (numFolders == 0 && numFiles == 0) {
            return context.getString(R.string.file_browser_empty_folder);
        } else if (numFolders == 0 && numFiles > 0) {
            return context.getResources().getQuantityString(R.plurals.num_files_with_parameter, numFiles, numFiles);
        } else if (numFiles == 0 && numFolders > 0) {
            return context.getResources().getQuantityString(R.plurals.num_folders_with_parameter, numFolders, numFolders);
        } else {
            return context.getResources().getQuantityString(R.plurals.num_folders_num_files, numFolders, numFolders) + context.getResources().getQuantityString(R.plurals.num_folders_num_files_2, numFiles, numFiles);
        }
    }

    /**
     * Gets the string to show as file info details with the next format: "size · date".
     *
     * @param size The file size.
     * @param date The file modification date.
     * @return The string so show as file info details.
     */
    public static String getFileInfo(String size, String date) {
        return String.format("%s · %s", size, date);
    }

    /**
     * If the string received is not null, neither empty, adds a STRING_SEPARATOR at the end.
     *
     * @param text Initial text without separator.
     * @return Text with separator.
     */
    public static String addStringSeparator(String text) {
        return isTextEmpty(text) ? text : text + STRING_SEPARATOR;
    }

    /**
     * Copies some content to the ClipBoard.
     *
     * @param activity   Activity from which the content has to be copied.
     * @param textToCopy Content to copy.
     */
    public static void copyToClipboard(Activity activity, String textToCopy) {
        ClipboardManager clipManager =
                (ClipboardManager) activity.getSystemService(BaseActivity.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText(COPIED_TEXT_LABEL, textToCopy);
        clipManager.setPrimaryClip(clip);
    }
}
