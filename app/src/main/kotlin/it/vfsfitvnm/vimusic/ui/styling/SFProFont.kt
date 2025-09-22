package it.vfsfitvnm.vimusic.ui.styling

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import it.vfsfitvnm.vimusic.R

/**
 * SF Pro font configuration for Apple Music style lyrics
 */
val SFProFontFamily = FontFamily(
    Font(R.font.sf_pro_text_regular, FontWeight.Normal),
    Font(R.font.sf_pro_text_medium, FontWeight.Medium),
    Font(R.font.sf_pro_text_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_text_bold, FontWeight.Bold),
    Font(R.font.sf_pro_text_heavy, FontWeight.Black)
)