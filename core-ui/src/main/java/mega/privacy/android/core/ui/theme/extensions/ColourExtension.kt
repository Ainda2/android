package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.amber_300
import mega.privacy.android.core.ui.theme.amber_700
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.blue_200
import mega.privacy.android.core.ui.theme.blue_400
import mega.privacy.android.core.ui.theme.dark_blue_200
import mega.privacy.android.core.ui.theme.dark_blue_500
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.green_400
import mega.privacy.android.core.ui.theme.green_500
import mega.privacy.android.core.ui.theme.grey_020
import mega.privacy.android.core.ui.theme.grey_050
import mega.privacy.android.core.ui.theme.grey_100
import mega.privacy.android.core.ui.theme.grey_200
import mega.privacy.android.core.ui.theme.grey_300
import mega.privacy.android.core.ui.theme.grey_600
import mega.privacy.android.core.ui.theme.grey_700
import mega.privacy.android.core.ui.theme.grey_800
import mega.privacy.android.core.ui.theme.grey_900
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_038
import mega.privacy.android.core.ui.theme.grey_alpha_050
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.lime_green_200
import mega.privacy.android.core.ui.theme.lime_green_500
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_400
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.core.ui.theme.red_800
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.core.ui.theme.white_alpha_050
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.core.ui.theme.yellow_300
import mega.privacy.android.core.ui.theme.yellow_600
import mega.privacy.android.core.ui.theme.yellow_700

/**
 * Text Color Primary for Composable
 */
val Colors.textColorPrimary: Color
    get() = if (isLight) dark_grey else white

/**
 * Text Color Secondary for Composable
 */
val Colors.textColorSecondary: Color
    get() = if (isLight) grey_alpha_054 else white_alpha_054

/**
 * Color [grey_alpha_012] when light and [white_alpha_038] when dark
 */
val Colors.grey_alpha_012_white_alpha_038: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_038

/**
 * Color [yellow_600] when light and [yellow_300] when dark
 */
val Colors.yellow_600_yellow_300: Color
    get() = if (isLight) yellow_600 else yellow_300

/**
 * Color [green_500] when light and [green_400] when dark
 */
val Colors.green_500_green_400: Color
    get() = if (isLight) green_500 else green_400

/**
 * Color [lime_green_500] when light and [lime_green_200] when dark
 */
val Colors.lime_green_500_lime_green_200: Color
    get() = if (isLight) lime_green_500 else lime_green_200

/**
 * Color [dark_blue_500] when light and [dark_blue_200] when dark
 */
val Colors.dark_blue_500_dark_blue_200: Color
    get() = if (isLight) dark_blue_500 else dark_blue_200

/**
 * Color [grey_alpha_012] when light and [white_alpha_012] when dark
 */
val Colors.grey_alpha_012_white_alpha_012: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_012

/**
 * Color [blue_400] when light and [blue_200] when dark
 */
val Colors.blue_400_blue_200: Color
    get() = if (isLight) blue_400 else blue_200

/**
 * Color [white_alpha_087] when light and [grey_alpha_087] when dark
 */
val Colors.white_alpha_087_grey_alpha_087: Color
    get() = if (isLight) white_alpha_087 else grey_alpha_087

/**
 * Color [red_800] when light and [red_400] when dark
 */
val Colors.red_800_red_400: Color
    get() = if (isLight) red_800 else red_400

/**
 * Color [grey_alpha_038] when light and [wwhite_alpha_038hite] when dark
 */
val Colors.grey_alpha_038_white_alpha_038: Color
    get() = if (isLight) grey_alpha_038 else white_alpha_038

/**
 * Color [red_600] when light and [red_300] when dark
 */
val Colors.red_600_red_300: Color
    get() = if (isLight) red_600 else red_300

/**
 * Color [grey_alpha_087] when light and [yellow_700] when dark
 */
val Colors.grey_alpha_087_yellow_700: Color
    get() = if (isLight) grey_alpha_087 else yellow_700

/**
 * Color [amber_700] when light and [amber_300] when dark
 */
val Colors.amber_700_amber_300: Color
    get() = if (isLight) amber_700 else amber_300

/**
 * Color [grey_900] when light and [grey_100] when dark
 */
val Colors.grey_900_grey_100: Color
    get() = if (isLight) grey_900 else grey_100

/**
 * Color [grey_300] when light and [grey_600] when dark
 */
val Colors.grey_300_grey_600: Color
    get() = if (isLight) grey_300 else grey_600

/**
 * Color [black] when light and [white] when dark
 */
val Colors.black_white: Color
    get() = if (isLight) black else white

/**
 * Color [red_600] when light and [white_alpha_087] when dark
 */
val Colors.red_600_white_alpha_087: Color
    get() = if (isLight) red_600 else white_alpha_087

/**
 * Color [grey_200] when light and [grey_700] when dark
 */
val Colors.grey_200_grey_700: Color
    get() = if (isLight) grey_200 else grey_700

/**
 * Color for grey when light [grey_020] and when dark [grey_700]
 */
val Colors.grey_020_grey_700: Color
    get() = if (isLight) grey_020 else grey_700

/**
 * Color for grey when light [teal_300] and when dark [teal_200]
 */
val Colors.teal_300_teal_200: Color
    get() = if (isLight) teal_300 else teal_200

/**
 * Color [white] when light and [grey_alpha_087] when dark
 */
val Colors.white_grey_alpha_087: Color
    get() = if (isLight) white else grey_alpha_087

/**
 * Color for grey when light [grey_050] and when dark [grey_800]
 */
val Colors.grey_050_grey_800: Color
    get() = if (isLight) grey_050 else grey_800

/**
 * Color for grey when light [grey_alpha_050] and when dark [white_alpha_050]
 */
val Colors.grey_alpha_050_white_alpha_050: Color
    get() = if (isLight) grey_alpha_050 else white_alpha_050

/**
 * Color [grey_alpha_087] when light and [white_alpha_087] when dark
 */
val Colors.grey_alpha_087_white_alpha_087: Color
    get() = if (isLight) grey_alpha_087 else white_alpha_087

/**
 * Color [black] when light and [white] when dark
 */
val Colors.textColorBlackWhite: Color
    get() = if (isLight) black else white

/**
 * Color for grey when light [grey_020] and when dark [grey_800]
 */
val Colors.grey_020_grey_800: Color
    get() = if (isLight) grey_020 else grey_800

/**
 * Text Color Primary reverted (when light [white] and when dark [dark_grey])
 */
val Colors.textColorPrimaryReverted: Color
    get() = if (isLight) white else dark_grey
