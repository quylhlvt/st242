package com.cute.anime.avatarmaker.utils

//import okhttp3.OkHttpClient
//import okhttp3.Request
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import com.cute.anime.avatarmaker.dialog.DialogRate
import com.facebook.shimmer.Shimmer
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.cute.anime.avatarmaker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


var RATE = "rate"
var RATENUMBER = "rate2"
val shimmer = Shimmer.ColorHighlightBuilder()
    .setDuration(1200)
    .setBaseColor(0xFFCCCCCC.toInt()) // màu nền đậm hơn (xám đậm)
    .setHighlightColor(0xFFFFFFFF.toInt()) // màu highlight sáng (trắng)
    .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
    .setAutoStart(true)
    .build()

fun Activity.shareApp() {
    ShareCompat.IntentBuilder.from(this)
        .setType("text/plain")
        .setChooserTitle("Chooser title")
        .setText("http://play.google.com/store/apps/details?id=" + (this).packageName)
        .startChooser()
}

fun Activity.policy() {
    val url = "https://sites.google.com/view/cute-anime-avatar-maker/"
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(url)
    startActivity(i)
}

fun newIntent(context: Context, cls: Class<*>): Intent {
    return Intent(
        context,
        cls
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}

var unItem: (() -> Unit)? = null
fun Activity.rateUs(i: Int) {
    var dialog = DialogRate(this)
    dialog.init(object : DialogRate.OnPress {
        override fun rating() {
            if (i == 0) {
                unItem?.invoke()
            }
            val manager = ReviewManagerFactory.create(this@rateUs!!)
            var request: Task<ReviewInfo> = manager.requestReviewFlow();
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var reviewInfo = task.result;
                    val flow: Task<Void> = manager.launchReviewFlow((this@rateUs)!!, reviewInfo)
                    dialog.dismiss()
                    flow.addOnCompleteListener { task2 ->
                        if (i == 1) {
                            finishAffinity()
                        }
                    }
                } else {
                    dialog.dismiss()
                    if (i == 1) {
                        finishAffinity()
                    }
                }
            }
        }

        override fun cancel() {
            if (i == 1) {
                finishAffinity()
            }
        }

        override fun later() {
            if (i == 0) {
                unItem?.invoke()
            }
            if (i == 1) {
                finishAffinity()
            }
        }
    })
    if (!SharedPreferenceUtils.getInstance(this).getBooleanValue(RATE)) {
        dialog.show()
    }
}

fun Activity.showSystemUI(white: Boolean = false, fullScreen: Boolean = false) {
    if (fullScreen) {
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT <= 30) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

    } else {
        if (white) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION


        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION


        }
    }
}

fun Activity.backPress(providerSharedPreference: SharedPreferenceUtils) {
    var a = providerSharedPreference.getNumber(RATENUMBER)
    a += 1
    providerSharedPreference.putNumber(RATENUMBER, a)
    if (a % 2 == 0) {
        if (!providerSharedPreference.getBooleanValue(RATE)
        ) {
            rateUs(1)
        } else {
            finishAffinity()

        }
    } else {
        finishAffinity()
    }
}

var currentToast: Toast? = null
fun showToast(context: Context, id: Int) {
    SystemUtils.setLocale(context)
    currentToast?.cancel()
    currentToast = Toast.makeText(context, context.resources.getText(id), Toast.LENGTH_SHORT)
    currentToast?.show()
}

fun changeColor(
    context: Context,
    text: String,
    color: Int,
    fontfamily: Int,
    textSize: Float
): SpannableString {
    val spannableString = SpannableString(text)
    spannableString.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, color)),
        0,
        text.length,
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    val font = ResourcesCompat.getFont(context, fontfamily)
    val typefaceSpan = CustomTypefaceSpan("", font)
    spannableString.setSpan(
        typefaceSpan,
        0,
        text.length,
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    spannableString.setSpan(
        RelativeSizeSpan(textSize),
        0,
        text.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannableString
}

class CustomTypefaceSpan(private val family: String, private val typeface: Typeface?) :
    TypefaceSpan(family) {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, typeface)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, typeface)
    }

    private fun applyCustomTypeFace(paint: Paint, tf: Typeface?) {
        if (tf != null) {
            paint.typeface = tf
        } else {
            paint.typeface = Typeface.DEFAULT
        }
    }
}

fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val services = activityManager.getRunningServices(Integer.MAX_VALUE)
    for (service in services) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

fun dpToPx(dp: Float, context: Context): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics)
}

fun dpToSp(sp: Float, context: Context): Float {
    val floatSize =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
    return floatSize
}

fun Activity.setupWindow() {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    getWindow().setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
//    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
}

fun AppCompatActivity.changeFragment(fragment: Fragment, tag: String?, id: Int) {
    var fragmentManager: FragmentManager? = null
    var transaction: FragmentTransaction? = null
    fragmentManager = supportFragmentManager
    transaction = fragmentManager!!.beginTransaction()
    val existingFragment: Fragment? = fragmentManager!!.findFragmentByTag(tag)
    if (existingFragment != null) {
        transaction!!.replace(id, existingFragment)
    } else {
        transaction!!.replace(id, fragment, tag)
        transaction!!.addToBackStack(tag)
    }
    transaction!!.commit()
}

fun shareFile(context: Context, file: File) {
    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "*/*"
    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(
            Intent.createChooser(intent, "Share file").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

//fun shareImageAsBitmap(context: Context, filePath: String) {
//    // Đọc ảnh từ đường dẫn file thành Bitmap
//    val bitmap = BitmapFactory.decodeFile(filePath) ?: return
//
//    // Tạo file PNG trong cacheDir để share
//    val file = File(context.cacheDir, "shared_image.webp")
//
//    // Lưu bitmap vào file PNG (giữ nền trong suốt)
//    file.outputStream().use { outputStream ->
//        bitmap.compress(Bitmap.CompressFormat.webp, 100, outputStream)
//    }
//
//    // Lấy Uri an toàn từ FileProvider
//    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//
//    // Tạo Intent chia sẻ
//    val intent = Intent(Intent.ACTION_SEND).apply {
//        type = "image/png"
//        putExtra(Intent.EXTRA_STREAM, fileUri)
//        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//    }
//
//    // Mở trình chọn chia sẻ
//    context.startActivity(Intent.createChooser(intent, "Chia sẻ ảnh"))
//}

fun shareListFiles(context: Context, files: List<String>) {
    val fileUris = kotlin.collections.ArrayList<Uri>()
    val authority = "${context.packageName}.provider"

    // Chuyển đổi mỗi File thành Uri thông qua FileProvider
    files.forEach { file ->
        val fileUri = FileProvider.getUriForFile(context, authority, File(file))
        fileUris.add(fileUri)
    }

    // Tạo Intent gửi nhiều tệp
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*" // Chia sẻ đa dạng tệp, có thể thay đổi thành "image/*" nếu chỉ chia sẻ ảnh
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Cho phép ứng dụng khác đọc tệp
    }

    // Kiểm tra có ứng dụng nào xử lý không
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(intent, "Share Files"))
    }
}

fun getAllUriInFileAsset(context: Context, filePaths: ArrayList<String>): ArrayList<Uri> {
    val assetUris = kotlin.collections.ArrayList<Uri>()
    for (filePath in filePaths) {

        var uri = Uri.parse(
            "content://com.keyboard.fonts.emojikeyboard.theme.Provider/" + filePath.replace(
                "file:///android_asset/",
                ""
            )
        )
        assetUris.add(uri)
    }
    return assetUris
}

//fun shareVideoOnFacebook(context: Context, file: File) {
//    // Tạo URI cho file video
//    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//
//    // Tạo intent gửi file với kiểu video
//    val intent = Intent(Intent.ACTION_SEND)
//    intent.type = "video/*"
//    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
//    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//    // Đặt gói ứng dụng là Facebook để chia sẻ trực tiếp lên Facebook
//    intent.setPackage("com.facebook.katana")
//
//    if(isAppInstalled(context,"com.facebook.katana")){
//        context.startActivity(Intent.createChooser(intent, "Share Video on Facebook"))
//
//    }else{
//        Toast.makeText(context, context.getString(R.string.facebook_app_is_not_installed), Toast.LENGTH_SHORT).show()
//    }
//
//
//
//}
//fun shareVideoOnTikTok(context: Context, file: File) {
//    // Tạo URI cho file video
//    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//
//    // Tạo intent gửi file với kiểu video
//    val intent = Intent(Intent.ACTION_SEND)
//    intent.type = "video/*"
//    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
//    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//    intent.setPackage("com.zhiliaoapp.musically")
//
//
//    if(isAppInstalled(context,"com.zhiliaoapp.musically")){
//        context.startActivity(Intent.createChooser(intent, "Share Video on TikTok"))
//    }else{
//        Toast.makeText(context, context.getString(R.string.tikTok_app_is_not_installed), Toast.LENGTH_SHORT).show()
//    }
//}
//fun shareVideoOnTele(context: Context, file: File) {
//    // Tạo URI cho file video
//    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//
//    // Tạo intent gửi file với kiểu video
//    val intent = Intent(Intent.ACTION_SEND)
//    intent.type = "video/*"
//    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
//    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//    // Chỉ định gói ứng dụng Instagram
//    intent.setPackage("org.telegram.messenger")
//    if(isAppInstalled(context,"org.telegram.messenger")){
//        context.startActivity(Intent.createChooser(intent, "Share Video on Telegram"))
//    }else{
//        Toast.makeText(context, context.getString(R.string.telegram_app_is_not_installed), Toast.LENGTH_SHORT).show()
//    }
//}
//fun shareVideoOnWhatsApp(context: Context, file: File) {
//    // Tạo URI cho file video
//    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//
//    // Tạo intent gửi file với kiểu video
//    val intent = Intent(Intent.ACTION_SEND)
//    intent.type = "video/*"
//    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
//    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//    // Kiểm tra nếu WhatsApp đã được cài đặt trên thiết bị
//    val whatsappIntent = Intent(intent).apply {
//        setPackage("com.whatsapp")
//    }
//    if(isAppInstalled(context,"com.whatsapp")){
//        context.startActivity(Intent.createChooser(whatsappIntent, "Share Video on WhatsApp"))
//    }else{
//        Toast.makeText(context, context.getString(R.string.whatsApp_app_is_not_installed), Toast.LENGTH_SHORT).show()
//    }
//}
fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        true // Ứng dụng tồn tại
    } catch (e: PackageManager.NameNotFoundException) {
        Log.d(TAG, "isAppInstalled: $e")
        false // Ứng dụng không tồn tại
    }
}

fun shareMusicUrl(context: Activity, musicUrl: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_TEXT, musicUrl) // Chia sẻ URL
    }
    context.startActivity(Intent.createChooser(intent, "Share Music Link"))
}

fun shareAllAudioFile(context: Activity, imageUris: ArrayList<Uri>) {
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Music"))
}

fun shareAllFile(context: Activity, musicUrls: List<String>, musicUris: List<Uri>) {
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*" // Dùng kiểu chung để gửi cả text & file
        val textParts = kotlin.collections.ArrayList<CharSequence>()
        val fileUris = kotlin.collections.ArrayList<Uri>()

        // Thêm URL vào danh sách chia sẻ
        textParts.addAll(musicUrls)

        // Thêm file nhạc offline
        fileUris.addAll(musicUris)

        // Nếu có URL thì gửi dạng EXTRA_TEXT
        if (textParts.isNotEmpty()) {
            putCharSequenceArrayListExtra(Intent.EXTRA_TEXT, textParts)
        }

        // Nếu có file nhạc thì gửi dạng EXTRA_STREAM
        if (fileUris.isNotEmpty()) {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    }
    context.startActivity(Intent.createChooser(intent, "Share Music"))
}

fun Context.showKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    inputMethodManager!!.showSoftInput(
        view,
        InputMethodManager.SHOW_IMPLICIT
    )
}

fun Context.hideKeyboard(view: View?) {
    view?.let {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}

fun getAllUriInFIleAsset(context: Context, path: String): ArrayList<String> {
    val pathList = arrayListOf<String>()
    try {
        val files = context.assets.list(path) ?: arrayOf()
        for (file in files) {
            val fullPath = "file:///android_asset/$path/$file"
            pathList.add(fullPath)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    Log.d(TAG, "getAllPathInFileAsset: ${pathList.size}")
    return pathList
}

fun getAllFile(folder: File): ArrayList<String> {
    var arr = arrayListOf<String>()
    if (folder.exists()) {
        val files = folder.listFiles()
        files.forEach {
            arr.add(it.path)
        }
    }
    return arr
}

fun pickImage(pickImageLauncher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/*"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }
    pickImageLauncher.launch(intent)
}

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
fun fileToDrawable(context: Context, filePath: String?): Drawable? {
    val file = File(filePath)
    if (!file.exists()) {
        return null
    }
    val bitmap = BitmapFactory.decodeFile(filePath)
    return BitmapDrawable(context.resources, bitmap)
}

fun Intent.putParcelableExtra(key: String, value: Parcelable): Intent {
    return this.putExtra(key, value)
}


fun setWidthHeight(view: View, width: Int, height: Int) {
    val params = view.layoutParams
    if (width != 0) {
        params.width = width
    }
    if (height != 0) {
        params.height = height
    }
    view.layoutParams = params
}

//fun setLayoutParamParent(view: View, top: Float, right: Float, bottom: Float, left: Float) {
//    val params = LinearLayout.LayoutParams(
//        LinearLayout.LayoutParams.MATCH_PARENT, // width
//        LinearLayout.LayoutParams.WRAP_CONTENT // height
//    )
//    val marginTopInPixels = dpToPx(top, view.context).toInt()
//    params.setMargins(0, marginTopInPixels, 0, 0)
//    view.layoutParams = params
//}

fun setLayoutParam(view: View, top: Float, right: Float, bottom: Float, left: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.setMargins(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    view.layoutParams = layoutParams
}

fun changeText(
    context: Context,
    text: String,
    @ColorRes colorRes: Int,
    @FontRes fontFamily: Int
): SpannableString {

    val spannableString = SpannableString(text)

    // resolve color
    val colorInt = ContextCompat.getColor(context, colorRes)
    spannableString.setSpan(
        ForegroundColorSpan(colorInt),
        0,
        text.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    // font
    ResourcesCompat.getFont(context, fontFamily)?.let { font ->
        spannableString.setSpan(
            CustomTypefaceSpan("", font),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    return spannableString
}


var lastClickTime = 0L
fun View.onSingleClick(action: () -> Unit) {
    this.setOnClickListener {
        if (System.currentTimeMillis() - lastClickTime >= 500) {
            action()
            lastClickTime = System.currentTimeMillis()
        }
    }
}
var lastClickTime2 = 0L
fun View.onClickCustom(action: () -> Unit) {
    this.setOnClickListener {
        if (System.currentTimeMillis() - lastClickTime2 >= 100) {
            action()
            lastClickTime2 = System.currentTimeMillis()
        }
    }
}


fun View.onClick(action: () -> Unit) {
    this.setOnClickListener {
        if (System.currentTimeMillis() - lastClickTime >= 1500) {
            action()
            lastClickTime = System.currentTimeMillis()
        }
    }
}

lateinit var documentFile: DocumentFile
fun Context.getUriFromFilePath(path: String): Uri? {
    val contentResolver = contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media._ID)
    val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(path)

    contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val id = cursor.getLong(idIndex)
            return ContentUris.withAppendedId(uri, id)
        }
    }
    return null
}

fun getFilePathFromURI(context: Context, uri: Uri): String? {
    var filePath: String? = null
    if (DocumentsContract.isDocumentUri(context, uri)) {
        // DocumentProvider
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val type = split[0]

        var contentUri: Uri? = null
        if ("image" == type) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else if ("video" == type) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else if ("audio" == type) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val selection = "_id=?"
        val selectionArgs = arrayOf(
            split[1]
        )

        filePath = getDataColumn(context, contentUri, selection, selectionArgs)
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        // MediaStore (and general)
        filePath = getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        filePath = uri.path
    }

    return filePath
}

fun getDataColumn(
    context: Context,
    uri: Uri?,
    selection: String?,
    selectionArgs: Array<String>?
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column
    )

    try {
        cursor = context.contentResolver.query(
            uri!!, projection, selection, selectionArgs,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(column_index)
        }
    } finally {
        cursor?.close()
    }
    return null
}


fun Activity.pathOfFileCreateFromUri(uri: Uri, endFile: String): String? {

    var filePath: String? = null
    var inputStream: InputStream? = null
    try {
        documentFile = DocumentFile.fromSingleUri(applicationContext, uri)!!
        val contentResolver = contentResolver
        inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val file = createTemporalFileFrom(inputStream, documentFile.name!!, endFile)
            filePath = file?.path
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    return filePath
}

val buffer = ByteArray(8 * 1024)
var outputDirectory: File? = null
var outputDir: File? = null
var uploadSuccess = MutableLiveData<Int>()

@Throws(IOException::class)
fun Activity.createTemporalFileFrom(
    inputStream: InputStream,
    name: String,
    endFile: String
): File? {
    outputDirectory = File(applicationContext.filesDir, "Ringtone")
    if (!outputDirectory!!.exists()) {
        outputDirectory!!.mkdirs()
    }
    var outputDir: File? = null
    outputDir = File(outputDirectory, "$name.$endFile")
    if (outputDir!!.exists()) {

    } else {
        try {
            inputStream.use { input ->
                FileOutputStream(outputDir).use { output ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        Log.d(TAG, "createTemporalFileFrom: ${bytesRead}")
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()

                }


            }
        } catch (e: Exception) {
            Log.d(TAG, "createTemporalFileFrom: ${e}")
            e.printStackTrace()
            outputDir = null
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                Log.d(TAG, "createTemporalFileFrom: Close stream error")
                e.printStackTrace()
            }
        }
    }

    uploadSuccess.postValue(1)
    return outputDir
}

fun Activity.setNotificationSound(uri: Uri) {
    RingtoneManager.setActualDefaultRingtoneUri(
        this,
        RingtoneManager.TYPE_NOTIFICATION,
        uri
    )
}

fun Activity.setAlarmSound(uri: Uri) {
    RingtoneManager.setActualDefaultRingtoneUri(
        this,
        RingtoneManager.TYPE_ALARM,
        uri
    )
}


fun Activity.requesPermission(
    requestCode: Int
): Int {   when (requestCode) {
    CONST.REQUEST_STORAGE_PERMISSION -> {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                return CONST.REQUEST_STORAGE_PERMISSION
            }
            // Xóa phần showDialogNotifiListener ở đây
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return CONST.REQUEST_STORAGE_PERMISSION
            }
        }
    }

    CONST.REQUEST_NOTIFICATION_PERMISSION -> {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return CONST.REQUEST_NOTIFICATION_PERMISSION
        }
    }
}
    return 0
}


fun Activity.showDialogNotifiListener(i: Int) {
    SystemUtils.setLocale(this)
    val builder = AlertDialog.Builder(this)
    builder.setTitle(R.string.permission)
        .setMessage(i)
        .setPositiveButton(R.string.tv_yes) { dialog, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.tv_cancel) { dialog, _ ->
            dialog.dismiss()
            showSystemUI()
        }
        .setCancelable(false)
    val alertDialog = builder.create()
    alertDialog.show()
    val negativeButton =
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    val negativeButton2 =
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
    negativeButton2?.setTextColor(resources.getColor(R.color.app_color))
    negativeButton?.setTextColor(resources.getColor(R.color._CCCCCC))
}

fun Activity.checkPermissionCamera(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}


fun startNewActivity(context: Context, cls: Class<*>): Intent {
    return Intent(
        context,
        cls
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}


//CameraCharacteristics.LENS_FACING_BACK    camera sau
//CameraCharacteristics.LENS_FACING_FRONT    camera trước
fun Activity.isCameraAvailable(lensFacing: Int): Boolean {
    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != null && facing == lensFacing) {
                return true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun changeGradientText(textView: AppCompatTextView) {
    textView.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (textView.width > 0 && textView.height > 0) {
                val textShader: Shader = LinearGradient(
                    0f, 0f, textView.width.toFloat(), 0f,
                    intArrayOf(
                        "#FF2626".toColorInt(),
                        "#FF8C27".toColorInt()
                    ), floatArrayOf(0.25f, 1f), Shader.TileMode.CLAMP
                )
                textView.paint.setShader(textShader)
            }
            textView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}

fun changeGradientText2(textView: AppCompatTextView, color: String) {
    textView.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (textView.width > 0 && textView.height > 0) {
                val textShader: Shader = LinearGradient(
                    0f, 0f, textView.width.toFloat(), textView.textSize.toFloat(),
                    intArrayOf(
                        color.toColorInt(),
                        color.toColorInt()
                    ), floatArrayOf(0.25f, 1f), Shader.TileMode.CLAMP
                )
                textView.paint.setShader(textShader)
            }
            textView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}

fun checkPermision(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 33) {
        return Environment.isExternalStorageManager()
    } else {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun requestPer33(context: Context): Boolean {
    return (ActivityCompat.checkSelfPermission(
        context, Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED)
}

fun requestPer26(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

fun checkUsePermision(): Array<String> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return storge_permissions_33
    } else {
        return storge_permissions_30
    }
}

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
var storge_permissions_33 = arrayOf(
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

var storge_permissions_30 = arrayOf(
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
)

fun formatDate(date: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
    return dateFormat.format(date)
}

fun formatDateTime(date: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.ENGLISH)
    return dateFormat.format(date)
}

fun formatDuration(duration: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun getVideoDuration(filePath: String): String {
    try {
        val retriever = MediaMetadataRetriever()
        Log.d(TAG, "getVideoDuration: $filePath")
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        retriever.release()
        return String.format("%02d:%02d", minutes, seconds)
    } catch (e: Exception) {
        return ""
    }
}

fun convertTimeToSeconds(time: String): Int {
    val parts = time.split(":") // Tách chuỗi thành country2 phần
    val minutes = parts[0].toIntOrNull() ?: 0 // Chuyển phút sang Int
    val seconds = parts[1].toIntOrNull() ?: 0 // Chuyển giây sang Int
    return minutes * 60 + seconds // Tính tổng số giây
}

fun getAllVideoFolders(context: Context, checkVideo: Boolean): ArrayList<String> {
    val mediaFolders = arrayListOf<String>()
    if (checkVideo) {
        // Query video
        val videoProjection = arrayOf(
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATA
        )
        val videoUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoCursor: Cursor? = context.contentResolver.query(
            videoUri, videoProjection, null, null, null
        )
        videoCursor?.use {
            val dataColumnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (it.moveToNext()) {
                val videoPath = it.getString(dataColumnIndex)
                val folderPath = File(videoPath).parent

                if (folderPath != null && !mediaFolders.contains(folderPath)) {
                    mediaFolders.add(folderPath)
                }
            }
        }

    } else {
        // Query image
        val imageProjection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.DATA
        )
        val imageUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageCursor: Cursor? = context.contentResolver.query(
            imageUri, imageProjection, null, null, null
        )
        imageCursor?.use {
            val dataColumnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val imagePath = it.getString(dataColumnIndex)
                val folderPath = File(imagePath).parent

                if (folderPath != null && !mediaFolders.contains(folderPath)) {
                    mediaFolders.add(folderPath)
                }
            }
        }
    }

    return mediaFolders
}

fun isImageFile(filePath: String): Boolean {
    val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "webp")
    val fileExtension = File(filePath).extension.lowercase()
    return imageExtensions.contains(fileExtension)
}

fun isVideoFile(filePath: String): Boolean {
    val videoExtensions = listOf("mp4", "mkv", "avi", "3gp", "mov", "flv", "wmv")
    val fileExtension = File(filePath).extension.lowercase()
    return videoExtensions.contains(fileExtension)
}

fun getAllAudioFiles(context: Context): List<String> {
    val audioFiles = mutableListOf<String>()

    val projection = arrayOf(
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )
    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    context.contentResolver.query(
        uri,
        projection,
        selection,
        null,
        null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (cursor.moveToNext()) {
            val name = cursor.getString(nameColumn)
            val data = cursor.getString(dataColumn)
            audioFiles.add("$data")
        }
    }
    return audioFiles
}

fun fileToBytes(file: File): ByteArray {
    val size = file.length().toInt()
    val bytes = ByteArray(size)
    try {
        val buf = BufferedInputStream(FileInputStream(file))
        buf.read(bytes, 0, bytes.size)
        buf.close()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return bytes
}

//fun downloadFile(url: String, fileDir: File, success: (Int) -> Unit) {
//
//    GlobalScope.launch(Dispatchers.IO) {
//        try {
//            val client = OkHttpClient()
//            Log.d(TAG, "downloadFile____: $url")
//            val request = Request.Builder().url(url).build()
//            client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) {
//                    success.invoke(1)
//                } else {
//                    val inputStream: InputStream = response.body!!.byteStream()
//                    val outputStream = FileOutputStream(fileDir)
//                    inputStream.use { input ->
//                        outputStream.use { output ->
//                            input.copyTo(output)
//                        }
//                    }
//                    success.invoke(0)
//                }
//            }
//        } catch (e: Exception) {
//            success.invoke(1)
//            Log.d(TAG, "downloadFile: $e")
//        }
//    }
//}


fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetworkInfo
    return activeNetwork?.isConnectedOrConnecting == true
}
fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}



fun requestDeleteFile(
    activity: Activity,
    path: ArrayList<String>,
    deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    var uriList: ArrayList<Uri> = arrayListOf()
    path.forEach {
        val mediaID: Long = getFilePathToMediaID(it, activity)
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            mediaID
        )
        uriList.add(uri)
    }

    requestDeletePermission(activity, deleteResultLauncher, uriList, path)
}

fun requestDeletePermission(
    activity: Activity,
    deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
    uriList: List<Uri>, path: List<String>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pi = MediaStore.createDeleteRequest(activity.contentResolver, uriList)
        try {
            deleteResultLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    } else {
        path.forEach { File(it).delete() }

    }
}

fun getFilePathToMediaID(path: String, context: Context): Long {
    var id: Long = 0
    val cr = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI // URI cho audio
    val selection = MediaStore.Audio.Media.DATA
    val selectionArgs = arrayOf(path)
    val projection = arrayOf(MediaStore.Audio.Media._ID)
    val cursor = cr.query(uri, projection, "$selection=?", selectionArgs, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) { // Kiểm tra xem cursor có dữ liệu không
            val idIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID) // Dùng getColumnIndexOrThrow để bắt lỗi nếu cột không tồn tại
            id = cursor.getLong(idIndex) // Dùng getLong cho ID kiểu Long
        }
        cursor.close() // Đóng cursor sau khi sử dụng
    }
    return id
}

fun renameFile(
    activity: Activity,
    path: String,
    newName: String,
    renameResultLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    val mediaID: Long = getFilePathToMediaID(path, activity)
    val uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        mediaID
    )
    requestRenamePermission(activity, renameResultLauncher, uri, newName, path)
}

fun requestRenamePermission(
    activity: Activity,
    renameResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
    uri: Uri,
    newName: String,
    path: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pi = MediaStore.createWriteRequest(
                activity.contentResolver,
                listOf(uri)
            ) // Tạo yêu cầu ghi để đổi tên
            val intentSender = IntentSenderRequest.Builder(pi.intentSender).build()
            renameResultLauncher.launch(intentSender)
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    } else {
        val file = File(path)
        val newFile = File(file.parent, newName) // Tạo File mới với tên mới
        file.renameTo(newFile) // Đổi tên file
    }
}

internal fun tryBlock(func: () -> Unit): Exception? {
    return try {
        func.invoke()
        null
    } catch (e: Exception) {
        e
    }
}


fun overlayImageViewWithBitmap(
    imageView1: AppCompatImageView,
    imageView2: AppCompatImageView,
    overlayBitmap: Bitmap,
    context: Context
): Bitmap? {
//    lateinit var resultBitmap2: Bitmap
//    lateinit var canvas: Canvas
//    // Lấy bitmap từ AppCompatImageView
//
//    val baseBitmap2 = getBitmapFromImageView(imageView2) ?: return null
//
//    // Tạo một bitmap mới để vẽ cả baseBitmap và overlayBitmap lên đó
//
//    resultBitmap2 = Bitmap.createBitmap(baseBitmap2.width, baseBitmap2.height, baseBitmap2.config)
//    canvas = Canvas(resultBitmap2)
//
//    // Vẽ baseBitmap lên canvas (nền)
////    canvas.drawBitmap(baseBitmap1,0f, 0f, null)
//    canvas.drawBitmap(baseBitmap2, 0f, 0f, null)
//
//    // Vẽ overlayBitmap lên canvas (trồng lên)
//    canvas.drawBitmap(overlayBitmap, 0f, 0f, null)

    return overlayBitmap
}

lateinit var bitmap: Bitmap
lateinit var canvas2: Canvas
fun getBitmapFromImageView(imageView: AppCompatImageView): Bitmap? {
    // Kiểm tra xem ImageView có được layout chưa
    if (imageView.width == 0 || imageView.height == 0) {
        return null
    }

    // Tạo bitmap với kích thước của ImageView
    bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
    canvas2 = Canvas(bitmap)

    // Vẽ bg của ImageView vào canvas
    imageView.background?.draw(canvas2)

    // Vẽ drawable (ảnh đã set) của ImageView vào canvas
    imageView.drawable?.draw(canvas2)

    return bitmap
}

fun Context.getMusicMetadata(uri: Uri): HashMap<String, Any?>? {
    val metadataRetriever = MediaMetadataRetriever()
    try {
        metadataRetriever.setDataSource(this, uri)
        val metadata: HashMap<String, Any?> = kotlin.collections.HashMap()

        metadata["artist"] =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: getString(R.string.unknown)
        metadata["album"] =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: getString(R.string.unknown)
        metadata["name"] =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: getSongTitleFromMediaStore(this, uri)

        // Lấy hình ảnh (nếu có)
        val albumArt = metadataRetriever.embeddedPicture
        if (albumArt != null) {
            metadata["image"] = albumArt// Đây là byte array, có thể chuyển thành Bitmap
        } else {
            metadata["image"] = null
        }
        metadata["duration"] = formatDuration(
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                .toLong()
        )
        metadataRetriever.release()
        return metadata
    } catch (e: Exception) {
        return null
    }
}

fun getSongTitleFromMediaStore(context: Context, uri: Uri): String {
    val projection = arrayOf(MediaStore.Audio.AudioColumns.TITLE)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val titleIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)
            if (titleIndex != -1) {
                return cursor.getString(titleIndex)
            }
        }
    }
    return uri.lastPathSegment ?: context.getString(R.string.unknown)
}

@SuppressLint("SuspiciousIndentation")
//fun getALlMusic(context: Context): List<MusicModel> {
//    val musicFiles = mutableListOf<MusicModel>()
//    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
//    val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
//    val sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
//    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//    val cursor = context.contentResolver.query(uri, projection, selection, null, sortOrder)
//
//    cursor?.use { cursor ->
//        val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
//        while (cursor.moveToNext()) {
//            val filePath = cursor.getString(dataIndex)
//            val metadata = context.getMusicMetadata(Uri.parse(filePath))
//            if (metadata != null) {
//            musicFiles.add(MusicModel(filePath).apply {
//                    byteArrayAvt = metadata["bitmap"] as? ByteArray ?: null
//                    name = metadata["name"] as? String ?: File(path).name
//                    if(artist==null){
//                        artist = metadata["artist"] as? String ?: ""
//                    }
//                    if(album==null){
//                        album = metadata["album"] as? String ?: ""
//                    }
//                    if(duration==null){
//                        duration = metadata["duration"] as? String ?: ""
//                    }
//
//            })
//            }
//        }
//    }
//    return musicFiles
//}

//fun setRingtone(context: Context, musicPath: String, type: Int) : Boolean {
//    try{
//        val file = File(musicPath)
//        if (!file.exists()) {
//            println(showToast(context,R.string.file_does_not_exist))
//            return false
//        }
//
//        val resolver = context.contentResolver
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.TITLE, file.nameWithoutExtension)
//            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
//            put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE)
//            put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneManager.TYPE_NOTIFICATION)
//            put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM)
//            put(MediaStore.Audio.Media.IS_MUSIC, type == RingtoneManager.TYPE_ALL)
//        }
//
//        val audioUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
//        audioUri?.let { uri ->
//            resolver.openOutputStream(uri)?.use { outputStream ->
//                FileInputStream(file).use { inputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (Settings.System.canWrite(context)) {
//                    Settings.System.putString(context.contentResolver, when (type) {
//                        RingtoneManager.TYPE_RINGTONE -> Settings.System.RINGTONE
//                        RingtoneManager.TYPE_NOTIFICATION -> Settings.System.NOTIFICATION_SOUND
//                        RingtoneManager.TYPE_ALARM -> Settings.System.ALARM_ALERT
//                        else -> ""
//                    }, uri.toString())
//                    return true
//                } else {
//                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
//                        data = Uri.parse("package:${context.packageName}")
//                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    }
//                    context.startActivity(intent)
//                    return false
//                }
//            }
//            return false
//        } ?: return false
//    }catch (e : Exception){
//        return false
//    }
//}

fun byteArrayToBitmap(byteArray: ByteArray?): Bitmap? {
    return byteArray?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }
}

fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun scanMediaFile(context: Context, file: File) {
    MediaScannerConnection.scanFile(
        context,
        arrayOf(file.absolutePath),
        arrayOf("image/*") // Hoặc "audio/mp3", "audio/mpeg", "audio/wav" tùy loại file
    ) { path, uri ->

    }
}

fun viewToBitmap(view: View): Bitmap {
    val bitmap = createBitmap(view.width, view.height)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}
fun viewToBitmap(view: View, maxDimension: Int = 512): Bitmap {
    val scaleX = maxDimension.toFloat() / view.width
    val scaleY = maxDimension.toFloat() / view.height
    val scale = minOf(scaleX, scaleY) // giữ nguyên tỷ lệ

    val targetWidth = (view.width * scale).toInt()
    val targetHeight = (view.height * scale).toInt()

    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(scale, scale)
    view.draw(canvas)
    return bitmap
}
fun isInternetReachable(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
        process.waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

val formatter = SimpleDateFormat("MM.dd.yy", Locale.getDefault())

fun getSize(file: File): Long {
    var a = 0L
    if (file.isFile) {
        a = file.length()
    } else {
        a = getFolderSize(file)
    }
    return a
}

fun getFolderSize(folder: File): Long {
    var totalSize = 0L
    folder.listFiles()?.forEach {
        totalSize += if (it.isFile) it.length() else getFolderSize(it)
    }
    return totalSize
}

fun formatSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

@SuppressLint("ServiceCast")
fun toggleFlash(context: Context, check: Boolean) {
    if (check) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return  // không có flash

        try {
            // Bật flash
            cameraManager.setTorchMode(cameraId, true)

            // Tắt sau 200ms
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    cameraManager.setTorchMode(cameraId, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 50)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@SuppressLint("MissingPermission")
fun vibrateOnce(context: Context, durationMs: Long = 100, check: Boolean) {
    if (check) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}

fun saveBitmap(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    checkAvatar: Boolean = false,
    success: (Boolean, String, String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        var file: File
        if (fileName == "") {
//            val downloadsDir =
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//            val appDir = File(downloadsDir, NAME_SAVE_FILE)
//            if (!appDir.exists()) appDir.mkdirs()
//            file = File(appDir, "${System.currentTimeMillis()}.png")
            if (!checkAvatar) {
                val imagesDir = File(context.filesDir, "design")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                file = File(imagesDir, "${System.currentTimeMillis()}.png")
            } else {
                val imagesDir = File(context.filesDir, "avatar")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                file = File(imagesDir, "${System.currentTimeMillis()}.png")
            }
        } else {
            val imagesDir = File(context.filesDir, "avatar")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            file = File(imagesDir, fileName)
            file.delete()
            file = File(imagesDir, fileName)
        }
        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
            withContext(Dispatchers.Main) {
                success.invoke(
                    true, file.path, if (fileName == "") {
                        ""
                    } else {
                        file.path.replace("0.png", ".png")
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "saveBitmap: $e")
            withContext(Dispatchers.Main) {
                success.invoke(false, "", "")
            }
        }
    }
}
fun saveFileToExternalStorage(
    context: Context,
    sourcePath: String,
    destFileName: String,
    success: (Boolean, String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val sourceFile = File(sourcePath)
            // Tạo tên file từ timestamp hiện tại
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = timestamp + sourceFile.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ → MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/" + context.getString(R.string.app_name)
                    )
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        success(true, uri.toString())
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        success(false, "")
                    }
                }

            } else {
                // Android <10 → đường dẫn truyền thống
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, context.getString(R.string.app_name))
                if (!appDir.exists()) appDir.mkdirs()

                val destFile = File(appDir, fileName)

                sourceFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    success(true, destFile.absolutePath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                success(false, "")
            }
        }
    }
}
//
//fun Activity.showInter(context:Context,action: (() -> Unit)) {
//    MusicLocal.pause()
//    Admob.getInstance().showInterAll(this, object : InterCallback() {
//        override fun onNextAction() {
//            super.onNextAction()
//            action()
//            MusicLocal.play(context)
//        }
//    })
//}
//fun Activity.loadNativeCollabAds(id: String, layout: FrameLayout) {
//    Admob.getInstance().loadNativeCollap(this, id, layout)
//}
//
//fun Activity.showInterAll(context: Context) {
//    MusicLocal.pause()
//    Admob.getInstance().showInterAll(this, object : InterCallback() {
//        override fun onNextAction() {
//            super.onNextAction()
//            MusicLocal.play(context)
//        }
//    })
//}
//fun Activity.logEvent(nameEvent: String, value: String) {
//    val bundle = Bundle()
//    bundle.putString("link", value)
//    AdmobEvent.logEvent(this, nameEvent, bundle)
//}
//fun Activity.logEvent(nameEvent: String) {
//    AdmobEvent.logEvent(this, nameEvent, null)
//}



fun View.hide() {
    this.visibility = View.GONE
}

fun View.inhide() {
    this.visibility = View.INVISIBLE
}

fun View.show() {
    this.visibility = View.VISIBLE
}


